package com.msarobotika.cyberdeck

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

/** Opt-in real inference from an existing verified phone reading; no downloads or model replacement. */
class SavedContextInferenceDeviceTest {
 @Test fun existingModelExplainsVerifiedSavedPhoneReading()=runBlocking {
  assumeTrue(InstrumentationRegistry.getArguments().getString("savedAi")=="qwen3")
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val model=AiModelStore(context).current()?:error("No imported model")
  assertEquals("7900eb4e7362d88c58782c6f9999bb7a129e03544aa98b8f338ea0cc5d8c22c1",model.sha256)
  val evidence=EvidenceStore(context)
  val entry=evidence.list().last{it.name.startsWith("diagnostico-phone-")}
  val source=loadSavedAiContext(evidence,entry)
  val state=MutableStateFlow(LocalAiState());val client=LocalAiClient(context,state)
  val record=JSONObject().put("sourceId",entry.id).put("sourceHash",entry.sha256).put("verifiedAt",source.verifiedAt).put("modelHash",model.sha256)
  try{
   client.connect();withTimeout(20000){state.first{it.runtime || it.error.isNotBlank()}};assertTrue(state.value.error,state.value.runtime)
   client.send(AiWire.LOAD);val loaded=withTimeout(95000){state.first{!it.busy && (it.loaded || it.error.isNotBlank())}};assertTrue(loaded.error,loaded.loaded)
   val checked=loadSavedAiContext(evidence,entry)
   val prompt=AiPolicy.modelPrompt("Explica los límites de esta lectura guardada del móvil en dos frases en español. No inventes ataques.",checked.input(),model.name)
   record.put("prompt",prompt)
   client.send(AiWire.ASK,prompt)
   val answer=withTimeout(95000){state.first{!it.busy && (it.chunks>0 || it.error.isNotBlank())}}
   record.put("output",answer.output).put("error",answer.error).put("elapsedMs",answer.elapsed)
   assertTrue(answer.error,answer.output.isNotBlank());assertTrue(answer.error,answer.error.isBlank());assertFalse(answer.output.contains("<think>"))
  }finally{
   val out=File(context.filesDir,"validation/saved-ai-result-private.json").apply{parentFile?.mkdirs()};out.writeText(record.toString(2));client.disconnect()
  }
 }
}
