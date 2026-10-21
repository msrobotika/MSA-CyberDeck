package com.msarobotika.cyberdeck

import java.text.Normalizer
import java.util.Locale

enum class AgentFollowup { REPEAT, SAVE, DETAILS, NEW }
data class AgentSavedResult(val runId:String,val evidence:Evidence)
object AgentFollowupPolicy {
 fun match(raw:String):AgentFollowup? {
  val text=Normalizer.normalize(raw.trim(),Normalizer.Form.NFD).replace(Regex("""\p{M}+"""),"").lowercase(Locale.ROOT).trim(' ','¿','?','¡','!','.').removePrefix("jarvis, ").removePrefix("ciberdeck, ")
  return when(text){
   "repite","repitelo","repite la tarea","repite la ultima tarea","vuelve a medir","mide otra vez"->AgentFollowup.REPEAT
   "guardalo","guarda el resultado","guarda esta lectura","guarda esta tarea"->AgentFollowup.SAVE
   "muestra los detalles","muestrame los detalles","como lo sabes","de donde sale"->AgentFollowup.DETAILS
   "nueva peticion","otra tarea","empezar otra tarea"->AgentFollowup.NEW
   else->null
  }
 }
 fun repeated(current:AgentSession):AgentSession? {
  val plan=current.plan?:return null
  if(current.running || current.planning || current.run==null)return null
  return AgentSession(request=current.request,modelHash=current.modelHash,plan=plan,inputs=current.inputs.copy(networkApproved=false),origin="Plan anterior · repetición solicitada")
 }
 fun canRepeatImmediately(session:AgentSession):Boolean {
  val safe=setOf(AgentTool.PHONE,AgentTool.CONNECTION,AgentTool.REVIEW,AgentTool.RESOURCES,AgentTool.SELF_APK,AgentTool.MAGNETIC)
  return session.plan?.steps?.all{it in safe}==true
 }
}
