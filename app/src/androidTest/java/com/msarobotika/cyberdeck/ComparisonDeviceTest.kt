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
 @Test fun compatiblePairCanBePreparedFromRealWorkReadings(){
  val store=EvidenceStore(ui.activity);val state=AuditWorkStore(ui.activity).read()
  val work=state.works.firstOrNull{it.id==state.activeId}
  assumeTrue(work!=null)
  val candidates=store.list().asReversed().filter{e->e.name.startsWith("diagnostico-") && !e.name.startsWith("diagnostico-comparison-") && e.bytes<=65536 && work!!.evidence.any{AuditWorkPolicy.matches(it,e)}}
  val expected=suggestComparison(candidates){store.diagnostic(it)}
  assumeTrue(expected!=null)
  ui.openDeck("Comparar")
  ui.onNodeWithTag("comparison-suggest").performScrollTo().performClick()
  ui.waitUntil(15000){ui.onAllNodesWithTag("comparison-pair-preview").fetchSemanticsNodes().isNotEmpty()}
  val selection=androidx.lifecycle.ViewModelProvider(ui.activity)[ComparisonSelectionModel::class.java]
  assertEquals(expected!!.baseline.entry.id,selection.first.value);assertEquals(expected.current.entry.id,selection.second.value)
  ui.onNodeWithTag("comparison-pair-preview").performScrollTo()
  val out=File(ui.activity.filesDir,"validation/ciberdeck-comparison-guided.png").apply{parentFile?.mkdirs()}
  out.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
  ui.onNodeWithText("Verificar y comparar").performScrollTo().performClick()
  ui.waitUntil(15000){ui.onAllNodesWithText("COPIAS VERIFICADAS").fetchSemanticsNodes().isNotEmpty()}
 }
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
  try{ui.waitUntil(10000){runCatching{ui.onNodeWithText("Verificar y comparar").assertIsEnabled()}.isSuccess}}
  catch(e:Throwable){File(ui.activity.filesDir,"validation/comparison-failure-private.txt").apply{parentFile?.mkdirs()}.writeText(ui.onRoot().printToString());throw e}
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
