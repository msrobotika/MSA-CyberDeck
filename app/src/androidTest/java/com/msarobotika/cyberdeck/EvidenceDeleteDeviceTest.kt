package com.msarobotika.cyberdeck

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import androidx.compose.ui.semantics.SemanticsActions

class EvidenceDeleteDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Test fun deletionRemovesOnlyPrivateCopyAndSurvivesReload() {
        val store = EvidenceStore(ui.activity)
        val before = store.list()
        val original = File(ui.activity.cacheDir,"delete-test-original.txt").apply { writeText("Generated original fixture") }
        val entry = store.saveBytes("delete-validation.txt",original.readBytes()).last()
        try {
            assertTrue(File(ui.activity.filesDir,"evidence/${entry.id}").exists())
            store.delete(entry.id)
            assertFalse(File(ui.activity.filesDir,"evidence/${entry.id}").exists())
            assertEquals(before, EvidenceStore(ui.activity).list())
            assertEquals("Generated original fixture",original.readText())
        } finally { if (store.list().any { it.id == entry.id }) store.delete(entry.id); original.delete() }
    }
    @Test fun interruptedDeletionBeforeIndexCommitIsRecovered() {
        val store = EvidenceStore(ui.activity)
        val entry = store.saveBytes("interrupted-delete-validation.txt","fixture".toByteArray()).last()
        val original = File(ui.activity.filesDir,"evidence/${entry.id}")
        val staged = File(ui.activity.filesDir,"evidence/.deleted-${entry.id}")
        try {
            assertTrue(original.renameTo(staged))
            assertTrue(EvidenceStore(ui.activity).list().any { it.id == entry.id })
            assertTrue(original.exists()); assertFalse(staged.exists())
        } finally { store.delete(entry.id) }
    }
    @Test fun cancellationKeepsCopyAndConfirmationDeletesFilteredFixture() {
        val store = EvidenceStore(ui.activity)
        val suffix = "delete-ui-${System.currentTimeMillis()}"
        val entry = store.saveBytes("$suffix.txt","fixture".toByteArray()).last()
        try {
            ui.activityRule.scenario.recreate()
            ui.openDeck("Evidencias")
            ui.onNodeWithText("Buscar por nombre, fecha o hash").performTextInput(suffix)
            ui.runOnUiThread {
                ui.activity.currentFocus?.let { focused ->
                    ui.activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                        .hideSoftInputFromWindow(focused.windowToken, 0)
                    focused.clearFocus()
                }
            }
            ui.waitForIdle()
            ui.onNodeWithText("Gestionar").performScrollTo().performClick()
            ui.onNodeWithText("Borrar resultados visibles").performScrollTo()
            ui.onNodeWithText("Borrar resultados visibles").performSemanticsAction(SemanticsActions.OnClick) { it() }
            ui.onNodeWithText("Conservar copias").performClick()
            assertTrue(store.list().any { it.id == entry.id })
            ui.onNodeWithText("Borrar resultados visibles").performScrollTo()
            ui.onNodeWithText("Borrar resultados visibles").performSemanticsAction(SemanticsActions.OnClick) { it() }
            ui.onNodeWithText("Borrar definitivamente").performClick()
            ui.waitUntil(5000) { store.list().none { it.id == entry.id } }
            assertFalse(File(ui.activity.filesDir,"evidence/${entry.id}").exists())
        } finally { if(store.list().any { it.id == entry.id }) store.delete(entry.id) }
    }
}
