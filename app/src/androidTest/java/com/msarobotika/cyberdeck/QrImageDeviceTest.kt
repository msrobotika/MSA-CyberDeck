package com.msarobotika.cyberdeck

import android.graphics.Bitmap
import android.net.Uri
import android.view.WindowManager
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class QrImageDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Before fun keepVisible(){ui.activity.runOnUiThread{ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}}
 private fun image(text:String):ByteArray {
  val matrix=QRCodeWriter().encode(text,BarcodeFormat.QR_CODE,512,512)
  val bitmap=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888)
  try{bitmap.setPixels(IntArray(512*512){i->if(matrix[i%512,i/512])-0x1000000 else -1},0,512,0,0,512,512);return ByteArrayOutputStream().also{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}.toByteArray()}finally{bitmap.recycle()}
 }
 @Test fun pngReadThroughAndroidUriProducesReportWithoutSecrets(){
  val bytes=image("WIFI:T:WPA;S:OWNED-UI-FIXTURE;P:PRIVATE-UI-FIXTURE;;")
  val file=File.createTempFile("qr-device-fixture-",".png",ui.activity.cacheDir)
  try{file.writeBytes(bytes);val result=QrImageReader.read(ui.activity,Uri.fromFile(file));assertEquals("Configuración Wi-Fi",result.type);assertEquals(PeerProtocol.sha256(bytes),result.imageSha256);assertFalse(result.diagnostic().toString().contains("PRIVATE-UI-FIXTURE"))}finally{assertTrue(file.delete())}
 }
 @Test fun payloadIsHiddenAndCanBeDiscardedWithoutSaving(){
  ui.openDeck("Enlaces")
  val session=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
  val observation=QrImageReader.readBytes(image("WIFI:T:WPA;S:OWNED-UI-FIXTURE;P:PRIVATE-UI-FIXTURE;;"))
  ui.runOnIdle{session.qr.value=observation}
  ui.onNodeWithTag("deck-list").performScrollToNode(hasTestTag("qr-panel"))
  ui.onAllNodesWithText(observation.payload).assertCountEquals(0)
  ui.onNodeWithTag("qr-reveal").performScrollTo().performClick()
  ui.onNodeWithText(observation.payload).performScrollTo().assertIsDisplayed()
  ui.onNodeWithTag("qr-reveal").performScrollTo().performClick()
  ui.onAllNodesWithText(observation.payload).assertCountEquals(0)
  ui.onNodeWithText("Descartar lectura QR").performScrollTo().performClick()
  ui.runOnIdle{assertNull(session.qr.value)}
  ui.onNodeWithTag("qr-panel").performScrollTo()
  File(ui.activity.filesDir,"validation/ciberdeck-qr-entry-029.png").apply{parentFile?.mkdirs()}.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
 }
 @Test fun invalidImageFailsWithoutDiagnostic(){assertTrue(runCatching{QrImageReader.readBytes("NOT AN IMAGE".toByteArray())}.isFailure)}
}
