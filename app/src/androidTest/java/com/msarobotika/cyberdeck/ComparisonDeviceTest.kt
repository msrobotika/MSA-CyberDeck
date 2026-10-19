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
class ComparisonDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun realSavedPhoneReadingsComparedAndSavedInActiveWork(){
  val store=EvidenceStore(ui.activity);val works=AuditWorkStore(ui.activity)
  val work=works.read().works.firstOrNull{it.id==works.read().activeId}
  val entries=store.list().filter{e->work?.evidence?.any{AuditWorkPolicy.matches(it,e)}==true}.filter{runCatching{store.diagnostic(it).kind=="phone"}.getOrDefault(false)}
  assumeTrue(entries.size>=2)
  val a=entries.first();val b=entries.last();val expected=loadSavedComparison(store,a,b)
  ui.openDeck("Comparar")
  ui.onNodeWithTag("comparison-before-choice").performScrollTo().performClick()
  ui.onNodeWithTag("comparison-before-${a.id}").performScrollTo().performClick()
  ui.onNodeWithTag("comparison-after-choice").performScrollTo().performClick()
  ui.onNodeWithTag("comparison-after-${b.id}").performScrollTo().performClick()
  ui.activityRule.scenario.recreate();ui.openDeck("Comparar")
  ui.waitUntil(10000){runCatching{ui.onNodeWithText("Verificar y comparar").assertIsEnabled()}.isSuccess}
  ui.onNodeWithText("Verificar y comparar").performScrollTo().performClick()
  ui.waitUntil(15000){ui.onAllNodesWithText("COPIAS VERIFICADAS").fetchSemanticsNodes().isNotEmpty()}
  for(change in FieldChange.entries)ui.onNodeWithText("${expected.count(change)} · ${change.label}").assertExists()
  ui.onNodeWithText("Campos observados; no es una puntuación de seguridad.").performScrollTo()
  val out=File(ui.activity.filesDir,"validation/ciberdeck-comparison.png").apply{parentFile?.mkdirs()}
  out.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
  val before=store.list().size
  ui.onNodeWithText("Guardar comparación").performScrollTo().performClick()
  ui.waitUntil(15000){store.list().size>before}
  val saved=store.list().last();assertEquals("comparison",store.diagnostic(saved).kind)
  assertTrue(works.read().works.first{it.id==work!!.id}.evidence.any{it.id==saved.id})
 }
}
