package com.msarobotika.cyberdeck

import android.view.KeyEvent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.io.File

class AgentActionsDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Before fun keepAwakeDuringTest(){ui.activity.runOnUiThread{ui.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}}
 private fun nativeClick(text:String){ui.waitUntil(12000){val root=InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow;val found=root?.findAccessibilityNodeInfosByText(text)?.firstOrNull{it.text?.toString()==text};if(found==null)false else{var node=found;while(node!=null && !node.isClickable)node=node.parent;node?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)==true}}}
 private fun session()=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
 private fun prepare(vararg tools:AgentTool){
  ui.openDeck("IA");ui.waitUntil(20000){session().ai.value.connected && !session().ai.value.busy}
  ui.runOnIdle{session().agentMode.value=true;session().agent.value=AgentSession(request="Prueba autorizada del agente",plan=AgentPlan(tools.toList()),origin="Prueba de dispositivo")}
  ui.onNodeWithText("Ejecutar este plan").performScrollTo().performClick()
 }
 @Test fun allRegisteredActionsDispatchAndFailuresNeverCompleteLaterSteps()=runBlocking {
  // Explicit fixture adapter validates orchestration only, not physical hardware.
  val seen=mutableListOf<AgentTool>()
  AgentTool.entries.chunked(8).forEach{chunk->val run=executeAgentActions(AgentPlan(chunk),{tool->seen+=tool;DiagnosticResult("fixture",tool.wire,"2026-10-19T00:00:00Z","Orchestration fixture, not a device reading",emptyList())});assertTrue(run.steps.all{it.result!=null && !it.awaiting})}
  assertEquals(AgentTool.entries.toList(),seen)
  val failed=executeAgentActions(AgentPlan(listOf(AgentTool.USB,AgentTool.INVENTORY)),{error("No device fixture")});assertEquals(1,failed.steps.size);assertNull(failed.steps.single().result)
  try{executeAgentActions(AgentPlan(listOf(AgentTool.NFC,AgentTool.INVENTORY)),{throw CancellationException("Cancelled fixture")});fail("Must cancel")}catch(_:CancellationException){}
 }
 @Test fun actualSensorAndCatalogContinueToOwnApkAndInventory(){
  assumeTrue(ui.activity.getSystemService(android.hardware.SensorManager::class.java).getDefaultSensor(android.hardware.Sensor.TYPE_MAGNETIC_FIELD)!=null)
  prepare(AgentTool.MAGNETIC,AgentTool.MODULES,AgentTool.SELF_APK,AgentTool.INVENTORY)
  ui.waitUntil(30000){!session().agent.value.running && session().agent.value.results.size==4}
  val rows=session().agent.value.results;assertEquals(listOf("magnetic","modules","apk","inventory"),rows.map{it.result?.kind});assertTrue(rows.none{it.awaiting || it.manual})
  assertEquals(6,session().magnetic.value.size)
  ui.onNodeWithText("Guardar ejecución como evidencia").performScrollTo()
  File(ui.activity.filesDir,"validation/ciberdeck-agent-actions.png").apply{parentFile?.mkdirs()}.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
 }
 @Test fun verifiesSelectedPrivateFixtureAndContinuesWithoutChangingSource(){
  val store=EvidenceStore(ui.activity);val name="agent-actions-fixture-${java.util.UUID.randomUUID()}.txt";val entry=store.saveBytes(name,"Owned test fixture".toByteArray()).last()
  try{prepare(AgentTool.FILES,AgentTool.INVENTORY)
   nativeClick("Verificar una copia")
   nativeClick("$name · ${entry.bytes} B")
   ui.waitUntil(15000){!session().agent.value.running && session().agent.value.results.size==2}
   assertEquals(listOf("files","inventory"),session().agent.value.results.map{it.result?.kind})
   assertTrue(store.verifiedFile(entry).isFile);assertTrue(session().agent.value.results.first().result!!.details.any{it.second==entry.sha256})
  }finally{store.delete(entry.id)}
 }
 @Test fun cancellingApkPickerDoesNotInventAResultOrRunNextStep(){
  prepare(AgentTool.APK,AgentTool.INVENTORY)
  ui.waitUntil(15000){session().agent.value.running && !ui.activity.hasWindowFocus()}
  assertTrue(session().agent.value.results.isEmpty())
  InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
  ui.waitUntil(15000){!session().agent.value.running}
  assertTrue(session().agent.value.results.isEmpty());assertTrue(session().agent.value.notice.contains("Detenido"))
 }
 @Test fun nfcWaitsForRealTagAndStopDoesNotCompleteInventory(){
  val adapter=android.nfc.NfcAdapter.getDefaultAdapter(ui.activity);assumeTrue(adapter?.isEnabled==true)
  prepare(AgentTool.NFC,AgentTool.INVENTORY)
  ui.waitUntil(10000){ui.onAllNodesWithText("Acerca una etiqueta propia",substring=true).fetchSemanticsNodes().isNotEmpty()}
  assertTrue(session().agent.value.results.isEmpty())
  ui.onNodeWithText("Detener ejecución").performScrollTo().performClick()
  ui.waitUntil(10000){!session().agent.value.running};assertTrue(session().agent.value.results.isEmpty())
 }
 @Test fun bleUsesActualControllerAndThenContinues(){
  assumeTrue(BleInspector.hasPermissions(ui.activity))
  val manager=ui.activity.getSystemService(android.bluetooth.BluetoothManager::class.java)
  @Suppress("MissingPermission") val enabled=manager?.adapter?.isEnabled==true
  assumeTrue(enabled)
  prepare(AgentTool.BLE,AgentTool.MODULES)
  ui.waitUntil(20000){!session().agent.value.running && session().agent.value.results.size==2}
  assertEquals(listOf("ble","modules"),session().agent.value.results.map{it.result?.kind});assertTrue(session().agent.value.results.none{it.awaiting})
 }
}
