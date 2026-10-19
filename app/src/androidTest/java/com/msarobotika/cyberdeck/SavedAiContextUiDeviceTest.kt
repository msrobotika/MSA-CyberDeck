package com.msarobotika.cyberdeck
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
class SavedAiContextUiDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun verifiedEvidenceCanBeChosenAndRemovedWithoutGenerating(){
  val store=EvidenceStore(ui.activity);val entry=store.list().last{it.name.startsWith("diagnostico-phone-")}
  ui.openDeck("Evidencias")
  ui.onNodeWithText("Buscar por nombre, fecha o hash").performScrollTo().performTextInput(entry.sha256)
  ui.onNodeWithText("Buscar por nombre, fecha o hash").performImeAction()
  ui.onNodeWithTag("deck-list").performScrollToNode(hasTestTag("evidence-${entry.id}"))
  ui.onNodeWithTag("evidence-ai-${entry.id}").performScrollTo().performClick()
  ui.waitUntil(15000){ui.onAllNodesWithText("FUENTE GUARDADA").fetchSemanticsNodes().isNotEmpty()}
  val session=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
  assertEquals(entry.id,session.aiSource.value!!.entry.id)
  assertFalse(session.ai.value.loaded);assertTrue(session.ai.value.output.isEmpty())
  ui.onNodeWithText("Preparar informe").performScrollTo().performClick()
  ui.onNodeWithText("Pregunta al analista").performScrollTo().assertTextContains("Redacta una nota breve",substring=true)
  ui.onNodeWithText("Retirar contexto guardado").performScrollTo().performClick()
  ui.waitUntil(5000){session.aiSource.value==null}
  assertTrue(store.verifiedFile(entry).isFile)
 }
}
