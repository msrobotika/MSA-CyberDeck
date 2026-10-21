package com.msarobotika.cyberdeck

import java.time.Instant
import java.util.concurrent.CancellationException

/** Activity-bound executor: every registered tool is dispatched, never marked complete by navigation. */
suspend fun executeAgentActions(plan:AgentPlan,perform:suspend(AgentTool)->DiagnosticResult,progress:(List<AgentStepResult>)->Unit={},previous:List<AgentStepResult> = emptyList(),startedAt:String=Instant.now().toString(),active:(AgentTool)->Unit={}):AgentRun {
 require(previous.size<=plan.steps.size && previous.map{it.tool}==plan.steps.take(previous.size) && previous.all{!it.awaiting && it.error.isBlank() && (it.result!=null || it.skipped)}){"El historial no corresponde al plan"}
 val steps=previous.toMutableList()
 for(job in AgentTeam.assign(plan).drop(previous.size)){
  val tool=job.tool
  active(tool)
  try{steps+=AgentStepResult(tool,result=job.worker.execute(tool,perform))}
  catch(e:CancellationException){throw e}
  catch(e:Exception){steps+=AgentStepResult(tool,error=e.message?:"Error de herramienta")}
  progress(steps.toList())
  if(steps.last().result==null)break
 }
 return AgentRun(plan,startedAt,Instant.now().toString(),steps.toList())
}
