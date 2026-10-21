package com.msarobotika.cyberdeck

sealed interface AgentInput {
 data class Task(val task:AgentTask):AgentInput
 data class Followup(val action:AgentFollowup):AgentInput
 data object MissingContext:AgentInput
 data object Unknown:AgentInput
}
/** Same routing for text, speech transcripts and the continuation field. */
object AgentInputPolicy {
 fun resolve(text:String,hasRun:Boolean):AgentInput {
  val followup=AgentFollowupPolicy.match(text)
  if(followup!=null)return if(hasRun)AgentInput.Followup(followup) else AgentInput.MissingContext
  return AgentTasks.match(text)?.let{AgentInput.Task(it)}?:AgentInput.Unknown
 }
}
