package com.msarobotika.cyberdeck

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class DeckExperienceDeviceTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private fun shot(name:String) {
        val folder=File(ui.activity.filesDir,"validation").apply{mkdirs()}
        File(folder,"ux-$name.png").outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun mainNavigationSearchAndToolBackWorkWithoutHorizontalTabs() {
        ui.onNodeWithTag("home-phone").assertIsDisplayed()
        shot("home")
        for(root in listOf("Resumen","Herramientas","Evidencias","Informes"))ui.onNodeWithTag("navigate-$root").assertIsDisplayed()
        ui.openDeck("Herramientas")
        shot("tools")
        ui.onNodeWithText("Buscar una herramienta").performTextInput("NFC")
        ui.onNodeWithText("Buscar una herramienta").performImeAction()
        ui.onNodeWithTag("tool-NFC").performScrollTo().performClick()
        ui.onNodeWithContentDescription("Volver a herramientas").assertIsDisplayed().performClick()
        ui.onNodeWithText("Buscar una herramienta").assertIsDisplayed()
        ui.openDeck("Evidencias")
        shot("evidence")
        ui.openDeck("Informes")
        ui.onNodeWithText("Nombre del trabajo").performScrollTo().assertExists()
        shot("report")
    }
    @Test fun reportDoesNotEmbedChangedDiagnosticAsVerifiedContent() {
        val store=EvidenceStore(ui.activity)
        val source=DiagnosticResult("nfc","LAB","2026-10-17","Original fixture",emptyList()).toEvidenceJson().toByteArray()
        val entry=store.saveBytes("diagnostico-nfc-ux-integrity.json",source).last()
        try {
            File(ui.activity.filesDir,"evidence/${entry.id}").writeText("changed content")
            val report=store.report(listOf(entry),ReportContext("LAB","fixture",""))
            assertTrue(report.contains("NO VERIFICADA"));assertFalse(report.contains("Resultado de diagnóstico (contenido guardado)"))
        } finally {store.delete(entry.id)}
    }
    @Test fun completedNfcObservationSurvivesActivityRecreation() {
        ui.openDeck("NFC")
        ui.runOnUiThread {
            androidx.lifecycle.ViewModelProvider(ui.activity)[DeckSessionModel::class.java].nfc.value =
                NfcSnapshot(result=DiagnosticResult("nfc","Fixture local","2026-10-17",
                    "Observación de prueba retenida",listOf("Tecnologías" to "NfcA")))
        }
        ui.onNodeWithText("Observación de prueba retenida").assertIsDisplayed()
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("Observación de prueba retenida").assertIsDisplayed()
        ui.onNodeWithText("Guardar informe NFC").assertIsDisplayed()
    }
}
