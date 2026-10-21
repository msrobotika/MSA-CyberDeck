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
import org.junit.Assume.assumeTrue

class AgentUiDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun actualLocalModelProposesPlanBeforeUserExecutesAndSaves(){
  assumeTrue(androidx.test.platform.app.InstrumentationRegistry.getArguments().getString("agentAi")=="qwen3")
  ui.openDeck("IA")
  val session=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
  ui.waitUntil(20000){session.ai.value.runtime && session.aiModel.value!=null}
  ui.onNode(hasText("Agente",substring=false) and hasClickAction() and !hasTestTag("drawer-agent")).performScrollTo().performClick()
  ui.waitUntil(20000){session.ai.value.runtime && !session.ai.value.busy}
  ui.onNodeWithText("Cargar IA local").performScrollTo().performClick()
  ui.waitUntil(95000){!session.ai.value.busy && (session.ai.value.loaded || session.ai.value.error.isNotBlank())}
  assertTrue(session.ai.value.error,session.ai.value.loaded)
  ui.onNodeWithText("Analista",substring=false).performScrollTo().performClick()
  ui.onNodeWithTag("screen-title").assertTextEquals("Asistente IA")
  ui.runOnIdle{assertTrue("Model must remain loaded when switching modes",session.ai.value.loaded)}
  ui.onNode(hasText("Agente",substring=false) and hasClickAction() and !hasTestTag("drawer-agent")).performScrollTo().performClick()
  ui.onNodeWithTag("screen-title").assertTextEquals("Agente")
  ui.runOnIdle{assertTrue("Switching back must not unload the model",session.ai.value.loaded)}
  ui.onNodeWithText("Qué quieres que haga CiberDeck").performScrollTo().performTextInput("Revisa mi móvil y el estado de su conexión")
  ui.onNodeWithText("Preparar plan con IA").performScrollTo().performClick()
  ui.waitUntil(95000){!session.agent.value.planning && (session.agent.value.plan!=null || session.agent.value.error.isNotBlank())}
  val private=File(ui.activity.filesDir,"validation/agent-plan-private.txt").apply{parentFile?.mkdirs()}
  private.writeText(session.agent.value.raw+"\nERROR: "+session.agent.value.error)
  assertNotNull(session.agent.value.error,session.agent.value.plan)
  assertEquals(listOf(AgentTool.PHONE,AgentTool.CONNECTION),session.agent.value.plan!!.steps)
  assertTrue(session.agent.value.results.isEmpty());assertNull(session.agent.value.run)
  ui.onNodeWithTag("agent-plan").performScrollTo()
  val planShot=File(ui.activity.filesDir,"validation/ciberdeck-agent-plan.png").apply{parentFile?.mkdirs()}
  planShot.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
  ui.onNodeWithText("Ejecutar este plan").performScrollTo().performClick()
  ui.waitUntil(20000){!session.agent.value.running && session.agent.value.run!=null}
  assertEquals(listOf("phone","connection"),session.agent.value.results.map{it.result?.kind})
  ui.onNodeWithText("Guardar ejecución como evidencia").performScrollTo()
  val out=File(ui.activity.filesDir,"validation/ciberdeck-agent.png").apply{parentFile?.mkdirs()}
  out.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
  val store=EvidenceStore(ui.activity);val before=store.list().size
  val works=AuditWorkStore(ui.activity);val active=works.read().activeId
  ui.onNodeWithText("Guardar ejecución como evidencia").performClick()
  ui.waitUntil(10000){store.list().size>before}
  val entry=store.list().last();val report=store.diagnostic(entry)
  assertEquals("agent",report.kind)
  assertTrue(report.details.any{it.first=="Plan aprobado" && it.second=="phone, connection"})
  assertTrue(report.details.any{it.first=="SHA-256 modelo planificador" && it.second==session.agent.value.modelHash})
  if(active!=null)ui.waitUntil(10000){works.read().works.first{it.id==active}.evidence.any{AuditWorkPolicy.matches(it,entry)}}
  ui.openDeck("Resumen")
 }
}
