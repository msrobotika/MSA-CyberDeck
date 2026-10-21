package com.msarobotika.cyberdeck
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.speech.RecognizerIntent
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
class AgentNetworkInputDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun session()=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
 @Before fun open(){ui.activity.runOnUiThread{ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)};ui.openDeck("Agente");ui.waitUntil(20000){session().ai.value.connected && !session().ai.value.busy};assertFalse(session().ai.value.loaded)}
 private fun finished(previous:String?=null){ui.waitUntil(20000){!session().agent.value.running && session().agent.value.results.size==2 && session().agent.value.run!=null && session().agent.value.run!!.startedAt!=previous};assertEquals(listOf("connection","resources"),session().agent.value.results.map{it.result?.kind});assertTrue(session().agent.value.results.all{it.error.isBlank()});assertFalse(session().ai.value.loaded);val source=session().agent.value.results.first().result!!;ui.onNodeWithTag("agent-network-transport").performScrollTo().assertTextEquals("Red observada: ${source.details.first{it.first=="Transporte"}.second}")}
 @Test fun exactWrittenWifiRequestWorksAlsoAsANewTaskFromContinuation(){
  ui.onNodeWithText("Qué quieres que haga CiberDeck").performScrollTo().performTextInput("analiza mi red wifi");ui.onNodeWithText("Enviar petición").performScrollTo().performClick();finished()
  val first=session().agent.value.run!!.startedAt
  ui.onNodeWithTag("agent-followup-input").performScrollTo().performTextReplacement("analiza mi red wifi");ui.onNodeWithTag("agent-followup-send").performScrollTo().performClick();finished(first)
  val current=session().agent.value.run!!.startedAt
  ui.onNodeWithTag("agent-followup-input").performScrollTo().performTextReplacement("no analices mi red wifi");ui.onNodeWithTag("agent-followup-send").performScrollTo().performClick();ui.waitUntil(5000){session().agentFollowupStatus.value.contains("No he reconocido")};assertEquals(current,session().agent.value.run!!.startedAt)
 }
 @Test fun wifiSpeechContractResultCanBeSubmittedWithoutAnAiModel(){
  val instrumentation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
  val phrase="analiza mi red Wi-Fi";val response=Intent().putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS,arrayListOf(phrase))
  val monitor=instrumentation.addMonitor(IntentFilter(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),Instrumentation.ActivityResult(Activity.RESULT_OK,response),true)
  try{ui.onNodeWithTag("agent-dictate").performScrollTo().performClick();ui.waitUntil(10000){monitor.hits>0};ui.onNodeWithText("Qué quieres que haga CiberDeck").performScrollTo().assertTextContains(phrase)}finally{instrumentation.removeMonitor(monitor)}
  ui.onNodeWithText("Enviar petición").performScrollTo().performClick();finished()
 }
 @Test fun unsupportedSecurityRequestGetsVisibleFeedbackInsteadOfPretendingToAudit(){
  ui.onNodeWithText("Qué quieres que haga CiberDeck").performScrollTo().performTextInput("analiza la seguridad de mi red wifi");ui.onNodeWithText("Enviar petición").performScrollTo().performClick();ui.onNodeWithTag("agent-submit-feedback").performScrollTo().assertIsDisplayed();assertTrue(session().agent.value.results.isEmpty());assertFalse(session().agent.value.running)
 }
}
