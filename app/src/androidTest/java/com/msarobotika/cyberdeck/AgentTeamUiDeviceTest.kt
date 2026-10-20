package com.msarobotika.cyberdeck

import android.graphics.Bitmap
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.view.WindowManager
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

class AgentTeamUiDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private val instrument get()=InstrumentationRegistry.getInstrumentation()
 private fun session()=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
 @Before fun keepVisible(){ui.activity.runOnUiThread{ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}}
 private fun open(){ui.openDeck("Agente");ui.waitUntil(20000){session().ai.value.connected && !session().ai.value.busy}}
 private fun shot(name:String){File(ui.activity.filesDir,"validation/$name").apply{parentFile?.mkdirs()}.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}}
 private fun nativeClick(text:String){try{ui.waitUntil(15000){val root=instrument.uiAutomation.rootInActiveWindow;var node=root?.findAccessibilityNodeInfosByText(text)?.firstOrNull{it.text?.toString()?.equals(text,ignoreCase=true)==true};while(node!=null && !node.isClickable)node=node.parent;node?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true}}catch(e:Exception){
  val lines=mutableListOf<String>();fun visit(node:AccessibilityNodeInfo?){if(node==null)return;lines+="${node.className}: ${node.text} · ${node.viewIdResourceName}";for(i in 0 until node.childCount)visit(node.getChild(i))};visit(instrument.uiAutomation.rootInActiveWindow)
  File(ui.activity.filesDir,"validation/team-023-ui-failure-private.txt").writeText("Waiting for $text\n"+lines.joinToString("\n"));throw e
 }}
 @Test fun actualPhoneTaskShowsRequiredSpecialistsAndDeterministicBalance(){
  open();ui.onNodeWithTag("agent-task-phone").performScrollTo().performClick()
  ui.waitUntil(25000){!session().agent.value.running && session().agent.value.results.size==5}
  assertTrue(session().agent.value.results.all{it.result!=null && it.error.isBlank()})
  ui.onNodeWithTag("agent-worker-mobile").assertTextEquals("Resultados recibidos · 4/4")
  ui.onNodeWithTag("agent-worker-network").assertTextEquals("Resultados recibidos · 1/1")
  ui.onAllNodesWithTag("agent-worker-hardware").assertCountEquals(0)
  ui.onNodeWithTag("agent-team").performScrollTo();shot("ciberdeck-team-023.png")
  ui.onNodeWithText("Balance del coordinador · sin IA").performScrollTo().performClick()
  ui.onNodeWithText(AgentTeam.review(session().agent.value.run!!)).performScrollTo().assertIsDisplayed()
  shot("ciberdeck-team-balance-023.png")
 }
 @Test fun sharedSpecialistContextPassesThroughActualAnalystChooserAndLocalEngine(){
  assumeTrue(InstrumentationRegistry.getArguments().getString("agentAi")=="qwen3")
  open()
  ui.runOnIdle{session().agent.value=AgentSession(request="Revisa mi móvil y conexión; interpreta los resultados",plan=AgentPlan(listOf(AgentTool.PHONE,AgentTool.CONNECTION,AgentTool.ANALYST)),origin="Prueba de integración autorizada")}
  ui.onNodeWithText("Ejecutar este plan").performScrollTo().performClick()
  nativeClick("Resumen de especialistas")
  ui.waitUntil(15000){val root=instrument.uiAutomation.rootInActiveWindow;val node=root?.findAccessibilityNodeInfosByViewId("android:id/custom")?.firstOrNull();val edit=root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);if(edit?.className=="android.widget.EditText")edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"Resume lo observado y di qué falta comprobar. No inventes datos.")}) else {
    fun input(n:AccessibilityNodeInfo?):AccessibilityNodeInfo? {if(n==null)return null;if(n.className=="android.widget.EditText")return n;for(i in 0 until n.childCount){input(n.getChild(i))?.let{return it}};return null}
    input(node?:root)?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"Resume lo observado y di qué falta comprobar. No inventes datos.")})==true
   }}
  nativeClick("Continuar")
  ui.waitUntil(120000){!session().agent.value.running}
  val state=session().agent.value
  File(ui.activity.filesDir,"validation/team-023-analyst-private.json").apply{parentFile?.mkdirs();writeText(state.run!!.diagnostic(state.request,state.modelHash,state.evidenceScope,state.origin).toEvidenceJson())}
  assertEquals(state.error,3,state.results.size)
  val result=state.results.last().result
  assertNotNull(state.results.last().error,result);assertEquals("ai",result!!.kind)
  assertEquals("Especialistas de la misión actual",result.target)
  val prompt=result.details.first{it.first=="Entrada enviada al motor"}.second
  assertTrue(prompt.contains("team-context"));assertTrue(prompt.contains("Móvil/phone"));assertTrue(prompt.contains("Red/connection"))
  assertTrue(result.details.first{it.first=="Respuesta generada"}.second.isNotBlank())
  ui.onNodeWithTag("agent-worker-analyst").assertTextEquals("Interpretación generada · revisar")
  ui.onNodeWithTag("agent-team").performScrollTo();shot("ciberdeck-team-analyst-023.png")
 }
}
