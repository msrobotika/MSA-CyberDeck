package com.msarobotika.cyberdeck
import org.junit.Test
import org.junit.Assert.*

class AgentExpansionTest {
 private fun fixture()=DiagnosticResult("fixture","Owned fixture","2026-10-19T00:00:00Z","Fixture, not physical data",emptyList())
 @Test fun extendedGrammarAcceptsOnlyKnownDistinctBoundedTools(){
  val tools=AgentTool.entries.take(8)
  assertEquals(tools,AgentPolicy.parse("{\"steps\":["+tools.joinToString{ "\"${it.wire}\"" }+"]}").steps)
  assertTrue(runCatching{AgentPolicy.parse("{\"steps\":["+AgentTool.entries.take(9).joinToString{"\"${it.wire}\""}+"]}")}.isFailure)
  assertTrue(runCatching{AgentPolicy.parse("{\"steps\":[\"nfc\"],\"root\":true}")}.isFailure)
 }
 @Test fun assistedStepNeverCallsDispatcherOrPretendsCompletion(){
  var calls=0;val run=executeAgentPlan(AgentPlan(listOf(AgentTool.NFC,AgentTool.PHONE)),{calls++;fixture()})
  assertEquals(0,calls);assertEquals(1,run.steps.size);assertTrue(run.steps.single().awaiting);assertNull(run.steps.single().result)
  assertTrue(run.diagnostic("fixture","","scope").summary.contains("0 de 2"))
 }
 @Test fun resumedPrefixIsNotRepeatedAndCannotSubstituteAnotherPlan(){
  val plan=AgentPlan(listOf(AgentTool.APK,AgentTool.PHONE));val source=AgentStepResult(AgentTool.APK,result=fixture())
  val called=mutableListOf<AgentTool>();val run=executeAgentPlan(plan,{called+=it;fixture()},previous=listOf(source),startedAt="original")
  assertEquals(listOf(AgentTool.PHONE),called);assertEquals("original",run.startedAt);assertEquals(2,run.steps.size)
  assertTrue(runCatching{executeAgentPlan(plan,{fixture()},previous=listOf(AgentStepResult(AgentTool.PHONE,result=fixture())))}.isFailure)
 }
 @Test fun skipAndManualConfirmationRemainDistinctFromMeasurements(){
  val plan=AgentPlan(listOf(AgentTool.NFC,AgentTool.PHONE));val skipped=executeAgentPlan(plan,{fixture()},previous=listOf(AgentStepResult(AgentTool.NFC,skipped=true)))
  assertTrue(skipped.diagnostic("fixture","","scope").summary.contains("1 omitidos"))
  val manual=executeAgentPlan(plan,{fixture()},previous=listOf(AgentStepResult(AgentTool.NFC,result=fixture(),manual=true)))
  assertTrue(manual.diagnostic("fixture","","scope").details.any{it.first.endsWith("/ estado") && it.second.contains("manualmente")})
 }
 @Test fun evidenceOfferNeedsMatchingRunRouteAndKind(){
  val plan=AgentPlan(listOf(AgentTool.NFC));val run=executeAgentPlan(plan,{fixture()});val state=AgentSession(plan=plan,run=run)
  val e=Evidence("id","diagnostico-nfc.json","a".repeat(64),20,"now")
  assertEquals(e,offerAgentEvidence(state,e,"nfc","NFC",run.startedAt).resumeEvidence)
  assertNull(offerAgentEvidence(state,e,"apk","NFC",run.startedAt).resumeEvidence)
  assertNull(offerAgentEvidence(state,e,"nfc","APK",run.startedAt).resumeEvidence)
  assertNull(offerAgentEvidence(state,e,"nfc","NFC","other run").resumeEvidence)
 }
 @Test fun networkDestinationsRequireExplicitInputsAndAuthorization(){
  val plan=AgentPlan(listOf(AgentTool.DNS,AgentTool.TCP,AgentTool.WEB))
  assertTrue(runCatching{validateAgentInputs(plan,AgentInputs())}.isFailure)
  val input=AgentInputs(host="127.0.0.1",port="443",url="https://example.com/",networkApproved=true)
  validateAgentInputs(plan,input)
  assertTrue(runCatching{validateAgentInputs(plan,input.copy(networkApproved=false))}.isFailure)
  assertTrue(runCatching{validateAgentInputs(plan,input.copy(port="0"))}.isFailure)
  assertTrue(runCatching{validateAgentInputs(plan,input.copy(url="https://user:password@example.com"))}.isFailure)
  assertTrue(runCatching{validateAgentInputs(plan,input.copy(host="example.com;command"))}.isFailure)
 }
 @Test fun offlineInputsDoNotRequireNetworkApproval(){validateAgentInputs(AgentPlan(listOf(AgentTool.CIDR,AgentTool.LINK)),AgentInputs(cidr="192.0.2.10/24",url="https://example.com/"));assertTrue(runCatching{validateAgentInputs(AgentPlan(listOf(AgentTool.CIDR)),AgentInputs(cidr="bad"))}.isFailure)}
 @Test fun allStepStatusesRemainInBoundedReport(){
  val tools=AgentTool.entries.take(8);val run=executeAgentPlan(AgentPlan(tools),{fixture().copy(details=(1..256).map{n->"field$n" to "\u0001".repeat(2000)})})
  val report=run.diagnostic("fixture","hash","scope","Selección manual")
  assertEquals(8,report.details.count{it.first.endsWith("/ estado")});assertTrue(report.toEvidenceJson().toByteArray().size<=65536)
 }
}
