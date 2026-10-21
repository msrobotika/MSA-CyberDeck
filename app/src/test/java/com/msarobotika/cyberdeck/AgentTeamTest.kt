package com.msarobotika.cyberdeck

import kotlin.coroutines.*
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class AgentTeamTest {
 private fun <T> sync(block:suspend()->T):T {
  var outcome:Result<T>?=null
  block.startCoroutine(object:Continuation<T>{override val context=EmptyCoroutineContext;override fun resumeWith(result:Result<T>){outcome=result}})
  return checkNotNull(outcome){"Test helper only supports immediate completion"}.getOrThrow()
 }
 private fun reading(tool:AgentTool)=DiagnosticResult("fixture",tool.wire,"2026-10-20T10:00:00Z","Fixture, not a hardware reading",emptyList())
 @Test fun everyRegisteredToolHasExactlyOneOwner(){
  AgentTool.entries.forEach{tool->assertEquals(1,AgentWorker.entries.count{it.accepts(tool)})}
  assertEquals(AgentWorker.SENSORS,AgentTeam.workerFor(AgentTool.NFC))
  assertEquals(AgentWorker.HARDWARE,AgentTeam.workerFor(AgentTool.RADIO))
 }
 @Test fun coordinatorSelectsOnlyRequiredWorkersWithoutReordering(){
  val plan=AgentPlan(listOf(AgentTool.PHONE,AgentTool.CONNECTION,AgentTool.RESOURCES))
  assertEquals(plan.steps,AgentTeam.assign(plan).map{it.tool})
  assertEquals(listOf(AgentWorker.MOBILE,AgentWorker.NETWORK,AgentWorker.MOBILE),AgentTeam.assign(plan).map{it.worker})
  val session=AgentSession(plan=plan)
  assertEquals(listOf(AgentWorker.MOBILE,AgentWorker.NETWORK),AgentTeam.states(session).map{it.worker})
 }
 @Test fun specialistCannotExecuteAnotherWorkersTool(){
  var dispatched=false
  try{sync{AgentWorker.HARDWARE.execute(AgentTool.NFC){dispatched=true;reading(it)}};fail("Must refuse wrong ownership")}catch(_:IllegalArgumentException){}
  assertFalse(dispatched)
 }
 @Test fun coordinatorReportsActiveToolBeforeReceivingResult(){
  val plan=AgentPlan(listOf(AgentTool.PHONE,AgentTool.DNS));val log=mutableListOf<String>()
  val run=sync{executeAgentActions(plan,{log+="perform:${it.wire}";reading(it)},progress={log+="result:${it.last().tool.wire}"},active={log+="active:${it.wire}"})}
  assertEquals(listOf("active:phone","perform:phone","result:phone","active:dns","perform:dns","result:dns"),log)
  assertEquals(2,run.steps.size)
 }
 @Test fun failureBlocksLaterSpecialistsAndDoesNotProduceFakeReading(){
  val plan=AgentPlan(listOf(AgentTool.USB,AgentTool.PHONE));val called=mutableListOf<AgentTool>()
  val run=sync{executeAgentActions(plan,{called+=it;error("No USB fixture")})}
  assertEquals(listOf(AgentTool.USB),called);assertNull(run.steps.single().result)
  val states=AgentTeam.states(AgentSession(plan=plan,run=run,results=run.steps))
  assertTrue(states[0].status.startsWith("Error"));assertTrue(states[1].status.contains("bloqueado"))
 }
 @Test fun cancellationNeverBecomesSuccessAndStopsLaterWork(){
  val called=mutableListOf<AgentTool>()
  try{sync{executeAgentActions(AgentPlan(listOf(AgentTool.NFC,AgentTool.PHONE)),{called+=it;throw CancellationException("fixture")})};fail("Must propagate cancellation")}catch(_:CancellationException){}
  assertEquals(listOf(AgentTool.NFC),called)
 }
 @Test fun stoppedAndGeneratedStatesNeverClaimVerifiedFacts(){
  val plan=AgentPlan(listOf(AgentTool.ANALYST,AgentTool.NFC))
  val steps=listOf(AgentStepResult(AgentTool.ANALYST,result=reading(AgentTool.ANALYST)))
  val run=AgentRun(plan,"start","end",steps);val session=AgentSession(plan=plan,run=run,results=steps)
  assertTrue(AgentTeam.states(session)[0].status.contains("revisar"))
  assertTrue(AgentTeam.states(session)[1].status.contains("sin ejecutar"))
  assertTrue(AgentTeam.review(run).contains("0 resultados de herramientas · 1 interpretaciones IA · 1 pasos sin resultado"))
 }
 @Test fun sharedContextKeepsTimeAndManualStatusInsteadOfInventingSensorData(){
  val plan=AgentPlan(listOf(AgentTool.PHONE,AgentTool.NFC,AgentTool.ANALYST))
  val steps=listOf(AgentStepResult(AgentTool.PHONE,result=reading(AgentTool.PHONE)),AgentStepResult(AgentTool.NFC,result=reading(AgentTool.NFC),manual=true))
  val run=AgentRun(plan,"start","end",steps)
  val data=AgentTeam.sharedContext(AgentSession(plan=plan,run=run,results=steps))!!
  assertEquals("team-context",data.kind)
  assertTrue(data.details[0].second.contains("2026-10-20T10:00:00Z"))
  assertEquals("Confirmación manual, sin verificación",data.details[1].second)
  assertNull(AgentTeam.sharedContext(AgentSession()))
 }
 @Test fun savedReportIncludesDelegationAndGenerationLimits(){
  val plan=AgentPlan(listOf(AgentTool.PHONE))
  val run=AgentRun(plan,"start","end",listOf(AgentStepResult(AgentTool.PHONE,result=reading(AgentTool.PHONE))))
  val report=run.diagnostic("fixture","hash","local")
  assertEquals("Móvil: phone",report.details.first{it.first=="Delegación del coordinador"}.second)
  assertTrue(report.details.first{it.first=="Configuración de generación"}.second.contains("puede equivocarse"))
 }
 @Test fun generationUsesLowVariabilityButNoTruthGuarantee(){
  assertEquals(1,AiGenerationPolicy.TOP_K);assertEquals(0.01,AiGenerationPolicy.TEMPERATURE,0.00001)
  assertTrue(AiGenerationPolicy.DESCRIPTION.contains("puede equivocarse"))
 }
 @Test fun stoppingBeforeAnyResponseNeverLeavesWorkersClaimingToBeQueued(){
  val session=AgentSession(plan=AgentPlan(listOf(AgentTool.PHONE)),notice="Detenido")
  assertEquals("Pendiente · sin ejecutar",AgentTeam.states(session).single().status)
 }
}
