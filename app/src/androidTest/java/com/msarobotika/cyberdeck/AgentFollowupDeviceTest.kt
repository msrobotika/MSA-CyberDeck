package com.msarobotika.cyberdeck
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
class AgentFollowupDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun session()=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
 @Before fun open(){ui.activity.runOnUiThread{ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)};ui.openDeck("Agente");ui.waitUntil(20000){session().ai.value.connected && !session().ai.value.busy};ui.onNodeWithTag("agent-task-connection").performScrollTo().performClick();ui.waitUntil(20000){!session().agent.value.running && session().agent.value.results.size==2}}
 private fun follow(text:String){ui.onNodeWithTag("agent-followup-input").performScrollTo().performTextReplacement(text);ui.onNodeWithTag("agent-followup-send").performScrollTo().performClick()}
 private fun shot(name:String){ui.waitForIdle();val bitmap=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()!!;File(ui.activity.filesDir,"validation/$name").apply{parentFile?.mkdirs()}.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
 @Test fun followupCanShowItsSourcesAndRepeatActualPhoneReadings(){
  val first=session().agent.value.run!!.startedAt;val source=session().agent.value.results.first().result!!.checkedAt
  follow("muéstrame los detalles");ui.onAllNodesWithText("Ver resultado y método").assertCountEquals(2)
  follow("repítelo");ui.waitUntil(20000){!session().agent.value.running && session().agent.value.run?.startedAt!=first && session().agent.value.results.size==2}
  assertEquals(listOf(AgentTool.CONNECTION,AgentTool.RESOURCES),session().agent.value.plan!!.steps);assertNotEquals(source,session().agent.value.results.first().result!!.checkedAt);assertFalse(session().ai.value.loaded)
  ui.onAllNodesWithText("Ver resultado y método").assertCountEquals(0)
  ui.onNodeWithTag("deck-list").performScrollToIndex(0);shot("ciberdeck-answer-036.png")
  ui.onNodeWithTag("agent-followup-send").performScrollTo();shot("ciberdeck-followup-036.png")
 }
 @Test fun saveIsAcknowledgedOnlyAfterARealVerifiedCopyAndDoesNotDuplicate(){
  val store=EvidenceStore(ui.activity);val before=store.list().map{it.id}.toSet();val runId=session().agent.value.run!!.startedAt;var created:String?=null
  try{
   follow("guárdalo");ui.waitUntil(20000){session().agentSaved.value?.runId==session().agent.value.run?.startedAt && session().agentFollowupStatus.value=="Resultado guardado con SHA-256 verificado."}
   val entry=session().agentSaved.value!!.evidence;created=entry.id;assertFalse(entry.id in before);assertEquals("agent",store.diagnostic(entry).kind)
   val count=store.list().size;follow("guárdalo");ui.waitUntil(5000){session().agentFollowupStatus.value.contains("ya está guardado")};assertEquals(count,store.list().size)
   ui.onNodeWithTag("agent-save-result").performScrollTo().assertIsNotEnabled()
  }finally{val owned=created?:store.list().filter{it.id !in before}.firstOrNull{entry->runCatching{store.diagnostic(entry).let{it.kind=="agent" && it.details.any{field->field.first=="Inicio" && field.second==runId}}}.getOrDefault(false)}?.id;owned?.let{id->val works=AuditWorkStore(ui.activity);works.read().works.filter{w->w.evidence.any{it.id==id}}.forEach{works.detach(it.id,setOf(id))};store.delete(id)}}
 }
}
