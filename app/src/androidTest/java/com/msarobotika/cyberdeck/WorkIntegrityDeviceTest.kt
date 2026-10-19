package com.msarobotika.cyberdeck

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

class WorkIntegrityDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun actualSavedCopiesProduceCountsAndToolShortcut(){
  val works=AuditWorkStore(ui.activity)
  val work=works.read().works.firstOrNull{it.id==works.read().activeId}
  assumeTrue(work!=null && work.evidence.isNotEmpty())
  ui.openDeck("Trabajos")
  ui.onNodeWithText("Comprobar copias del trabajo").performScrollTo().performClick()
  ui.waitUntil(15000){ui.onAllNodes(hasText(" · Verificadas",substring=true)).fetchSemanticsNodes().isNotEmpty()}
  val checked=verifyWorkCopies(work!!,EvidenceStore(ui.activity))
  ui.onNodeWithText("${checked.count(WorkCopyState.VERIFIED)} · Verificadas").performScrollTo().assertIsDisplayed()
  ui.onNodeWithText("${checked.count(WorkCopyState.UNVERIFIED)} · No verificables").assertExists()
  ui.onNodeWithText("Integridad de copias; no análisis de malware.").performScrollTo()
  val out=File(ui.activity.filesDir,"validation/ciberdeck-work-integrity.png").apply{parentFile?.mkdirs()}
  out.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
  ui.onNodeWithText("Revisar este móvil").performScrollTo().performClick()
  ui.onNode(hasText("Analizar ahora") or hasText("Actualizar")).performScrollTo().assertIsDisplayed()
  assertEquals(work.id,works.read().activeId)
 }
}
