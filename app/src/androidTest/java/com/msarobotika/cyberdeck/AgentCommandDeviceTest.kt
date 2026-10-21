package com.msarobotika.cyberdeck
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.Test
class AgentCommandDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private var originalOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
 @Before fun portraitForScreenshots(){originalOrientation=ui.activity.requestedOrientation;ui.activity.runOnUiThread{ui.activity.requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT};ui.waitUntil(15000){ui.activity.resources.configuration.orientation==android.content.res.Configuration.ORIENTATION_PORTRAIT}}
 @After fun restoreOrientation(){ui.activity.runOnUiThread{ui.activity.requestedOrientation=originalOrientation}}
 private fun shot(name:String){File(ui.activity.filesDir,"validation/$name").apply{parentFile?.mkdirs()}.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}}
 @Test fun naturalRequestRunsRealToolsAndVoiceControlsDoNotRequireModelOrMicrophonePermission(){
  ui.activity.runOnUiThread{ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
  ui.openDeck("Agente")
  val session=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
  ui.waitUntil(20000){session.ai.value.connected && !session.ai.value.busy};assertFalse(session.ai.value.loaded)
  ui.onNodeWithTag("agent-command-center").assertExists()
  ui.onNodeWithTag("agent-hud-status").assertTextEquals("EN ESPERA")
  ui.onNodeWithTag("agent-dictate").performScrollTo().assertIsDisplayed()
  ui.onAllNodesWithTag("agent-team").assertCountEquals(0)
  ui.onNodeWithTag("deck-list").performScrollToIndex(0);shot("ciberdeck-agent-command-033.png")
  ui.onNodeWithText("Qué quieres que haga CiberDeck").performScrollTo().performTextInput("CiberDeck, ¿tengo Internet?")
  ui.onNodeWithText("Enviar petición").performScrollTo().performClick()
  ui.waitUntil(20000){!session.agent.value.running && session.agent.value.results.size==2}
  ui.onNodeWithTag("agent-hud-status").performScrollTo().assertTextEquals("TAREA FINALIZADA")
  assertEquals(listOf("connection","resources"),session.agent.value.results.map{it.result?.kind})
  assertTrue(session.agent.value.results.all{it.error.isBlank() && !it.manual && !it.awaiting})
  val connection=session.agent.value.results.first{it.tool==AgentTool.CONNECTION}.result!!
  ui.onNodeWithTag("agent-answer").performScrollTo().assertTextEquals(connection.summary)
  ui.onNodeWithTag("agent-speak").performScrollTo().assertExists()
  assertFalse(session.ai.value.loaded)
  ui.onNodeWithTag("deck-list").performScrollToIndex(0);shot("ciberdeck-agent-answer-033.png")
  val permissions=ui.activity.packageManager.getPackageInfo(ui.activity.packageName,android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
  assertFalse(permissions.contains("android.permission.RECORD_AUDIO"))
 }
 @Test fun speechEngineEitherFinishesLocalSampleOrOffersTextFallback(){
  ui.activity.runOnUiThread{ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
  lateinit var speaker:AgentSpeaker
  ui.runOnIdle{speaker=AgentSpeaker(ui.activity)}
  try{
   ui.waitUntil(20000){speaker.status.value!="Preparando voz del sistema…"}
   val available=speaker.ready.value
   if(available){ui.runOnIdle{speaker.speak("CiberDeck. Prueba de voz local.")};ui.waitUntil(20000){speaker.status.value=="Lectura finalizada." || speaker.status.value.contains("No se pudo")};assertEquals("Lectura finalizada.",speaker.status.value)}
   else{ui.runOnIdle{speaker.speak("Prueba")};assertTrue(speaker.status.value.contains("no disponible"))}
   File(ui.activity.filesDir,"validation/agent-voice-033.json").apply{parentFile?.mkdirs()}.writeText(org.json.JSONObject().put("localVoiceAvailable",available).put("status",speaker.status.value).put("audibleSoundPhysicallyVerified",false).toString())
  }finally{ui.runOnIdle{speaker.close()};assertFalse(speaker.ready.value)}
 }
}
