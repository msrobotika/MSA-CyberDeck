package com.msarobotika.cyberdeck

import org.junit.Assert.*
import org.junit.Test

class AgentSummaryTest {
 private val plan=AgentPlan(listOf(AgentTool.PHONE,AgentTool.CONNECTION))
 private fun result(tool:AgentTool)=AgentStepResult(tool,result=DiagnosticResult("fixture",tool.wire,"test","Fixture, not a reading",emptyList()))
 private fun finished(steps:List<AgentStepResult>,chosen:AgentPlan=plan)=AgentSession(plan=chosen,results=steps,run=AgentRun(chosen,"start","end",steps))
 @Test fun preparingPlanIsNotAnExecutedReview(){assertNull(AgentSummary.from(AgentSession(plan=plan)))}
 @Test fun waitingForFirstControllerStillOffersRunningState(){val s=AgentSummary.from(AgentSession(plan=plan,running=true))!!;assertEquals("Revisión en curso",s.title);assertEquals(0,s.results);assertEquals(0f,s.progress,0.0001f)}
 @Test fun stoppingAfterOneStepDoesNotCompleteTheRest(){val s=AgentSummary.from(finished(listOf(result(AgentTool.PHONE))).copy(notice="Detenido"))!!;assertEquals("Revisión detenida",s.title);assertEquals(1,s.results);assertEquals(0.5f,s.progress,0.0001f)}
 @Test fun awaitingInteractionIsNotAnErrorOrResult(){val s=AgentSummary.from(finished(listOf(result(AgentTool.PHONE),AgentStepResult(AgentTool.CONNECTION,awaiting=true))))!!;assertEquals("Esperando tu intervención",s.title);assertEquals(1,s.results)}
 @Test fun lastStepErrorNeverLooksLikeSuccess(){val s=AgentSummary.from(finished(listOf(result(AgentTool.PHONE),AgentStepResult(AgentTool.CONNECTION,error="Fixture error"))))!!;assertEquals("Revisión con error",s.title);assertEquals(0.5f,s.progress,0.0001f)}
 @Test fun manualOmittedAndGeneratedAreDistinguished(){
  val p=AgentPlan(listOf(AgentTool.PHONE,AgentTool.CONNECTION,AgentTool.ANALYST))
  val s=AgentSummary.from(finished(listOf(result(AgentTool.PHONE).copy(manual=true),AgentStepResult(AgentTool.CONNECTION,skipped=true),result(AgentTool.ANALYST)),p))!!
  assertEquals(1,s.results);assertEquals(1,s.manual);assertEquals(1,s.skipped);assertEquals(1,s.generated);assertEquals(1f,s.progress,0.0001f);assertEquals("Finalizada con pasos omitidos o manuales",s.title)
 }
 @Test fun completedMeansToolResultsNotSecurityVerdict(){val s=AgentSummary.from(finished(plan.steps.map{result(it)}))!!;assertEquals("Revisión finalizada",s.title);assertEquals(2,s.results);assertEquals(1f,s.progress,0.0001f);assertFalse(s.title.contains("seguro",true))}
}
