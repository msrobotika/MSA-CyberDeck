package com.msarobotika.cyberdeck

import android.content.Context
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DeckRecentToolsDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private var previous:String?=null
 @Before fun setup(){
  previous=ui.activity.getSharedPreferences("deck-navigation",Context.MODE_PRIVATE).getString("recent-tools",null)
  ui.activity.runOnUiThread{ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
 }
 @After fun restore(){val editor=ui.activity.getSharedPreferences("deck-navigation",Context.MODE_PRIVATE).edit();previous?.let{editor.putString("recent-tools",it)}?:editor.remove("recent-tools");editor.commit()}
 @Test fun visitedToolCanBeReopenedAfterRecreation(){
  ui.openDeck("NFC")
  ui.onNodeWithTag("tool-guide").performScrollTo().performClick()
  ui.onNodeWithText("Necesitas NFC activo y una etiqueta compatible propia.").performScrollTo().assertIsDisplayed()
  ui.openDeck("Resumen")
  ui.onNodeWithTag("home-more").performScrollTo().performClick()
  ui.onNodeWithTag("deck-list").performScrollToNode(hasTestTag("recent-NFC"))
  ui.onNodeWithTag("recent-NFC").assertIsDisplayed()
  ui.activityRule.scenario.recreate()
  ui.onNodeWithTag("deck-list").performScrollToNode(hasTestTag("recent-NFC"))
  ui.onNodeWithTag("recent-NFC").performClick()
  ui.onNodeWithTag("screen-title").assertTextEquals("NFC")
 }
}
