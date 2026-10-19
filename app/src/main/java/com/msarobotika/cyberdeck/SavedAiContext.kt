package com.msarobotika.cyberdeck

import java.time.Instant

data class SavedAiContext(val entry:Evidence,val result:DiagnosticResult,val verifiedAt:String){
 fun input()=result.copy(details=listOf("SHA-256 de la fuente" to entry.sha256,"ID de la fuente" to entry.id,"Verificada" to verifiedAt)+result.details)
}
fun loadSavedAiContext(store:EvidenceStore,entry:Evidence):SavedAiContext {
 val result=store.diagnostic(entry)
 require(result.kind!="ai"){"Selecciona una lectura o comparación, no una interpretación de IA"}
 return SavedAiContext(entry,result,Instant.now().toString())
}
