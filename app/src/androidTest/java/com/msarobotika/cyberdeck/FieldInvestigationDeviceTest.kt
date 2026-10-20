package com.msarobotika.cyberdeck
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
class FieldInvestigationDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun missionReadsRealSavedCopiesWithoutMutatingThem(){
  ui.activity.runOnUiThread{ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
  val store=EvidenceStore(ui.activity);val before=store.list()
  val works=AuditWorkStore(ui.activity).read();val work=works.works.firstOrNull{it.id==works.activeId}
  val candidates=before.asReversed().filter{e->e.name.startsWith("diagnostico-") && e.bytes<=65536 && (work==null || work.evidence.any{AuditWorkPolicy.matches(it,e)})}
  val expected=investigateSavedReadings(candidates){store.diagnostic(it)}
  ui.openDeck("Resumen")
  ui.onNodeWithTag("home-investigation").performScrollTo().performClick()
  ui.onNodeWithTag("investigation-panel").assertExists()
  if(candidates.isEmpty()){ui.onNodeWithTag("investigation-run").assertIsNotEnabled()}
  else{
   ui.onNodeWithTag("investigation-run").performScrollTo().performClick()
   ui.waitUntil(20000){ui.onAllNodesWithTag("investigation-result").fetchSemanticsNodes().isNotEmpty()}
   ui.onNodeWithText("${expected.changed} campos diferentes").assertExists()
   ui.onNodeWithTag("investigation-result").performScrollTo()
   File(ui.activity.filesDir,"validation/ciberdeck-investigation-030.png").apply{parentFile?.mkdirs()}.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
  }
  assertEquals(before,store.list());assertEquals(works,AuditWorkStore(ui.activity).read())
 }
}
