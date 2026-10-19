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

class AgentWorkflowDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun select(vararg tools:AgentTool){
  ui.openDeck("IA");val session=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
  ui.waitUntil(20000){session.ai.value.runtime && !session.ai.value.busy}
  ui.onNodeWithText("Agente",substring=false).performScrollTo().performClick()
  ui.onNodeWithText("Elegir funciones · 30").performScrollTo().performClick()
  tools.forEach{ui.onNodeWithTag("agent-pick-${it.wire}").performScrollTo().performClick()}
  ui.onNodeWithText("Preparar selección · ${tools.size}/8").performScrollTo().performClick()
 }
 @Test fun savedApkResultReturnsToAgentAndContinuesWithoutInventedCompletion(){
  select(AgentTool.APK,AgentTool.INVENTORY)
  val session=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
  ui.onNodeWithText("Ejecutar este plan").performScrollTo().performClick()
  ui.waitUntil(10000){session.agent.value.run?.steps?.lastOrNull()?.awaiting==true && !session.agent.value.running}
  assertNull(session.agent.value.results.single().result)
  ui.onNodeWithTag("agent-open-apk").performScrollTo().performClick()
  ui.onNodeWithText("Inspeccionar CiberDeck instalada").performScrollTo().performClick()
  ui.waitUntil(20000){ui.onAllNodesWithText("Guardar informe APK").fetchSemanticsNodes().isNotEmpty()}
  ui.onNodeWithText("Guardar informe APK").performScrollTo().performClick()
  ui.waitUntil(10000){session.agent.value.resumeEvidence!=null}
  ui.onNodeWithText("Volver al agente · paso pendiente").performScrollTo().performClick()
  ui.waitUntil(10000){session.ai.value.connected && !session.ai.value.busy}
  ui.onNodeWithText("Usar resultado guardado y continuar").performScrollTo().performClick()
  ui.waitUntil(20000){!session.agent.value.running && session.agent.value.results.size==2 && session.agent.value.results.last().result!=null}
  assertEquals(listOf("apk","inventory"),session.agent.value.results.map{it.result?.kind})
  val source=session.agent.value.results.first().source!!;assertTrue(EvidenceStore(ui.activity).verifiedFile(source).isFile)
  ui.onNodeWithText("Guardar ejecución como evidencia").performScrollTo()
  val out=File(ui.activity.filesDir,"validation/ciberdeck-agent-workflow.png").apply{parentFile?.mkdirs()}
  out.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
  val store=EvidenceStore(ui.activity);val before=store.list().size
  ui.onNodeWithText("Guardar ejecución como evidencia").performClick();ui.waitUntil(10000){store.list().size>before}
  val report=store.diagnostic(store.list().last());assertTrue(report.details.any{it.first.endsWith("/ fuente") && it.second.contains(source.sha256)})
 }
 @Test fun offlineLinkAndCidrUseExplicitInputsWithoutLoadingModel(){
  select(AgentTool.LINK,AgentTool.CIDR)
  val session=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
  assertFalse(session.ai.value.loaded)
  ui.onNodeWithText("URL elegida").performScrollTo().performTextInput("https://example.com/")
  ui.onNodeWithText("IPv4 y prefijo CIDR").performScrollTo().performTextInput("192.0.2.10/24")
  ui.onNodeWithText("Ejecutar este plan").performScrollTo().performClick()
  ui.waitUntil(10000){!session.agent.value.running && session.agent.value.results.size==2}
  assertEquals(listOf("link","cidr"),session.agent.value.results.map{it.result?.kind})
  assertTrue(session.agent.value.results.last().result!!.details.any{it.first=="Red" && it.second=="192.0.2.0"})
 }
}
