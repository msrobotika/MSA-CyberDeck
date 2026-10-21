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
  ui.onNode(hasText("Agente",substring=false) and hasClickAction() and !hasTestTag("drawer-agent")).performScrollTo().performClick()
  ui.onNodeWithText("Elegir funciones · 30").performScrollTo().performClick()
  tools.forEach{ui.onNodeWithTag("agent-pick-${it.wire}").performScrollTo().performClick()}
  ui.onNodeWithText("Preparar selección · ${tools.size}/8").performScrollTo().performClick()
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
