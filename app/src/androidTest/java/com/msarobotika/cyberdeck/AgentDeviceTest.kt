package com.msarobotika.cyberdeck

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*

class AgentDeviceTest {
 @Test fun comparisonToolUsesActualPinnedWorkEvidence(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val state=AuditWorkStore(context).read();val work=state.works.firstOrNull{it.id==state.activeId}
  org.junit.Assume.assumeTrue(work!=null && work.evidence.size>=2)
  val before=EvidenceStore(context).list()
  val result=performAgentTool(context,AgentTool.COMPARE,work!!.id)
  assertEquals("comparison",result.kind)
  assertTrue(result.details.any{it.first=="Evidencia A" && it.second.contains("SHA-256")})
  assertEquals(before,EvidenceStore(context).list())
 }
 @Test fun androidValidatorAcceptsOnlyTypedPlans(){
  assertEquals(listOf(AgentTool.PHONE,AgentTool.CONNECTION),AgentPolicy.parse("{\"steps\":[\"phone\",\"connection\"]}").steps)
  assertTrue(runCatching{AgentPolicy.parse("{\"steps\":[\"shell\"]}")}.isFailure)
  assertTrue(runCatching{AgentPolicy.parse("{\"steps\":[\"phone\",\"phone\"]}")}.isFailure)
 }
 @Test fun typedExecutorReadsActualPhoneAndConnectionWithoutWritingEvidence(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val store=EvidenceStore(context);val before=store.list()
  val run=executeAgentPlan(AgentPlan(listOf(AgentTool.PHONE,AgentTool.CONNECTION)),{performAgentTool(context,it,null)})
  assertEquals(listOf("phone","connection"),run.steps.map{it.result?.kind})
  assertTrue(run.steps.first().result!!.details.isNotEmpty());assertEquals(before,store.list())
 }
}
