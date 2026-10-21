package com.msarobotika.cyberdeck
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/** Produces a real local review of the owner's A15; retains its work/evidence for the owner. */
class AuditWorkFlowDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun actualA15ReviewIsAssociatedAndSurvivesRecreation(){
  val store=AuditWorkStore(ui.activity);var work=store.read().works.firstOrNull{it.name=="Revisión local del A15" && !it.archived}
  ui.openDeck("Trabajos")
  if(work==null){
   if(store.read().works.isNotEmpty())ui.onNodeWithText("Nuevo trabajo").performScrollTo().performClick()
   ui.onNodeWithText("Nombre del trabajo nuevo").performScrollTo().performTextInput("Revisión local del A15")
   ui.onNodeWithText("Alcance del trabajo nuevo").performScrollTo().performTextInput("Diagnóstico local del Samsung SM-A155F del propietario. Sin pruebas sobre terceros.")
   ui.onNodeWithText("Objetivos declarados").performScrollTo().performTextInput("Samsung SM-A155F · ajustes y recursos locales")
   ui.onNodeWithText("Crear y activar").performScrollTo().performClick()
   ui.waitUntil(10000){val s=store.read();s.works.any{it.name=="Revisión local del A15" && it.id==s.activeId}}
   work=store.read().works.first{it.id==store.read().activeId}
  }else{store.activate(work.id);ui.activityRule.scenario.recreate();ui.openDeck("Trabajos")}
  val id=work!!.id;val before=store.read().works.first{it.id==id}.evidence.size
  ui.openDeck("Móvil")
  ui.onNode(hasText("Analizar ahora") or hasText("Actualizar")).performScrollTo().performClick()
  ui.waitUntil(10000){ui.onAllNodesWithText("Guardar chequeo").fetchSemanticsNodes().isNotEmpty()}
  ui.onNodeWithText("Guardar chequeo").performScrollTo().performClick()
  ui.waitUntil(15000){store.read().works.first{it.id==id}.evidence.size>before}
  ui.openDeck("Trabajos")
  ui.onNodeWithText("TRABAJO ACTIVO").performScrollTo().assertIsDisplayed()
  val report=store.report(id,EvidenceStore(ui.activity));assertTrue(report.contains("SHA-256 verificado"));assertTrue(report.contains("samsung SM-A155F",true))
  val out=File(ui.activity.filesDir,"validation").apply{mkdirs()}
  File(out,"a15-work-review.md").writeText(report)
  File(out,"ciberdeck-work.png").outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
  ui.activityRule.scenario.recreate();assertEquals(id,store.read().activeId)
 }
}
