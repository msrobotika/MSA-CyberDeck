package com.msarobotika.cyberdeck

import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AgentFocusDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun session()=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
 @Before fun keepVisible(){ui.activity.runOnUiThread{ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}}
 @Test fun preparedPlanHasOneFocusAndChooserDoesNotReplaceIt(){
  ui.openDeck("Agente")
  val original=AgentSession(request="Calcula mi subred",plan=AgentPlan(listOf(AgentTool.CIDR)),origin="UI fixture; no network reading")
  ui.runOnIdle{session().agent.value=original}
  ui.onAllNodesWithText("Qué quieres que haga CiberDeck").assertCountEquals(0)
  ui.onNodeWithText("IPv4 y prefijo CIDR").performScrollTo().assertIsDisplayed()
  ui.onNodeWithTag("agent-change-task").performScrollTo().performClick()
  ui.onNodeWithText("Qué quieres que haga CiberDeck").performScrollTo().assertIsDisplayed()
  ui.onAllNodesWithText("IPv4 y prefijo CIDR").assertCountEquals(0)
  ui.runOnIdle{assertEquals(original,session().agent.value)}
  ui.onNodeWithTag("agent-change-task").performScrollTo().performClick()
  ui.onAllNodesWithText("Qué quieres que haga CiberDeck").assertCountEquals(0)
  ui.runOnIdle{assertEquals(original,session().agent.value)}
 }
 @Test fun completedActualTaskHidesComposerUntilRequested(){
  ui.openDeck("Agente")
  ui.onNodeWithTag("agent-task-connection").performScrollTo().performClick()
  ui.waitUntil(20000){!session().agent.value.running && session().agent.value.results.size==2}
  ui.onNodeWithTag("agent-summary").performScrollTo().assertIsDisplayed()
  ui.onAllNodesWithText("Qué quieres que haga CiberDeck").assertCountEquals(0)
  val run=session().agent.value.run
  assertNotNull(run)
  ui.onNodeWithTag("agent-change-task").performScrollTo().performClick()
  ui.onNodeWithText("Qué quieres que haga CiberDeck").performScrollTo().assertIsDisplayed()
  ui.runOnIdle{assertEquals(run,session().agent.value.run)}
 }
}
