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

class AgentNavigationDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun assertAgent(){
  val session=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
  ui.waitUntil(20000){session.agentMode.value && session.ai.value.connected && !session.ai.value.busy}
  ui.onNodeWithTag("screen-title").assertTextEquals("Agente")
  if(session.agent.value.plan!=null || session.agent.value.run!=null){
   ui.onNodeWithTag("agent-change-task").performScrollTo().assertIsDisplayed()
   ui.onAllNodesWithText("Qué quieres que haga CiberDeck").assertCountEquals(0)
  } else {
   ui.onNodeWithText("Qué quieres que haga CiberDeck").performScrollTo().assertIsDisplayed()
   ui.onNodeWithText("Elegir funciones · 30").performScrollTo().assertIsDisplayed()
  }
 }
 @Test fun homeOpensActualAgentWithoutHiddenModeSwitch(){
  ui.onNodeWithTag("home-agent").performScrollTo().performClick();assertAgent()
  ui.onNodeWithTag("deck-list").performScrollToIndex(0)
  File(ui.activity.filesDir,"validation/ciberdeck-agent-entry.png").apply{parentFile?.mkdirs()}.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
 }
 @Test fun drawerAgentIsVisibleWithoutExpandingAnyArea(){
  ui.onNodeWithContentDescription("Abrir menú").performClick()
  ui.onNodeWithTag("drawer-agent").assertIsDisplayed().performClick();assertAgent()
 }
 @Test fun toolkitEntryAndAnalystSwitchPreserveThePreparedPlan(){
  ui.openDeck("Agente");assertAgent()
  val session=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
  val plan=AgentPlan(listOf(AgentTool.PHONE,AgentTool.CONNECTION))
  ui.runOnIdle{session.agent.value=AgentSession(request="Plan propio conservado",plan=plan)}
  ui.onNodeWithText("Analista",substring=false).performScrollTo().performClick()
  ui.onNodeWithTag("screen-title").assertTextEquals("Asistente IA")
  ui.waitUntil(20000){!session.ai.value.busy && session.ai.value.connected}
  ui.onNode(hasText("Agente",substring=false) and hasClickAction() and !hasTestTag("drawer-agent")).performScrollTo().performClick();assertAgent()
  assertEquals(plan,session.agent.value.plan)
 }
}
