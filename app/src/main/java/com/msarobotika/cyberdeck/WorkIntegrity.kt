package com.msarobotika.cyberdeck

import java.time.Instant
import java.util.concurrent.CancellationException

enum class WorkCopyState(val label:String) {
 VERIFIED("Verificadas"), UNVERIFIED("No verificables"), INDEX_MISMATCH("Sin referencia coincidente")
}
data class WorkCopyCheck(val id:String,val name:String,val state:WorkCopyState)
data class WorkVerification(val work:AuditWork,val at:String,val checks:List<WorkCopyCheck>) {
 fun count(state:WorkCopyState)=checks.count{it.state==state}
}

/** Checks a fixed work snapshot without writing files or making a security assessment. */
fun verifyWorkCopies(work:AuditWork,store:EvidenceStore,cancelled:()->Boolean={false}):WorkVerification {
 val entries=store.list()
 val checks=work.evidence.map{ref->
  if(cancelled())throw CancellationException("Verificación detenida")
  val entry=entries.firstOrNull{AuditWorkPolicy.matches(ref,it)}
  val state=if(entry==null)WorkCopyState.INDEX_MISMATCH else {
   try{store.verifiedFile(entry);WorkCopyState.VERIFIED}catch(e:Exception){WorkCopyState.UNVERIFIED}
  }
  if(cancelled())throw CancellationException("Verificación detenida")
  WorkCopyCheck(ref.id,ref.name,state)
 }
 return WorkVerification(work,Instant.now().toString(),checks)
}
