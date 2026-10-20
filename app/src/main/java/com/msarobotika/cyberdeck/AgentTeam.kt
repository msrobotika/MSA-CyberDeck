package com.msarobotika.cyberdeck

/** Specialists are bounded tool workers, not separate loaded language models. */
enum class AgentWorker(val label:String,val scope:String) {
 MOBILE("Móvil","Diagnóstico Android, recursos, APK y llamadas"),
 NETWORK("Red","Conectividad, destinos y servicios autorizados"),
 SENSORS("Sensores","Bluetooth LE, NFC y magnetómetro"),
 HARDWARE("Hardware","USB, radar del ESP y capacidades pendientes"),
 EVIDENCE("Evidencias","Copias, trabajos, integridad e informes"),
 ANALYST("Analista","Interpretación con el único modelo local; requiere revisión");
 fun accepts(tool:AgentTool)=AgentTeam.workerFor(tool)==this
 suspend fun execute(tool:AgentTool,perform:suspend(AgentTool)->DiagnosticResult):DiagnosticResult {
  require(accepts(tool)){"La herramienta no pertenece a este especialista"}
  return perform(tool)
 }
}

data class AgentAssignment(val tool:AgentTool,val worker:AgentWorker)
data class AgentWorkerState(val worker:AgentWorker,val tools:List<AgentTool>,val status:String)

object AgentTeam {
 fun workerFor(tool:AgentTool):AgentWorker=when(tool){
  AgentTool.PHONE,AgentTool.RESOURCES,AgentTool.REVIEW,AgentTool.SELF_APK,AgentTool.APK,AgentTool.CALL_STATUS,AgentTool.CALLS->AgentWorker.MOBILE
  AgentTool.CONNECTION,AgentTool.DNS,AgentTool.TCP,AgentTool.WEB,AgentTool.LINK,AgentTool.CIDR,AgentTool.LAN,AgentTool.EXPOSURE,AgentTool.TRANSFER,AgentTool.RECEIVE->AgentWorker.NETWORK
  AgentTool.BLE,AgentTool.NFC,AgentTool.MAGNETIC->AgentWorker.SENSORS
  AgentTool.USB,AgentTool.RADIO,AgentTool.MODULES->AgentWorker.HARDWARE
  AgentTool.COMPARE,AgentTool.INVENTORY,AgentTool.INTEGRITY,AgentTool.FILES,AgentTool.WORKS,AgentTool.EXPORT->AgentWorker.EVIDENCE
  AgentTool.ANALYST->AgentWorker.ANALYST
 }
 fun assign(plan:AgentPlan)=plan.steps.map{AgentAssignment(it,workerFor(it))}
 fun sharedContext(session:AgentSession):DiagnosticResult? {
  val run=session.run?:return null
  if(session.results.isEmpty())return null
  return DiagnosticResult("team-context","Especialistas de la misión actual",run.finishedAt,
   "Resumen abreviado de resultados en memoria; no es una nueva medición ni verificación independiente",
   session.results.map{step->"${workerFor(step.tool).label}/${step.tool.wire}" to when {
    step.error.isNotBlank()->"ERROR: ${step.error.take(100)}"
    step.awaiting->"Pendiente, sin lectura"
    step.manual->"Confirmación manual, sin verificación"
    step.skipped->"Omitido, sin lectura"
    step.tool==AgentTool.ANALYST->"Interpretación IA anterior, no una lectura"
    else->step.result?.let{"${it.checkedAt.take(35)} · ${it.summary.take(90)} · "+it.details.take(2).joinToString{"${it.first.take(35)}=${it.second.take(55)}"}}?:"Sin resultado"
   }})
 }
 fun states(session:AgentSession):List<AgentWorkerState> {
  val plan=session.plan?:return emptyList()
  val failed=session.error.isNotBlank() || session.results.any{it.error.isNotBlank()}
  return assign(plan).groupBy{it.worker}.map{(worker,jobs)->
   val tools=jobs.map{it.tool};val results=session.results.filter{it.tool in tools}
   val status=when {
    results.any{it.error.isNotBlank()}->"Error · misión detenida"
    session.running && session.activeTool in tools->"Trabajando · ${session.activeTool?.label}"
    results.any{it.awaiting}->"Espera tu intervención"
    results.size==tools.size && results.all{it.result!=null || it.skipped}->when {
     results.any{it.manual || it.skipped}->"Finalizado con pasos manuales u omitidos"
     worker==AgentWorker.ANALYST->"Interpretación generada · revisar"
     else->"Resultados recibidos · ${results.size}/${tools.size}"
    }
    failed->"Pendiente · bloqueado por error"
    !session.running && (session.run!=null || session.notice.isNotBlank())->"Pendiente · sin ejecutar"
    else->"En cola · ${results.count{it.result!=null}}/${tools.size}"
   }
   AgentWorkerState(worker,tools,status)
  }
 }
 /** No generative verdict: counts and source dates only, including missing/omitted/manual work. */
 fun review(run:AgentRun):String {
  val readings=run.steps.count{it.result!=null && !it.manual && it.tool!=AgentTool.ANALYST}
  val generated=run.steps.count{it.result!=null && !it.manual && it.tool==AgentTool.ANALYST}
  val remaining=run.plan.steps.count{tool->run.steps.none{it.tool==tool && (it.result!=null || it.skipped)}}
  return "$readings resultados de herramientas · $generated interpretaciones IA · $remaining pasos sin resultado. "+
   "${run.steps.count{it.manual}} confirmaciones manuales · ${run.steps.count{it.skipped}} omitidos. No acredita ausencia de amenazas."
 }
}

/** Low variability reduces sampling choices; it does not establish factual accuracy. */
object AiGenerationPolicy {
 const val TOP_K=1
 const val TOP_P=1.0
 const val TEMPERATURE=0.01
 const val DESCRIPTION="Temperatura 0,01 · selección top-k 1. Menor variación; la IA aún puede equivocarse."
}
