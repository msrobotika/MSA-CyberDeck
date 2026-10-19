package com.msarobotika.cyberdeck

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import android.net.Uri
import java.io.File
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

/** Opt-in physical test using a verified, publicly downloadable model; never a CI download. */
class LocalModelDeviceTest {
 @Test fun importedModelGeneratesFromActualPhoneObservations()=runBlocking {
  val instrumentation=InstrumentationRegistry.getInstrumentation()
  assumeTrue(InstrumentationRegistry.getArguments().getString("localModel")=="qwen3")
  val context=instrumentation.targetContext
  val source=File(context.cacheDir,"qwen3_0_6b_mixed_int4.litertlm")
  assertTrue("Transfer the verified model to the app cache first",source.isFile)
  val store=AiModelStore(context)
  val model=store.import(Uri.fromFile(source)){}
  assertEquals("7900eb4e7362d88c58782c6f9999bb7a129e03544aa98b8f338ea0cc5d8c22c1",model.sha256)
  assertEquals(497516544L,model.bytes)
  assertEquals(source.name,model.name)
  val state=MutableStateFlow(LocalAiState());val client=LocalAiClient(context,state)
  val record=JSONObject().put("model",model.name).put("sha256",model.sha256)
  val report=File(context.filesDir,"validation/local-model-result.json").apply{parentFile?.mkdirs()}
  try {
   client.connect()
   withTimeout(20000){state.first{it.runtime || it.error.isNotBlank()}}
   assertTrue(state.value.error,state.value.runtime)
   val loadStart=android.os.SystemClock.elapsedRealtime()
   client.send(AiWire.LOAD)
   val loaded=withTimeout(95000){state.first{!it.busy && (it.loaded || it.error.isNotBlank())}}
   record.put("loadMs",android.os.SystemClock.elapsedRealtime()-loadStart).put("loadError",loaded.error)
   assertTrue(loaded.error,loaded.loaded)
   val phone=PhoneAudit.read(context).diagnostic()
   val input=phone.copy(details=phone.details.take(4))
   val prompt=AiPolicy.modelPrompt("Explica estas observaciones reales del móvil en dos frases en español. No inventes un ataque.",input,model.name)
   record.put("prompt",prompt)
   client.send(AiWire.ASK,prompt)
   val answer=withTimeout(95000){state.first{!it.busy && (it.chunks>0 || it.error.isNotBlank())}}
   record.put("output",answer.output).put("error",answer.error).put("elapsedMs",answer.elapsed).put("chunks",answer.chunks)
   assertTrue("No generated response: ${answer.error}",answer.output.isNotBlank())
   assertTrue("Generation incomplete: ${answer.error}",answer.error.isBlank())
   assertFalse(answer.output.contains("<think>"))
   assertFalse(answer.busy)
  } finally {
   record.put("finalState",state.value.phase)
   report.writeText(record.toString(2))
   client.disconnect()
  }
 }
}
