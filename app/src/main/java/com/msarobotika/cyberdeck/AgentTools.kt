package com.msarobotika.cyberdeck

import android.content.Context
import java.time.Instant

fun performAgentTool(context:Context,tool:AgentTool,workId:String?,inputs:AgentInputs=AgentInputs(),cancelled:()->Boolean={Thread.currentThread().isInterrupted}):DiagnosticResult {
 validateAgentInputs(AgentPlan(listOf(tool)),inputs)
 return when(tool){
 AgentTool.PHONE->PhoneAudit.read(context).diagnostic()
 AgentTool.CONNECTION->ConnectionDiagnostic.read(context)
 AgentTool.COMPARE->{
  val store=EvidenceStore(context)
  val work=workId?.let{id->AuditWorkStore(context).read().works.firstOrNull{it.id==id}?:error("El trabajo seleccionado ya no está disponible")}
  val entries=store.list().asReversed().filter{e->e.name.startsWith("diagnostico-") && !e.name.startsWith("diagnostico-comparison-") && e.bytes<=65536 && (work==null || work.evidence.any{AuditWorkPolicy.matches(it,e)})}
  val pair=suggestComparison(entries){store.diagnostic(it)}?:error("No hay dos lecturas compatibles verificables entre las últimas 100 copias del alcance")
  loadSavedComparison(store,pair.baseline.entry,pair.current.entry).diagnostic()
 }
 AgentTool.REVIEW->phoneReviewDiagnostic(PhoneAudit.read(context),PhoneReviewStore(context).read())
 AgentTool.RESOURCES->ResourceSampler.diagnostic(listOf(ResourceSampler.read(context)))
 AgentTool.SELF_APK->ApkInspector.inspectFile(context,java.io.File(context.applicationInfo.sourceDir)).diagnostic()
 AgentTool.INVENTORY->{val entries=EvidenceStore(context).list();DiagnosticResult("inventory","Copias privadas de CiberDeck",Instant.now().toString(),"${entries.size} copias en el índice local",listOf("Bytes registrados" to entries.sumOf{it.bytes}.toString(),"Diagnósticos" to entries.count{it.name.startsWith("diagnostico-")}.toString(),"Método" to "Recuento del índice privado; no verifica contenido ni accede a originales del usuario."))}
 AgentTool.INTEGRITY->{
  val id=workId?:error("Activa un trabajo para verificar sus copias")
  val work=AuditWorkStore(context).read().works.firstOrNull{it.id==id}?:error("Trabajo no disponible")
  val check=verifyWorkCopies(work,EvidenceStore(context),cancelled)
  DiagnosticResult("work-integrity",work.name,check.at,"${check.count(WorkCopyState.VERIFIED)} de ${check.checks.size} copias verificadas",WorkCopyState.entries.map{it.label to check.count(it).toString()}+listOf("Método" to "Comprobación de referencias fijadas, tamaño y SHA-256. No acredita procedencia ni ausencia de malware."))
 }
 AgentTool.CALL_STATUS->DiagnosticResult("call-status","Este móvil",Instant.now().toString(),if(CallJournal.active(context))"CiberDeck tiene el rol de filtrado" else "Filtro CiberDeck no activado",listOf("API disponible" to CallJournal.available(context).toString(),"Eventos privados registrados" to CallJournal(context).records().size.toString(),"Método" to "Consulta de estado local, sin leer números ni modificar el rol. No analiza una llamada nueva."))
 AgentTool.DNS->Diagnostics.dns(inputs.host)
 AgentTool.TCP->Diagnostics.tcp(inputs.host,inputs.port.toInt())
 AgentTool.WEB->Diagnostics.web(inputs.url)
 AgentTool.LINK->if(inputs.qrImage)error("La lectura QR requiere el selector de imágenes de Android") else LinkInspection.inspect(inputs.url).diagnostic()
 AgentTool.CIDR->DiagnosticResult("cidr",inputs.cidr,Instant.now().toString(),"Cálculo IPv4 local, sin escaneo",NetworkPlan.details(NetworkPlan.parse(inputs.cidr)))
 else->error("Esta herramienta requiere interacción en su pantalla; no se ejecuta en segundo plano")

}

}

fun loadAgentStepResult(store:EvidenceStore,pending:AgentStepResult,entry:Evidence):AgentStepResult {
 require(pending.awaiting && pending.tool.expectedKind!=null){"El paso no admite un diagnóstico guardado"}
 val data=store.diagnostic(entry)
 require(data.kind==pending.tool.expectedKind){"El diagnóstico no corresponde a este paso"}
 return AgentStepResult(pending.tool,result=data,source=entry)
}
