package com.msarobotika.cyberdeck
import android.view.WindowManager
import android.graphics.Bitmap
import java.io.File
import org.junit.After
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
class AgentConversationDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private var originalOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
 private fun shot(name:String){ui.waitForIdle();val bitmap=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()?:error("No screenshot");File(ui.activity.filesDir,"validation/$name").apply{parentFile?.mkdirs()}.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
 @After fun restoreOrientation(){ui.activity.runOnUiThread{ui.activity.requestedOrientation=originalOrientation}}
 private fun session()=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
 @Before fun open(){originalOrientation=ui.activity.requestedOrientation;ui.activity.runOnUiThread{ui.activity.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT};ui.waitUntil(15000){ui.activity.resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT};ui.activity.runOnUiThread{ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)};ui.openDeck("Agente");ui.waitUntil(20000){session().ai.value.connected && !session().ai.value.busy}}
 @Test fun actualTaskBecomesSessionMemoryAndReuseOnlyPrefillsTheComposer(){
  ui.onNodeWithTag("agent-auto-voice").performScrollTo().assertIsOff()
  ui.onNodeWithTag("agent-task-connection").performScrollTo().performClick()
  ui.waitUntil(20000){!session().agent.value.running && session().agentRecent.value.size==1}
  val original=session().agent.value;val row=session().agentRecent.value.single()
  assertEquals(original.run!!.startedAt,row.id);assertEquals(AgentAnswerPolicy.primary(original)!!.result!!.summary,row.answer)
  assertTrue(session().agentLastSpokenId.value.isBlank())
  ui.onNodeWithTag("agent-recent").performScrollTo().performClick()
  ui.onNodeWithTag("agent-reuse-${row.id}").performScrollTo();shot("ciberdeck-conversation-035.png")
  ui.onNodeWithTag("agent-reuse-${row.id}").performClick()
  ui.onNodeWithText("Qué quieres que haga CiberDeck").performScrollTo().assertTextContains(row.request)
  ui.runOnIdle{assertEquals(original,session().agent.value);assertFalse(session().agent.value.running)}
  ui.onAllNodesWithTag("agent-summary").assertCountEquals(0)
  ui.onNodeWithTag("deck-list").performScrollToIndex(0);shot("ciberdeck-followup-035.png")
  ui.onNodeWithTag("agent-clear-recent").performScrollTo().performClick()
  ui.runOnIdle{assertTrue(session().agentRecent.value.isEmpty());assertEquals(original,session().agent.value)}
 }
 @Test fun optedInVoiceUsesOneCompletionAndSurvivesRecreationWithoutRepeatingIt(){
  ui.onNodeWithTag("agent-auto-voice").performScrollTo().performClick()
  ui.onNodeWithTag("agent-task-connection").performScrollTo().performClick()
  ui.waitUntil(20000){!session().agent.value.running && session().agent.value.run!=null && session().agentLastSpokenId.value.isNotBlank()}
  val spoken=session().agentLastSpokenId.value;assertEquals(session().agent.value.run!!.startedAt,spoken)
  ui.waitUntil(35000){ui.onAllNodesWithText("Lectura finalizada.").fetchSemanticsNodes().isNotEmpty()}
  ui.onNodeWithTag("agent-auto-voice").performScrollTo();shot("ciberdeck-voice-035.png")
  ui.activityRule.scenario.recreate()
  ui.runOnIdle{assertTrue(session().agentSpeakReplies.value);assertEquals(spoken,session().agentLastSpokenId.value);assertEquals(1,session().agentRecent.value.size)}
  ui.onNodeWithTag("agent-auto-voice").performScrollTo().performClick()
  ui.runOnIdle{assertFalse(session().agentSpeakReplies.value)}
 }
}
