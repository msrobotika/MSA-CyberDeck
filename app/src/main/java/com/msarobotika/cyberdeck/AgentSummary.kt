package com.msarobotika.cyberdeck

/** Execution state only. It never derives a security score from successful tool calls. */
data class AgentSummary(val title:String,val total:Int,val results:Int,val skipped:Int,val manual:Int,val generated:Int,val progress:Float) {
 companion object {
  fun from(session:AgentSession):AgentSummary? {
   val plan=session.plan?:return null
   if(!session.running && session.run==null && session.notice.isBlank() && session.error.isBlank())return null
   val steps=session.results
   val results=steps.count{it.result!=null && !it.manual}
   val skipped=steps.count{it.skipped}
   val manual=steps.count{it.manual}
   val generated=steps.count{it.tool==AgentTool.ANALYST && it.result!=null && !it.manual}
   val title=when {
    session.running->"Revisión en curso"
    session.error.isNotBlank() || steps.any{it.error.isNotBlank()}->"Revisión con error"
    steps.any{it.awaiting}->"Esperando tu intervención"
    results+skipped+manual<plan.steps.size->"Revisión detenida"
    skipped>0 || manual>0->"Finalizada con pasos omitidos o manuales"
    else->"Revisión finalizada"
   }
   return AgentSummary(title,plan.steps.size,results,skipped,manual,generated,((results+skipped+manual).toFloat()/plan.steps.size).coerceIn(0f,1f))
  }
 }
}
