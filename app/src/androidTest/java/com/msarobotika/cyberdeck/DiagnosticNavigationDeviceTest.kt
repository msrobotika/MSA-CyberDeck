package com.msarobotika.cyberdeck

import android.view.WindowManager
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class DiagnosticNavigationDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun session()=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
 @Test fun actualLoopbackDnsShowsResultAndSurvivesRecreation(){
  ui.activity.runOnUiThread{ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
  ui.openDeck("Red")
  ui.onNodeWithText("Dominio o IP").performScrollTo().performTextInput("127.0.0.1")
  ui.onNodeWithText("Resolver DNS / IP").performScrollTo().performClick()
  ui.waitUntil(15000){session().diagnostic.value!=null}
  val original=session().diagnostic.value!!
  assertEquals("dns",original.kind);assertEquals("127.0.0.1",original.target)
  ui.onNodeWithTag("diagnostic-result").assertIsDisplayed()
  File(ui.activity.filesDir,"validation/ciberdeck-dns-027.png").apply{parentFile?.mkdirs()}.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
  ui.activityRule.scenario.recreate()
  ui.runOnIdle{assertEquals(original,session().diagnostic.value)}
  ui.onNodeWithTag("diagnostic-result").assertIsDisplayed()
  ui.onNodeWithTag("diagnostic-details-toggle").performScrollTo().performClick()
  ui.onNodeWithText("A / IPv4",substring=false).performScrollTo().assertIsDisplayed()
 }
}
