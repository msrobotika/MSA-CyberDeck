package com.msarobotika.cyberdeck

import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.CancellationException

class AgentPolicyTest {
 private fun fixture()=DiagnosticResult("fixture","Owned fixture","2026-10-19T00:00:00Z","Fixture, not actual data",listOf("k" to "v"))
 @Test fun supportedPlanHasTypedOrderedSteps(){assertEquals(listOf(AgentTool.PHONE,AgentTool.CONNECTION),AgentPolicy.parse("{\"steps\":[\"phone\",\"connection\"]}").steps)}
 @Test fun codeFenceAcceptedButProseAndUnknownArgumentsRejected(){
  assertEquals(AgentTool.PHONE,AgentPolicy.parse("```json\n{\"steps\":[\"phone\"]}\n```").steps.single())
  for(raw in listOf("Here is the plan: {\"steps\":[\"phone\"]}","{\"steps\":[\"shell\"]}","{\"steps\":[\"phone\"],\"cmd\":\"delete\"}","{\"steps\":[\"phone\"],\"steps\":[\"compare\"]}","{\"steps\":[1]}","{\"steps\":[\"PHONE\"]}")){assertTrue(raw,runCatching{AgentPolicy.parse(raw)}.isFailure)}
 }
 @Test fun emptyDuplicateAndOversizedPlansRejected(){for(raw in listOf("{\"steps\":[]}","{\"steps\":[\"phone\",\"phone\"]}"," ".repeat(1025))){assertTrue(runCatching{AgentPolicy.parse(raw)}.isFailure)}}
 @Test fun promptOnlyIncludesExplicitRequestAndKnownTools(){val prompt=AgentPolicy.prompt("Revisa mi móvil","qwen3");assertTrue(prompt.contains("Revisa mi móvil"));assertTrue(prompt.endsWith("/no_think"));assertTrue(runCatching{AgentPolicy.prompt("x".repeat(801),"")}.isFailure)}
 @Test fun preparationNeverInvokesExecutor(){var calls=0;val plan=AgentPolicy.parse("{\"steps\":[\"phone\",\"connection\"]}");assertEquals(0,calls);val run=executeAgentPlan(plan,{calls++;fixture()});assertEquals(2,calls);assertEquals(2,run.steps.size);assertTrue(run.steps.all{it.result==fixture()})}
 @Test fun failureStopsLaterToolsAndKeepsPriorResults(){val calls=mutableListOf<AgentTool>();val plan=AgentPlan(listOf(AgentTool.PHONE,AgentTool.CONNECTION,AgentTool.COMPARE));val run=executeAgentPlan(plan,{tool->calls+=tool;if(tool==AgentTool.CONNECTION)error("Unavailable")else fixture()});assertEquals(listOf(AgentTool.PHONE,AgentTool.CONNECTION),calls);assertEquals("Unavailable",run.steps.last().error);assertNotNull(run.steps.first().result);assertNull(run.steps.last().result)}
 @Test fun cancellationDoesNotBecomeCompletedTool(){try{executeAgentPlan(AgentPlan(listOf(AgentTool.PHONE)),{throw CancellationException("Stop")});fail("Cancellation swallowed")}catch(_:CancellationException){}}
 @Test fun reportBindsRequestPlanModelAndActualReadingsWithinLimit(){val giant=fixture().copy(details=(1..256).map{"field$it" to "\u0001".repeat(4000)});val run=executeAgentPlan(AgentPlan(listOf(AgentTool.PHONE)),{giant});val report=run.diagnostic("Explicit request","a".repeat(64),"owned work");assertEquals("agent",report.kind);assertTrue(report.toEvidenceJson().toByteArray().size<=65536);assertTrue(report.details.any{it.first=="SHA-256 modelo planificador" && it.second=="a".repeat(64)});assertTrue(report.details.any{it.first=="Plan aprobado" && it.second=="phone"});assertTrue(report.summary.contains("1 de 1"))}
}
