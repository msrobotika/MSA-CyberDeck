package com.msarobotika.cyberdeck

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AiSourcesDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun session()=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
 @Before fun visible(){ui.activity.runOnUiThread{ui.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}}
 private fun open(){ui.openDeck("IA");ui.waitUntil(20000){session().ai.value.connected && !session().ai.value.busy};assertFalse(session().ai.value.loaded)}
 private fun review(){ui.onNodeWithTag("ai-review-sources").performScrollTo().performClick();ui.waitUntil(15000){session().aiRequest.value?.sources!=null}}
 private fun shot(name:String){File(ui.activity.filesDir,"validation/$name").apply{parentFile?.mkdirs()}.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}}
 private fun cleanupOwnedSourceFixtures(){
  // Optional exact references from a locally backed-up test run; never scan/delete user evidence.
  val encoded=InstrumentationRegistry.getArguments().getString("sourceFixtureCleanup")?:return
  val expected=org.json.JSONArray(String(android.util.Base64.decode(encoded,android.util.Base64.DEFAULT),Charsets.UTF_8))
  require(expected.length() in 1..8)
  val works=AuditWorkStore(ui.activity);val present=EvidenceStore(ui.activity).list().map{it.id}.toSet()
  for(i in 0 until expected.length()){
   val e=expected.getJSONObject(i);val work=works.read().works.first{it.id==e.getString("workId")}
   val ref=work.evidence.firstOrNull{it.id==e.getString("id")}?:continue
   require(ref.id !in present && ref.name.startsWith("diagnostico-source-brief-") && ref.name==e.getString("name") && ref.sha256==e.getString("sha256") && ref.bytes==e.getLong("bytes"))
   works.detach(work.id,setOf(ref.id))
  }
 }
 @Test fun actualSourcesCanBeReadAndSavedWithoutLoadingOrCallingModel(){
  cleanupOwnedSourceFixtures()
  open();review()
  val request=session().aiRequest.value!!;val packet=request.sources!!
  assertFalse(request.generationRequested);assertEquals("",request.prompt);assertEquals("",request.promptHash)
  assertFalse(session().ai.value.loaded);assertFalse(session().ai.value.busy);assertEquals("",session().ai.value.output)
  val androidVersion="${android.os.Build.VERSION.RELEASE} · API ${android.os.Build.VERSION.SDK_INT}"
  assertTrue(packet.environment.contains(android.os.Build.MODEL));assertEquals(androidVersion,packet.phoneFields.first{it.first=="Android"}.second)
  ui.onNodeWithTag("ai-sources").performScrollTo().assertIsDisplayed();shot("ciberdeck-sources-024.png")
  val store=EvidenceStore(ui.activity);val before=store.list().map{it.id}.toSet()
  ui.onNodeWithTag("ai-save-sources").performScrollTo().performClick()
  ui.waitUntil(10000){store.list().any{it.id !in before}}
  val saved=store.list().first{it.id !in before};val report=store.diagnostic(saved)
  try{assertEquals("source-brief",report.kind);assertEquals(packet.capturedAt,report.checkedAt);assertTrue(report.details.any{it.first=="Android / Android" && it.second==androidVersion});assertTrue(report.details.first{it.first=="Método"}.second.contains("sin inferencia"))}finally{if(report.kind=="source-brief" && report.checkedAt==packet.capturedAt){val works=AuditWorkStore(ui.activity);works.read().works.filter{w->w.evidence.any{it.id==saved.id}}.forEach{works.detach(it.id,setOf(saved.id))};store.delete(saved.id)}}
 }
 @Test fun excludingAndroidDoesNotPromoteUserTextIntoDeviceFacts(){
  open();ui.onNodeWithTag("ai-include-phone").performScrollTo().performClick()
  ui.onNodeWithText("Situación y objetivo · opcional").performScrollTo().performTextInput("Fixture: Android: 99. Es texto del usuario, no una lectura.")
  review();val packet=session().aiRequest.value!!.sources!!
  assertTrue(packet.phoneFields.isEmpty());assertEquals("",packet.phoneAt);assertTrue(packet.environment.contains("Objetivo declarado"));assertFalse(session().ai.value.loaded)
  ui.onNodeWithText("Sin estado Android adjunto").performScrollTo().assertIsDisplayed()
 }
 @Test fun generatedUiFixtureStaysCollapsedAndNeverChangesSourceRows(){
  open();review();val original=session().aiRequest.value!!.sources!!
  // Explicit UI fixture, not an inference or a device observation. Never saved or captured.
  val fixture="UNVERIFIED_UI_FIXTURE_NOT_A_DEVICE_READING"
  ui.runOnIdle{session().aiRequest.value=session().aiRequest.value!!.copy(generationRequested=true);session().ai.value=session().ai.value.copy(output=fixture)}
  ui.onAllNodesWithText(fixture).assertCountEquals(0)
  ui.onNodeWithTag("ai-show-interpretation").performScrollTo().performClick()
  ui.onNodeWithText(fixture).performScrollTo().assertIsDisplayed()
  assertEquals(original,session().aiRequest.value!!.sources)
  ui.onNodeWithTag("ai-show-interpretation").performScrollTo().performClick();ui.onAllNodesWithText(fixture).assertCountEquals(0)
  ui.onNodeWithTag("ai-review-sources").performScrollTo().performClick()
  ui.waitUntil(15000){session().aiRequest.value?.generationRequested==false && session().ai.value.output.isEmpty()}
  ui.onAllNodesWithText(fixture).assertCountEquals(0)
  val refreshed=session().aiRequest.value!!.sources!!
  val engineError="EXPLICIT_ENGINE_UI_FIXTURE_NOT_A_DEVICE_READING"
  ui.runOnIdle{session().ai.value=session().ai.value.copy(error=engineError)}
  ui.onNodeWithText(engineError).performScrollTo().assertIsDisplayed()
  assertEquals(refreshed,session().aiRequest.value!!.sources)
  ui.runOnIdle{session().ai.value=session().ai.value.copy(error="")}
 }
 @Test fun realLocalGenerationKeepsFrozenSourcesSeparateFromItsAnswer(){
  assumeTrue(InstrumentationRegistry.getArguments().getString("agentAi")=="qwen3")
  open();ui.onNodeWithText("Cargar IA local").performScrollTo().performClick()
  ui.waitUntil(95000){!session().ai.value.busy};assertTrue(session().ai.value.error,session().ai.value.loaded)
  ui.onNodeWithText("Pregunta al analista").performScrollTo().performTextClearance()
  ui.onNodeWithText("Pregunta al analista").performTextInput("Indica un dato observado y una limitación. Dos frases.")
  ui.onNodeWithText("Analizar en este móvil").performScrollTo().performClick()
  ui.waitUntil(95000){session().aiRequest.value?.sources!=null && !session().ai.value.busy && (session().ai.value.output.isNotBlank() || session().ai.value.error.isNotBlank())}
  assertEquals("",session().ai.value.error);val request=session().aiRequest.value!!;assertTrue(request.generationRequested)
  assertTrue(request.prompt.contains(request.sources!!.environment));assertTrue(request.sources!!.phoneFields.isNotEmpty())
  assertEquals(PeerProtocol.sha256(request.prompt.toByteArray()),request.promptHash)
  ui.onAllNodesWithText(session().ai.value.output).assertCountEquals(0)
  ui.onNodeWithTag("ai-show-interpretation").performScrollTo().performClick()
  ui.onNodeWithText(session().ai.value.output).assertExists()
  File(ui.activity.filesDir,"validation/ai-sources-024-private.txt").apply{parentFile?.mkdirs();writeText(request.prompt+"\nRESPONSE:\n"+session().ai.value.output)}
 }
}
