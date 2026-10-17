package com.msarobotika.cyberdeck

import org.json.JSONObject
import org.json.JSONArray

fun DiagnosticResult.toEvidenceJson(): String = JSONObject()
    .put("format", "msa.cyberdeck.diagnostic.v1")
    .put("kind", kind).put("target", target).put("checkedAt", checkedAt)
    .put("summary", summary)
    .put("details", JSONArray().apply {
        details.forEach { (key, value) -> put(JSONObject().put("key", key).put("value", value)) }
    }).toString(2)
