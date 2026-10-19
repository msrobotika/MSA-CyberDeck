package com.msarobotika.cyberdeck

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

/** Real planning and read-only execution without relying on an unlocked screen. */
class AgentInferenceDeviceTest {
 @Test fun importedModelPlansBothRequestedLocalTools()=runBlocking<Unit> {
  assumeTrue(InstrumentationRegistry.getArguments().getString("agentAi")=="qwen3")
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val model=AiModelStore(context).current()?:error("No model imported")
  val state=MutableStateFlow(LocalAiState());val client=LocalAiClient(context,state)
  val record=JSONObject().put("modelHash",model.sha256)
  try{
   client.connect();withTimeout(20000){state.first{it.runtime || it.error.isNotBlank()}};assertTrue(state.value.error,state.value.runtime)
   client.send(AiWire.LOAD);val loaded=withTimeout(95000){state.first{!it.busy && (it.loaded || it.error.isNotBlank())}};assertTrue(loaded.error,loaded.loaded)
   client.send(AiWire.PLAN,AgentPolicy.prompt("Revisa mi móvil y el estado de su conexión",model.name))
   val output=withTimeout(95000){state.first{!it.busy && (it.chunks>0 || it.error.isNotBlank())}}
   record.put("output",output.output).put("error",output.error).put("elapsedMs",output.elapsed)
   assertTrue(output.error,output.error.isBlank())
   val plan=AgentPolicy.parse(output.output)
   assertEquals(listOf(AgentTool.PHONE,AgentTool.CONNECTION),plan.steps)
   val before=EvidenceStore(context).list()
   val run=executeAgentPlan(plan,{performAgentTool(context,it,null)})
   assertEquals(listOf("phone","connection"),run.steps.map{it.result?.kind})
   assertEquals(before,EvidenceStore(context).list())
   record.put("completed",run.steps.size)
  }finally{
   File(context.filesDir,"validation/agent-inference-private.json").apply{parentFile?.mkdirs()}.writeText(record.toString(2));client.disconnect()
  }
 }
}
