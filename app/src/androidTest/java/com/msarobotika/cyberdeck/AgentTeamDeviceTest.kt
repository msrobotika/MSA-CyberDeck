package com.msarobotika.cyberdeck

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.Instant
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Public Android diagnostics and service IPC; does not unlock or require access to the phone UI. */
class AgentTeamDeviceTest {
 private val instrument=InstrumentationRegistry.getInstrumentation()
 private val app get()=instrument.targetContext
 private fun await(timeout:Long,ready:()->Boolean){val end=SystemClock.elapsedRealtime()+timeout;while(!ready()){check(SystemClock.elapsedRealtime()<end){"Timed out waiting for local engine"};Thread.sleep(50)}}
 @Test fun coordinatorDelegatesActualAndroidReadingsAndRecordsTheirSources():Unit=runBlocking {
  val plan=AgentPlan(listOf(AgentTool.PHONE,AgentTool.CONNECTION,AgentTool.RESOURCES))
  val active=mutableListOf<AgentTool>();val started=Instant.now()
  val run=executeAgentActions(plan,{performAgentTool(app,it,null)},active={active+=it})
  assertEquals(plan.steps,active)
  assertEquals(listOf("phone","connection","resources"),run.steps.map{it.result?.kind})
  assertTrue(run.steps.all{it.error.isBlank() && !it.manual && !it.awaiting && Instant.parse(it.result!!.checkedAt)>=started})
  assertTrue(run.steps.first().result!!.target.contains(android.os.Build.MODEL))
  val session=AgentSession(plan=plan,results=run.steps,run=run)
  assertEquals(listOf(AgentWorker.MOBILE,AgentWorker.NETWORK),AgentTeam.states(session).map{it.worker})
  assertTrue(AgentTeam.states(session).all{it.status.startsWith("Resultados recibidos")})
  assertEquals(3,AgentTeam.sharedContext(session)!!.details.size)
  val report=run.diagnostic("Instrumented Android read","","This device",origin="Device test")
  assertTrue(report.details.any{it.first=="Delegación del coordinador" && it.second.contains("Red: connection")})
  File(app.filesDir,"validation/team-023-private.json").apply{parentFile?.mkdirs();writeText(report.toEvidenceJson())}
 }
 @Test fun actualLocalModelUsesNewSamplingAndProposesOnlyRequestedPhoneTools(){
  assumeTrue(InstrumentationRegistry.getArguments().getString("agentAi")=="qwen3")
  val state=MutableStateFlow(LocalAiState());lateinit var client:LocalAiClient
  instrument.runOnMainSync{client=LocalAiClient(app,state);client.connect()}
  try{
   await(20000){state.value.connected && !state.value.busy}
   instrument.runOnMainSync{client.send(AiWire.LOAD)}
   await(95000){!state.value.busy};assertTrue(state.value.error,state.value.loaded)
   val environment=AiContext.snapshot(PhoneAudit.read(app).diagnostic())
   val request="Revisa mi móvil y el estado de su conexión"
   instrument.runOnMainSync{client.send(AiWire.PLAN,AgentPolicy.prompt(request,"qwen3",environment))}
   await(95000){!state.value.busy}
   File(app.filesDir,"validation/team-023-plan-private.txt").apply{parentFile?.mkdirs();writeText(AiGenerationPolicy.DESCRIPTION+"\n"+state.value.output+"\nERROR: "+state.value.error)}
   assertTrue(state.value.error,state.value.error.isBlank())
   val plan=AgentTasks.checkHardwareRequest(request,AgentPolicy.parse(state.value.output))
   assertEquals(listOf(AgentTool.PHONE,AgentTool.CONNECTION),plan.steps)
   assertEquals(listOf(AgentWorker.MOBILE,AgentWorker.NETWORK),AgentTeam.assign(plan).map{it.worker})
  }finally{instrument.runOnMainSync{client.disconnect()}}
 }
}
