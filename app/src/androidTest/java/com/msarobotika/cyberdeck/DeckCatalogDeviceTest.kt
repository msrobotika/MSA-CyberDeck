package com.msarobotika.cyberdeck

import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.io.File
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DeckCatalogDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Before fun keepVisible(){ui.activity.runOnUiThread{ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}}
 private fun shot(name:String){File(ui.activity.filesDir,"validation/$name").apply{parentFile?.mkdirs()}.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}}
 @Test fun homeOpensSelectedAreaAndBackKeepsIt(){
  ui.openDeck("Resumen");ui.onNodeWithTag("deck-list").performScrollToIndex(0);shot("ciberdeck-home-025.png")
  ui.onNodeWithTag("deck-list").performScrollToNode(hasTestTag("home-area-proximity"))
  ui.onNodeWithTag("home-area-proximity").performClick()
  ui.onNodeWithTag("tool-NFC").performScrollTo().assertIsDisplayed()
  ui.onAllNodesWithTag("tool-USB").assertCountEquals(0)
  ui.onNodeWithTag("tool-NFC").performClick();ui.onNodeWithTag("screen-title").assertTextEquals("NFC")
  ui.onNodeWithContentDescription("Volver a herramientas").performClick()
  ui.onNodeWithTag("tool-NFC").performScrollTo().assertIsDisplayed()
  ui.onNodeWithTag("deck-list").performScrollToIndex(0)
  ui.onNodeWithTag("tool-areas").performClick();shot("ciberdeck-kit-025.png")
 }
 @Test fun searchIsGlobalAndCanRecoverFromNoResults(){
  ui.openDeck("Herramientas");ui.onNodeWithTag("deck-list").performScrollToIndex(0)
  ui.onNodeWithTag("tool-areas").performClick()
  ui.onNodeWithTag("tool-area-hardware").performScrollTo().performClick()
  ui.onNodeWithText("Buscar una herramienta").performTextInput("transferir")
  ui.onNodeWithText("Buscar una herramienta").performImeAction();ui.waitForIdle()
  ui.onNodeWithTag("tool-Recibir").performScrollTo().assertIsDisplayed()
  ui.onNodeWithTag("deck-list").performScrollToIndex(0);shot("ciberdeck-search-025.png")
  ui.onNodeWithText("Buscar una herramienta").performTextReplacement("not-a-real-tool-937")
  ui.onNodeWithText("Buscar una herramienta").performImeAction()
  ui.onNodeWithText("No hemos encontrado esa herramienta").performScrollTo().assertIsDisplayed()
  ui.onNodeWithTag("deck-list").performScrollToIndex(0)
  ui.onNodeWithTag("search-clear").performClick()
  ui.onNodeWithTag("tool-USB").performScrollTo().assertIsDisplayed()
 }
 @Test fun primaryHomeActionReadsPhoneAndAgentRemainsReachable(){
  ui.openDeck("Resumen");ui.onNodeWithTag("deck-list").performScrollToIndex(0)
  ui.onNodeWithTag("home-phone").performScrollTo().performClick()
  ui.waitUntil(20000){ui.onAllNodesWithText("Mi móvil").fetchSemanticsNodes().isNotEmpty()}
  ui.openDeck("Resumen");ui.onNodeWithTag("deck-list").performScrollToIndex(0)
  ui.onNodeWithTag("home-agent").performScrollTo().performClick()
  ui.onNodeWithTag("screen-title").assertTextEquals("Agente")
 }
}
