package com.msarobotika.cyberdeck

import android.content.Context

fun performAgentTool(context:Context,tool:AgentTool,workId:String?):DiagnosticResult=when(tool){
 AgentTool.PHONE->PhoneAudit.read(context).diagnostic()
 AgentTool.CONNECTION->ConnectionDiagnostic.read(context)
 AgentTool.COMPARE->{
  val store=EvidenceStore(context)
  val work=workId?.let{id->AuditWorkStore(context).read().works.firstOrNull{it.id==id}?:error("El trabajo seleccionado ya no está disponible")}
  val entries=store.list().asReversed().filter{e->e.name.startsWith("diagnostico-") && !e.name.startsWith("diagnostico-comparison-") && e.bytes<=65536 && (work==null || work.evidence.any{AuditWorkPolicy.matches(it,e)})}
  val pair=suggestComparison(entries){store.diagnostic(it)}?:error("No hay dos lecturas compatibles verificables entre las últimas 100 copias del alcance")
  loadSavedComparison(store,pair.baseline.entry,pair.current.entry).diagnostic()
 }
}
