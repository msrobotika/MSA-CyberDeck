package com.msarobotika.cyberdeck

import java.time.Instant
import java.util.concurrent.CancellationException

data class FieldInvestigation(val pairs:List<SavedComparison>,val verified:Int,val unreadable:Int,val unpaired:Int,val checkedAt:String=Instant.now().toString()) {
 val changed get()=pairs.sumOf{p->p.rows.count{it.change!=FieldChange.SAME}}
 fun diagnostic():DiagnosticResult {
  fun build(limit:Int):DiagnosticResult=DiagnosticResult("investigation","Lecturas guardadas",checkedAt,
  "$changed campos diferentes en ${pairs.size} parejas verificadas; no acredita ataques",
  listOf("Copias verificadas" to "$verified","Copias no legibles" to "$unreadable","Grupos sin pareja" to "$unpaired",
   "Método" to "Hasta 100 copias por orden de guardado, dos últimas de cada tipo y destino exactos. SHA-256 verificado al leer. El orden de guardado no demuestra orden de observación ni identidad física.",
   "Parejas en este informe" to "$limit de ${pairs.size}; consulta la misión para el resto",
   "Alcance" to "Sin nuevas mediciones ni tráfico. Diferencias textuales, sin atribución de amenazas. Consulta cada pareja para ver valores. No incluye contenido de los campos originales.")+
   pairs.take(limit).flatMap{p->listOf("Pareja / ${p.baseline.kind.take(64)}" to "Destino: ${p.baseline.target.take(256)}; ${p.rows.count{it.change!=FieldChange.SAME}} diferencias",
    "Fuente A" to "${p.first.id} · SHA-256 ${p.first.sha256} · lectura ${p.baseline.checkedAt.take(100)}",
    "Fuente B" to "${p.second.id} · SHA-256 ${p.second.sha256} · lectura ${p.current.checkedAt.take(100)}")})
  var limit=minOf(50,pairs.size);var report=build(limit)
  while(report.toEvidenceJson().toByteArray(Charsets.UTF_8).size>60000 && limit>0){limit--;report=build(limit)}
  return report
 }
}
/** Input newest first. The supplied reader must verify each source before returning it. */
fun investigateSavedReadings(newestFirst:List<Evidence>,read:(Evidence)->DiagnosticResult):FieldInvestigation {
 var unreadable=0
 val readings=newestFirst.take(100).distinctBy{it.id}.mapNotNull{entry->
  try{val result=read(entry)
   if(result.kind.isBlank() || result.target.isBlank() || result.kind in setOf("comparison","investigation") || result.details.size>256)null
   else ComparisonReading(entry,result)
  }catch(e:CancellationException){throw e}catch(_:Exception){unreadable++;null}
 }
 val groups=readings.groupBy{it.diagnostic.kind to it.diagnostic.target}.values
 val pairs=groups.filter{it.size>=2}.map{g->val a=g[1];val b=g[0];SavedComparison(a.entry,b.entry,a.diagnostic,b.diagnostic,ComparisonPolicy.compare(a.diagnostic,b.diagnostic))}
 return FieldInvestigation(pairs,readings.size,unreadable,groups.count{it.size<2})
}
