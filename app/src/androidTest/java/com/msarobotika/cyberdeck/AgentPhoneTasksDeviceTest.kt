package com.msarobotika.cyberdeck
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.ViewModelProvider
import android.graphics.Bitmap
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
class AgentPhoneTasksDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun session()=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
 private fun open(){ui.openDeck("Agente");ui.waitUntil(20000){session().ai.value.connected && !session().ai.value.busy};assertFalse(session().ai.value.loaded)}
 @Test fun oneTapReadsActualPhoneWithoutModelPeripheralOrPlanConfirmation(){
  open();ui.onNodeWithTag("agent-task-phone").performScrollTo().performClick()
  ui.waitUntil(25000){!session().agent.value.running && session().agent.value.results.size==5}
  val rows=session().agent.value.results;assertEquals(listOf("phone","connection","resources","phone-review","apk"),rows.map{it.result?.kind});assertTrue(rows.all{!it.awaiting && !it.manual});assertFalse(session().ai.value.loaded);assertEquals("Tarea del móvil",session().agent.value.origin)
  ui.onNodeWithTag("agent-summary").performScrollTo().assertIsDisplayed()
  ui.onNodeWithText("Revisión finalizada").assertExists()
  ui.onNodeWithText("5 de 5 pasos con resultado").assertExists()
  ui.onNodeWithText("Guardar ejecución como evidencia").performScrollTo()
  File(ui.activity.filesDir,"validation/ciberdeck-phone-task.png").apply{parentFile?.mkdirs()}.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
 }
 @Test fun plainConnectionRequestRunsOnPhoneWithoutModel(){
  open();ui.onNodeWithText("Qué quieres que haga CiberDeck").performScrollTo().performTextInput("Comprueba mi conexión")
  ui.onNodeWithText("Enviar petición",substring=false).performScrollTo().performClick()
  ui.waitUntil(15000){!session().agent.value.running && session().agent.value.results.size==2}
  assertEquals(listOf("connection","resources"),session().agent.value.results.map{it.result?.kind});assertFalse(session().ai.value.loaded)
 }
 @Test fun unrecognizedRequestOffersNextStepInsteadOfDemandingEsp(){
  open();ui.onNodeWithText("Qué quieres que haga CiberDeck").performScrollTo().performTextInput("Quiero investigar algo diferente")
  ui.onNodeWithText("Enviar petición",substring=false).performScrollTo().performClick()
  ui.waitUntil(5000){session().agent.value.notice.isNotBlank()};assertTrue(session().agent.value.error.isBlank());assertTrue(session().agent.value.results.isEmpty());assertFalse(session().agent.value.notice.contains("ESP"))
 }
 @Test fun summaryKeepsMethodsAccessibleAndReprepareDoesNotRerun(){
  open();ui.onNodeWithTag("agent-task-connection").performScrollTo().performClick()
  ui.waitUntil(15000){!session().agent.value.running && session().agent.value.results.size==2}
  ui.onAllNodesWithText("Ver resultado y método").assertCountEquals(0)
  ui.onNodeWithText("Ver detalles de resultados").performScrollTo().performClick()
  ui.onAllNodesWithText("Ver resultado y método").assertCountEquals(2)
  ui.onNodeWithText("Ocultar detalles de resultados").performScrollTo().performClick()
  ui.onAllNodesWithText("Ver resultado y método").assertCountEquals(0)
  ui.onNodeWithText("Preparar de nuevo").performScrollTo().performClick()
  ui.runOnIdle{assertNull(session().agent.value.run);assertFalse(session().agent.value.running);assertTrue(session().agent.value.results.isEmpty())}
  ui.onNodeWithText("Ejecutar este plan").performScrollTo().assertIsDisplayed()
 }
}
