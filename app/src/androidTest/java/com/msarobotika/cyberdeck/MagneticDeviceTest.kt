package com.msarobotika.cyberdeck
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.lifecycle.ViewModelProvider
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.Assume.assumeNotNull
import org.junit.Assert.*
class MagneticDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun actualMagnetometerSamplesPauseAndExport(){
  assumeNotNull(ui.activity.getSystemService(SensorManager::class.java).getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD))
  ui.openDeck("Magnético")
  ui.onNodeWithText("Medir campo magnético").performScrollTo().performClick()
  val session=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
  ui.waitUntil(15000){session.magnetic.value.size>=4}
  ui.onNodeWithText("Pausar medición").performScrollTo().performClick()
  assertFalse(session.magneticLive.value)
  assertTrue(session.magnetic.value.all{it.magnitude.isFinite() && it.magnitude>=0})
  ui.onNodeWithText("Fijar referencia").performScrollTo().performClick()
  assertNotNull(session.magneticReference.value)
  val report=MagneticPolicy.report("Actual Android sensor",session.magnetic.value,session.magneticReference.value)
  assertEquals("magnetic",report.kind)
  ui.onNode(hasText("Escala",substring=true)).performScrollTo()
  val shot=File(ui.activity.filesDir,"validation/ciberdeck-magnetic.png").apply{parentFile?.mkdirs()}
  shot.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
  ui.openDeck("Resumen");assertFalse(session.magneticLive.value)
 }
}
