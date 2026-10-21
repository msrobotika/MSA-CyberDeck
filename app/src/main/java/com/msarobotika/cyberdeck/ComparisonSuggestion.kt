package com.msarobotika.cyberdeck

import java.util.concurrent.CancellationException

data class ComparisonReading(val entry:Evidence,val diagnostic:DiagnosticResult)
data class ComparisonSuggestion(val baseline:ComparisonReading,val current:ComparisonReading)

/** Input is newest first by local save order, not by the device's claimed observation date. */
fun suggestComparison(newestFirst:List<Evidence>,read:(Evidence)->DiagnosticResult):ComparisonSuggestion? {
 val readings=newestFirst.take(100).distinctBy{it.id}.mapNotNull{entry->
  try{
   val result=read(entry)
   if(result.kind.isBlank() || result.target.isBlank() || result.kind=="comparison" || result.details.size>256)null
   else ComparisonReading(entry,result)
  }catch(e:CancellationException){throw e}catch(_:Exception){null}
 }
 // Linked grouping preserves the newest current reading, even if its mate is much older.
 val group=readings.groupBy{it.diagnostic.kind to it.diagnostic.target}.values.firstOrNull{it.size>=2}?:return null
 return ComparisonSuggestion(group[1],group[0])
}
