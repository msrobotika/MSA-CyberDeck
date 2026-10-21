package com.msarobotika.cyberdeck
import org.junit.Assert.*
import org.junit.Test
class AgentAnswerPolicyTest {
 private fun r(tool:AgentTool)=AgentStepResult(tool,result=DiagnosticResult(tool.wire,"own","now","Observed ${tool.wire}",emptyList()))
 @Test fun connectionIsNotHiddenByALaterRamSample(){assertEquals(AgentTool.CONNECTION,AgentAnswerPolicy.primary(AgentSession(results=listOf(r(AgentTool.CONNECTION),r(AgentTool.RESOURCES))))!!.tool)}
 @Test fun actualWebObservationLeadsOverSyntacticLinkCheck(){assertEquals(AgentTool.WEB,AgentAnswerPolicy.primary(AgentSession(results=listOf(r(AgentTool.LINK),r(AgentTool.WEB))))!!.tool)}
 @Test fun generatedManualErroredAndAwaitingAreNotPromotedIntoObservations(){assertNull(AgentAnswerPolicy.primary(AgentSession(results=listOf(r(AgentTool.ANALYST),r(AgentTool.PHONE).copy(manual=true),r(AgentTool.CONNECTION).copy(error="error"),r(AgentTool.NFC).copy(awaiting=true)))))}
 @Test fun loneResourcesRemainARealObservationAndSourcesAreUnmodified(){val result=r(AgentTool.RESOURCES);assertSame(result,AgentAnswerPolicy.primary(AgentSession(results=listOf(result))))}
}
