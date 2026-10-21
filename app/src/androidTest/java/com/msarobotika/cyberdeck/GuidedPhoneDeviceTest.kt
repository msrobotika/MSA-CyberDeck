package com.msarobotika.cyberdeck
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class GuidedPhoneDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun shot(name:String){File(ui.activity.filesDir,"validation/$name").apply{parentFile?.mkdirs()}.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}}
 @Test fun realObservationLeadsToNextStepAndFreshReadingWithoutLeavingTheTool(){
  ui.activity.runOnUiThread{ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
  val store=EvidenceStore(ui.activity);val beforeEntries=store.list();val beforeWorks=AuditWorkStore(ui.activity).read()
  ui.openDeck("Resumen");ui.onNodeWithTag("home-phone").performScrollTo().performClick()
  val session=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
  ui.waitUntil(15000){session.phone.value!=null}
  val first=session.phone.value!!
  assertEquals("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",first.device)
  ui.onNodeWithTag("phone-guided").assertExists()
  ui.onAllNodesWithText("Plan de revisión").assertCountEquals(0)
  ui.onNodeWithTag("deck-list").performScrollToIndex(0);shot("ciberdeck-guided-summary-032.png")
  val check=PhoneBriefPolicy.priorities(first.checks).firstOrNull()?:first.checks.first()
  ui.onNodeWithTag("guided-check-${PhoneBriefPolicy.key(check)}").performScrollTo().performClick()
  ui.activityRule.scenario.recreate()
  ui.onNodeWithTag("deck-list").performScrollToNode(hasTestTag("guided-next-step"))
  ui.onNodeWithTag("guided-next-step").assertIsDisplayed()
  ui.onNodeWithText(check.title).assertExists()
  ui.onNodeWithTag("guided-recheck").performScrollTo().performClick()
  ui.waitUntil(15000){session.phone.value!!.at!=first.at}
  val current=session.phone.value!!
  val value=current.checks.first{PhoneBriefPolicy.key(it)==PhoneBriefPolicy.key(check)}.value
  ui.onNodeWithTag("guided-recheck-result").performScrollTo().assertTextEquals(if(value==check.value)"El valor observado no ha cambiado." else "Antes: ${check.value}\nAhora: $value")
  ui.onNodeWithTag("guided-next-step").performScrollTo();shot("ciberdeck-guided-step-032.png")
  ui.onNodeWithTag("guided-back").performScrollTo().performClick()
  ui.onAllNodesWithTag("guided-next-step").assertCountEquals(0)
  ui.onNodeWithTag("phone-advanced").performScrollTo().performClick()
  ui.onNodeWithText("Recursos").performScrollTo().performClick()
  ui.onNodeWithText("Iniciar gráfica en vivo").performScrollTo().assertIsDisplayed()
  ui.onNodeWithTag("phone-guided-return").performScrollTo().performClick()
  assertFalse(session.phoneLive.value)
  assertEquals(beforeEntries,store.list());assertEquals(beforeWorks,AuditWorkStore(ui.activity).read())
 }
}
