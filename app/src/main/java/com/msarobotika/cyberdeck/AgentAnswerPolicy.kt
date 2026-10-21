package com.msarobotika.cyberdeck

/** Selects an existing tool observation, never generates a conclusion. */
object AgentAnswerPolicy {
 fun observations(session:AgentSession)=session.results.filter{it.result!=null && it.result.summary.isNotBlank() && !it.manual && !it.skipped && !it.awaiting && it.error.isBlank() && it.tool!=AgentTool.ANALYST}
 fun primary(session:AgentSession):AgentStepResult? {
  val results=observations(session)
  val auxiliary=setOf(AgentTool.RESOURCES,AgentTool.SELF_APK,AgentTool.REVIEW,AgentTool.INVENTORY,AgentTool.MODULES)
  return results.firstOrNull{it.tool !in auxiliary && !(it.tool==AgentTool.LINK && results.any{r->r.tool==AgentTool.WEB})}?:results.firstOrNull()
 }
}
