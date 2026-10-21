package com.msarobotika.cyberdeck

object AgentVoicePolicy {
 fun draft(raw:String):String {
  val text=raw.trim()
  require(text.length in 1..800 && text.none{it.code<32 && it!='\n' && it!='\t'}){"El dictado debe tener entre 1 y 800 caracteres legibles"}
  return text
 }
 /** Fixed execution status plus limited native summaries, never raw fields, URL or QR payload. */
 fun brief(session:AgentSession):String {
  val summary=AgentSummary.from(session)?:return "Todavía no hay una ejecución con resultados."
  val native=AgentAnswerPolicy.observations(session).filter{it.tool in setOf(AgentTool.PHONE,AgentTool.CONNECTION,AgentTool.RESOURCES,AgentTool.MAGNETIC)}.take(2)
  return buildString{
   append("${summary.title}. ${summary.results} de ${summary.total} pasos con resultado. ")
   if(summary.generated>0)append(if(summary.generated==1)"Un resultado es una interpretación de inteligencia artificial. " else "${summary.generated} resultados son interpretaciones de inteligencia artificial. ")
   if(summary.manual>0)append(if(summary.manual==1)"Un paso fue confirmado manualmente. " else "${summary.manual} pasos fueron confirmados manualmente. ")
   if(summary.skipped>0)append(if(summary.skipped==1)"Se omitió un paso. " else "${summary.skipped} pasos se omitieron. ")
   native.forEach{append("${it.tool.label}: ${it.result!!.summary.take(180)}. ")}
   append("Consulta las fuentes en pantalla; completar una herramienta no acredita seguridad.")
  }.take(1100)
 }
}
