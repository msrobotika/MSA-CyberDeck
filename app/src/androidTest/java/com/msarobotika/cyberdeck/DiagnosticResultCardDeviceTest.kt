package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Explicit UI fixtures only. No clipboard, evidence store or network writes. */
class DiagnosticResultCardDeviceTest {
 @get:Rule val ui=createComposeRule()
 private val fixture=DiagnosticResult("ui-fixture","owned.example","2026-10-20T10:00:00Z","UI fixture; not a device measurement",listOf("Header" to "Content-Type","Header" to "Content-Length","Method" to "HEAD"))
 @Test fun filteringNeverChangesWhatIsSavedAndCopyUsesOnlySummary(){
  var saved:DiagnosticResult?=null;var copied=""
  ui.setContent{DeckTheme{Column(Modifier.verticalScroll(rememberScrollState())){DiagnosticResultCard(fixture,false,save={saved=it},copySummary={copied=it})}}}
  ui.onAllNodesWithText("Content-Type").assertCountEquals(0)
  ui.onNodeWithTag("diagnostic-details-toggle").performScrollTo().performClick()
  ui.onNodeWithTag("diagnostic-details-search").performScrollTo().performTextInput("length")
  ui.onNodeWithTag("diagnostic-details-search").performImeAction()
  ui.onNodeWithText("Content-Length").performScrollTo().assertIsDisplayed()
  ui.onAllNodesWithText("Content-Type").assertCountEquals(0)
  ui.onNodeWithTag("diagnostic-save").performScrollTo().performClick()
  ui.runOnIdle{assertSame(fixture,saved);assertEquals(3,saved!!.details.size)}
  ui.onNodeWithTag("diagnostic-copy").performScrollTo().performClick()
  ui.runOnIdle{assertEquals(DiagnosticPresentation.summary(fixture),copied)}
 }
 @Test fun newObservationDoesNotInheritFilterOrCopiedMessage(){
  val current=mutableStateOf(fixture)
  ui.setContent{DeckTheme{Column(Modifier.verticalScroll(rememberScrollState())){DiagnosticResultCard(current.value,false,save={},copySummary={})}}}
  ui.onNodeWithTag("diagnostic-copy").performScrollTo().performClick()
  ui.onNodeWithTag("diagnostic-details-toggle").performScrollTo().performClick()
  ui.onNodeWithTag("diagnostic-details-search").performScrollTo().performTextInput("length")
  ui.onNodeWithTag("diagnostic-details-search").performImeAction()
  ui.runOnIdle{current.value=fixture.copy(checkedAt="2026-10-20T10:01:00Z",summary="Second explicit UI fixture")}
  ui.onAllNodesWithTag("diagnostic-details-search").assertCountEquals(0)
  ui.onAllNodesWithText("Resumen copiado · sin campos técnicos").assertCountEquals(0)
  ui.onNodeWithText("Second explicit UI fixture").performScrollTo().assertIsDisplayed()
 }
}
