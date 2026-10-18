package com.msarobotika.cyberdeck

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import android.content.ComponentName
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class GuardToolsDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 private fun shot(name:String){val f=File(ui.activity.filesDir,"validation/ciberdeck-$name.png").apply{parentFile?.mkdirs()};f.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}}
 @Test fun offlineLinkShowsActualDestinationAndOmitsSecrets(){
  ui.openDeck("Enlaces")
  ui.onNodeWithText("Enlace HTTP o HTTPS").performTextInput("https://trusted.example@review.example/login?token=private-fixture")
  ui.onNodeWithText("Analizar sin abrir").performScrollTo().performClick()
  ui.onNodeWithText("1 señales para revisar").performScrollTo().assertIsDisplayed()
  ui.onNodeWithText("review.example").performScrollTo().assertIsDisplayed()
  val r=LinkInspection.inspect("https://trusted.example@review.example/login?token=private-fixture").diagnostic()
  assertFalse(r.toString().contains("private-fixture"))
  // Fixture input is not published as a real incident screenshot.
 }
 @Test fun callScreeningIsSystemProtectedAndNeedsUserRole(){
  val component=ComponentName(ui.activity,CallGuardService::class.java)
  @Suppress("DEPRECATION") val info=ui.activity.packageManager.getServiceInfo(component,0)
  assertEquals("android.permission.BIND_SCREENING_SERVICE",info.permission)
  assertTrue(info.exported)
  ui.openDeck("Llamadas")
  ui.onNodeWithText("GUARDIA DE LLAMADAS").assertIsDisplayed()
  ui.onNodeWithText(if(CallJournal.active(ui.activity))"Activada en Android" else "Pendiente de activación").assertIsDisplayed()
  if(!CallJournal.active(ui.activity))ui.onNodeWithText("Activar en Android").assertIsDisplayed()
  shot("calls")
  // Do not accept a role dialog, place calls, or populate the journal with fictitious events.
  ui.openDeck("Resumen")
  ui.onNodeWithText("Analizar mi móvil").assertIsDisplayed()
 }
}
