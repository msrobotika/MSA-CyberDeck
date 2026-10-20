package com.msarobotika.cyberdeck

import java.text.Normalizer
import java.util.Locale

object DiagnosticPresentation {
    private fun words(text:String)=Normalizer.normalize(text,Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT)
    fun details(result:DiagnosticResult,query:String):List<Pair<String,String>> {
        val terms=words(query).trim().split(Regex("\\s+")).filter{it.isNotBlank()}
        return result.details.filter{(key,value)->val text=words("$key $value");terms.all{text.contains(it)}}
    }
    fun summary(result:DiagnosticResult):String = listOf(
        "CiberDeck · resumen de una observación",
        "Tipo: ${result.kind}","Destino: ${result.target}","Lectura: ${result.checkedAt}",result.summary,
        "Resumen sin campos técnicos. Conserva el informe completo desde CiberDeck."
    ).joinToString("\n")
}
