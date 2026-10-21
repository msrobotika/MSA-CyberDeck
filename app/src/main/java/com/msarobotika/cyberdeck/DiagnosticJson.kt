package com.msarobotika.cyberdeck

import org.json.JSONObject
import org.json.JSONArray

fun diagnosticFromJson(raw: String): DiagnosticResult {
    require(raw.toByteArray(Charsets.UTF_8).size <= 65536) { "Informe superior a 64 KiB" }
    val data = JSONObject(raw)
    require(data.getString("format") == "msa.cyberdeck.diagnostic.v1") { "Formato de diagnóstico no compatible" }
    val details = data.getJSONArray("details")
    require(details.length() <= 256) { "Demasiados campos de diagnóstico" }
    return DiagnosticResult(data.getString("kind"), data.getString("target"), data.getString("checkedAt"),
        data.getString("summary"), (0 until details.length()).map {
            val field = details.getJSONObject(it)
            field.getString("key") to field.getString("value")
        })
}

/** Compares observations, not authenticity, authorization or the unobserved card memory. */
fun compareNfc(first: DiagnosticResult, second: DiagnosticResult): List<Pair<String, String>> {
    require(first.kind == "nfc" && second.kind == "nfc") { "Selecciona dos informes NFC" }
    val keys = listOf("Identificador observado", "Tecnologías", "SHA-256 NDEF")
    return keys.map { key ->
        val a = first.details.firstOrNull { it.first == key }?.second
        val b = second.details.firstOrNull { it.first == key }?.second
        key to when {
            a == null || b == null -> "No comparable: campo ausente en una o ambas lecturas"
            a == b -> "Coincide en ambas lecturas"
            else -> "Diferente\nReferencia: $a\nOtra lectura: $b"
        }
    }
}

fun DiagnosticResult.toEvidenceJson(): String = JSONObject()
    .put("format", "msa.cyberdeck.diagnostic.v1")
    .put("kind", kind).put("target", target).put("checkedAt", checkedAt)
    .put("summary", summary)
    .put("details", JSONArray().apply {
        details.forEach { (key, value) -> put(JSONObject().put("key", key).put("value", value)) }
    }).toString(2)
