package com.msarobotika.cyberdeck

import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.CancellationException

enum class AgentTool(val wire:String,val label:String){PHONE("phone","Revisar este móvil"),CONNECTION("connection","Leer el estado de conexión"),COMPARE("compare","Comparar dos lecturas guardadas")}
data class AgentPlan(val steps:List<AgentTool>){init{require(steps.isNotEmpty() && steps.size<=3 && steps.distinct().size==steps.size){"Plan vacío, duplicado o demasiado largo"}}}
object AgentPolicy {
 const val SYSTEM="Traduce peticiones a un plan JSON para CiberDeck. No ejecutas nada. Herramientas: phone = revisar el móvil/teléfono; connection = leer el estado local de conexión/red; compare = comparar lecturas/diagnósticos guardados. Una petición puede necesitar varias herramientas: móvil Y conexión requiere phone Y connection. Incluye TODOS los pasos pedidos, sin duplicados, máximo tres. Responde SOLO {\"steps\":[...]} con esos identificadores, sin argumentos ni explicación. Si ninguna herramienta puede atender la petición responde {\"steps\":[]}. No inventes herramientas, resultados ni acciones."
 fun prompt(request:String,model:String):String {
  require(request.isNotBlank() && request.length<=800){"Escribe una petición de hasta 800 caracteres"}
  return "Ejemplos:\nPetición: Revisa mi móvil y el estado de su conexión\n{\"steps\":[\"phone\",\"connection\"]}\nPetición: Revisa mi móvil, lee su conexión y compara mis lecturas guardadas\n{\"steps\":[\"phone\",\"connection\",\"compare\"]}\nPetición: Compara mis lecturas guardadas\n{\"steps\":[\"compare\"]}\nPetición: Borra archivos o ataca una red\n{\"steps\":[]}\nTraduce esta petición, solo JSON:\nPetición: ${request.trim()}"+if(model.contains("qwen3",true))"\n/no_think" else ""
 }
 fun parse(raw:String):AgentPlan {
  require(raw.length<=1024){"La IA devolvió un plan demasiado largo; no se ha ejecutado nada"}
  var json=raw.trim()
  if(json.startsWith("```json\n") && json.endsWith("```"))json=json.removePrefix("```json\n").removeSuffix("```").trim()
  val grammar=Regex("""\{\s*"steps"\s*:\s*\[\s*(?:"(?:phone|connection|compare)"(?:\s*,\s*"(?:phone|connection|compare)"){0,2})?\s*\]\s*\}""")
  require(grammar.matches(json)){"Plan no compatible: la IA debe elegir solo las herramientas disponibles. No se ha ejecutado nada."}
  val array=JSONObject(json).getJSONArray("steps")
  require(array.length()>0){"Esta petición no tiene una herramienta compatible en el agente actual"}
  return AgentPlan((0 until array.length()).map{i->AgentTool.entries.first{it.wire==array.getString(i)}})
 }
}
data class AgentStepResult(val tool:AgentTool,val result:DiagnosticResult?=null,val error:String="")
data class AgentRun(val plan:AgentPlan,val startedAt:String,val finishedAt:String,val steps:List<AgentStepResult>){
 fun diagnostic(request:String,modelHash:String,scope:String):DiagnosticResult {
  val completed=steps.count{it.result!=null}
  val fields=listOf("Petición del usuario" to request.take(800),"SHA-256 modelo planificador" to modelHash,"Alcance de copias" to scope,"Inicio" to startedAt,"Fin" to finishedAt,"Plan aprobado" to plan.steps.joinToString{it.wire},"Método" to "La IA propuso el plan; el usuario pulsó Ejecutar. Resultados de herramientas locales, no texto generado. Sin cambios de ajustes ni peticiones de red; guardado manual.")+
   steps.flatMap{step->listOf("${step.tool.label} / estado" to if(step.result!=null)"Completado" else "Error: ${step.error.take(400)}")+(step.result?.let{r->listOf("${step.tool.label} / tipo" to r.kind,"${step.tool.label} / destino" to r.target,"${step.tool.label} / lectura" to r.checkedAt,"${step.tool.label} / resumen" to r.summary)+r.details.map{(key,value)->"${step.tool.label} / $key" to value}}?:emptyList())}
  fun build(limit:Int)=DiagnosticResult("agent","Este móvil y copias locales",finishedAt,"$completed de ${plan.steps.size} pasos completados; sesión de herramientas locales",fields.take(limit).map{it.first.take(160) to it.second.take(1000)}+("Límite del informe" to "Hasta $limit campos abreviados a 1000 caracteres. No certifica ausencia de ataques; las lecturas de origen conservan sus límites."))
  var limit=minOf(80,fields.size);var report=build(limit)
  while(report.toEvidenceJson().toByteArray().size>60000 && limit>7){limit--;report=build(limit)}
  require(report.toEvidenceJson().toByteArray().size<=65536){"Informe demasiado grande"}
  return report
 }
}
/** Only typed, validated tools reach this executor. A failure stops later steps. */
fun executeAgentPlan(plan:AgentPlan,perform:(AgentTool)->DiagnosticResult,progress:(List<AgentStepResult>)->Unit={}):AgentRun {
 val at=Instant.now().toString();val steps=mutableListOf<AgentStepResult>()
 for(tool in plan.steps){
  try{steps+=AgentStepResult(tool,result=perform(tool))}catch(e:CancellationException){throw e}catch(e:Exception){steps+=AgentStepResult(tool,error=e.message?:"Error de herramienta")}
  progress(steps.toList())
  if(steps.last().result==null)break
 }
 return AgentRun(plan,at,Instant.now().toString(),steps.toList())
}
