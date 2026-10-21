package com.msarobotika.cyberdeck

import java.time.Instant

enum class FieldChange(val label:String){SAME("Sin cambios"),CHANGED("Modificados"),ADDED("Añadidos"),REMOVED("Ausentes")}
data class FieldDifference(val key:String,val before:List<String>,val after:List<String>,val change:FieldChange)
data class SavedComparison(val first:Evidence,val second:Evidence,val baseline:DiagnosticResult,val current:DiagnosticResult,val rows:List<FieldDifference>,val verifiedAt:String=Instant.now().toString()){
 fun count(change:FieldChange)=rows.count{it.change==change}
 fun diagnostic():DiagnosticResult {
  fun brief(values:List<String>)=values.joinToString(" | ").take(240).ifEmpty{"(sin valor)"}
  val changes=rows.filter{it.change!=FieldChange.SAME}
  fun build(limit:Int)=DiagnosticResult("comparison",baseline.target.take(512),verifiedAt,"${changes.size} campos diferentes de ${rows.size}; comparación textual, no detección de ataques",listOf(
   "Tipo comparado" to baseline.kind.take(128),"Lectura A" to baseline.checkedAt.take(100),"Lectura B" to current.checkedAt.take(100),
   "Evidencia A" to "${first.id} · SHA-256 ${first.sha256}","Evidencia B" to "${second.id} · SHA-256 ${second.sha256}",
   "Resumen A" to baseline.summary.take(400),"Resumen B" to current.summary.take(400),
   "Límite" to "Hasta $limit diferencias; valores abreviados a 240 caracteres. ${maxOf(0,changes.size-limit)} diferencias adicionales no incluidas. Consulta los originales verificados para el contenido completo.")+
   changes.take(limit).map{it.key.take(120) to "${it.change.label} · A: ${brief(it.before)} · B: ${brief(it.after)}"}+
   ("Método" to "Coincidencia exacta de tipo y destino declarados. Compara grupos de valores por etiqueta, preservando duplicados e ignorando su orden. Fechas y resúmenes se muestran aparte. Cambios de recursos, latencia o esquema no acreditan un incidente ni la identidad del destino."))
  var limit=minOf(50,changes.size);var report=build(limit)
  while(report.toEvidenceJson().toByteArray(Charsets.UTF_8).size>60000 && limit>0){limit--;report=build(limit)}
  require(report.toEvidenceJson().toByteArray(Charsets.UTF_8).size<=65536){"Comparación demasiado grande para guardar"}
  return report
 }
}
object ComparisonPolicy {
 fun compare(a:DiagnosticResult,b:DiagnosticResult):List<FieldDifference>{
  require(a.kind.isNotBlank() && a.kind==b.kind && a.target.isNotBlank() && a.target==b.target){"Elige diagnósticos del mismo tipo y destino exacto"}
  require(a.details.size<=256 && b.details.size<=256){"Demasiados campos para comparar"}
  val first=a.details.groupBy({it.first},{it.second});val second=b.details.groupBy({it.first},{it.second})
  return (first.keys+second.keys).sorted().map{key->
   val before=first[key].orEmpty();val after=second[key].orEmpty()
   FieldDifference(key,before,after,when{key !in first->FieldChange.ADDED;key !in second->FieldChange.REMOVED;before.sorted()==after.sorted()->FieldChange.SAME;else->FieldChange.CHANGED})
  }
 }
}
fun loadSavedComparison(store:EvidenceStore,a:Evidence,b:Evidence):SavedComparison {
 require(a.id!=b.id){"Selecciona dos copias distintas"}
 val first=store.diagnostic(a);val second=store.diagnostic(b)
 return SavedComparison(a,b,first,second,ComparisonPolicy.compare(first,second))
}
