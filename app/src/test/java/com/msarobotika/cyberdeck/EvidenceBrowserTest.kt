package com.msarobotika.cyberdeck

import org.junit.Assert.*
import org.junit.Test

class EvidenceBrowserTest {
    private fun fixture(details: List<Pair<String, String>>) = DiagnosticResult("nfc", "LAB", "2026-10-17", "Prueba", details)
    @Test fun diagnosticRoundTripPreservesObservedFields() {
        val result = fixture(listOf("Tecnologías" to "NfcA", "Texto" to "á\n<texto>"))
        assertEquals(result, diagnosticFromJson(result.toEvidenceJson()))
    }
    @Test fun missingIdentifierDoesNotBecomeAnIdentityMatch() {
        val compared = compareNfc(fixture(emptyList()), fixture(emptyList()))
        assertTrue(compared.all { it.second.startsWith("No comparable") })
    }
    @Test fun comparisonSeparatesIdentifierAndContent() {
        val a = fixture(listOf("Identificador observado" to "A", "SHA-256 NDEF" to "123"))
        val b = fixture(listOf("Identificador observado" to "A", "SHA-256 NDEF" to "456"))
        val result = compareNfc(a,b).toMap()
        assertEquals("Coincide en ambas lecturas", result["Identificador observado"])
        assertTrue(result.getValue("SHA-256 NDEF").startsWith("Diferente"))
    }
    @Test fun filtersDoNotClaimToClassifyFileContents() {
        val entry = Evidence("id", "diagnostico-nfc-1.json", "abcd123", 50, "2026-10-17")
        assertTrue(evidenceMatches(entry,"ABCD", "NFC"))
        assertFalse(evidenceMatches(entry,"", "BLE"))
        assertFalse(evidenceMatches(entry,"absent", "Todas"))
    }
    @Test(expected = IllegalArgumentException::class) fun unsupportedFormatRejected() {
        diagnosticFromJson("""{"format":"unknown"}""")
    }
    @Test(expected = IllegalArgumentException::class) fun oversizedReportRejected() {
        diagnosticFromJson(" ".repeat(65537))
    }
}
