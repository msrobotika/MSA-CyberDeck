package com.msarobotika.cyberdeck

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class EvidenceBrowserDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Test fun modifiedDiagnosticIsRejectedBeforeOpening() {
        val store = EvidenceStore(ui.activity)
        val fixture = DiagnosticResult("nfc", "LAB", "2026-10-17", "Integrity fixture", emptyList())
        val raw = fixture.toEvidenceJson().toByteArray()
        val entry = store.saveBytes("diagnostico-nfc-integrity-validation.json", raw).last()
        val file = File(ui.activity.filesDir, "evidence/${entry.id}")
        try {
            val altered = raw.copyOf().apply { this[lastIndex] = ' '.code.toByte() }
            file.writeBytes(altered)
            val failure = runCatching { store.diagnostic(entry) }.exceptionOrNull()
            assertTrue(failure is IllegalArgumentException)
            assertTrue(failure?.message.orEmpty().contains("SHA-256"))
        } finally { file.writeBytes(raw) }
        assertEquals(fixture, store.diagnostic(entry))
    }
    @Test fun savedNfcReportCanBeRecoveredAfterActivityRecreation() {
        val suffix = System.currentTimeMillis().toString()
        val name = "diagnostico-nfc-validation-$suffix.json"
        val result = DiagnosticResult("nfc", "LAB TEST FIXTURE", "2026-10-17", "NFC de laboratorio para validar historial",
            listOf("Tecnologías" to "NfcA", "Identificador observado" to "LAB-FIXTURE"))
        EvidenceStore(ui.activity).saveBytes(name, result.toEvidenceJson().toByteArray())
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("NFC").performScrollTo().performClick()
        ui.onNodeWithText("Historial NFC guardado").performScrollTo().performClick()
        ui.onNodeWithText("Buscar por nombre, fecha o hash").performTextInput(suffix)
        ui.onNodeWithText(name).performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Abrir informe guardado").performScrollTo().performClick()
        ui.waitUntil(5000) { ui.onAllNodesWithText(result.summary).fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText(result.summary).assertIsDisplayed()
        ui.onNodeWithText("Cerrar informe").performClick()
    }
}
