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
 private fun shot(name:String){File(ui.activity.filesDir,"validation/$name").apply{parentFile?.mkdirs()}.outputStream().use{androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()!!.compress(Bitmap.CompressFormat.PNG,100,it)}}
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
 @Test fun dictatedMagneticRequestReturnsToComposerSurvivesRecreationAndRunsRealSensor(){
  ui.activity.runOnUiThread{ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
  ui.openDeck("Agente")
  val instrument=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
  val phrase="mírame el campo magnético del cuarto de baño"
  // Contract fixture only: no claim to have tested microphone transcription.
  val reply=android.content.Intent().putStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS,arrayListOf(phrase))
  val monitor=instrument.addMonitor(android.content.IntentFilter(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH),android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_OK,reply),true)
  try{
   ui.onNodeWithTag("agent-dictate").performScrollTo().performClick()
   ui.waitUntil(10000){monitor.hits>0}
   ui.onNodeWithText("Qué quieres que haga CiberDeck").performScrollTo().assertTextContains(phrase)
  }finally{instrument.removeMonitor(monitor)}
  ui.activityRule.scenario.recreate()
  ui.onNodeWithText("Qué quieres que haga CiberDeck").performScrollTo().assertTextContains(phrase)
  ui.onNodeWithTag("agent-magnetic-preview").performScrollTo().assertIsDisplayed()
  val session=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
  ui.waitUntil(15000){!session.ai.value.busy}
  assertFalse(session.ai.value.loaded)
  ui.onNodeWithText("Enviar petición").performScrollTo().assertIsEnabled().performClick()
  ui.waitUntil(15000){!session.agent.value.running && session.agent.value.results.isNotEmpty()}
  assertEquals(listOf(AgentTool.MAGNETIC),session.agent.value.plan!!.steps)
  val sensor=ui.activity.getSystemService(android.hardware.SensorManager::class.java).getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD)
  val step=session.agent.value.results.single()
  if(sensor==null){assertNull(step.result);assertTrue(step.error.contains("magnetómetro"))}
  else{
   assertTrue(step.error.isBlank());assertEquals("magnetic",step.result!!.kind);assertEquals(6,session.magnetic.value.size)
   assertTrue(step.result.summary.contains("µT"));ui.onNodeWithTag("agent-answer").performScrollTo().assertTextEquals(step.result.summary)
  }
  File(ui.activity.filesDir,"validation/agent-magnetic-034.json").apply{parentFile?.mkdirs()}.writeText(org.json.JSONObject().put("speechContractFixture",true).put("microphoneTranscriptionTested",false).put("sensorAvailable",sensor!=null).put("request",phrase).put("result",step.result?.toEvidenceJson()).put("error",step.error).toString())
  ui.onNodeWithTag("deck-list").performScrollToIndex(0);shot("ciberdeck-agent-magnetic-034.png")
 }
 @Test fun cancelledDictationDoesNotSendAnyTask(){
  ui.openDeck("Agente")
  val instrument=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
  val monitor=instrument.addMonitor(android.content.IntentFilter(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH),android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED,null),true)
  try{ui.onNodeWithTag("agent-dictate").performScrollTo().performClick();ui.waitUntil(10000){monitor.hits>0};ui.onNodeWithText("Dictado cancelado. No se ha enviado ninguna tarea.").performScrollTo().assertIsDisplayed();ui.onNodeWithText("Enviar petición").performScrollTo().assertIsNotEnabled();assertTrue(ViewModelProvider(ui.activity)[DeckSessionModel::class.java].agent.value.results.isEmpty())}finally{instrument.removeMonitor(monitor)}
 }
}
