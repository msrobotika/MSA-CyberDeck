package com.msarobotika.cyberdeck

import android.content.ContentValues
import android.provider.MediaStore
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
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
 @Test fun androidPickerReadsOwnedImageAndSavesPrivateReport(){
  val resolver=ui.activity.contentResolver
  val name="CQR-${java.util.UUID.randomUUID().toString().take(6)}.png"
  val payload="WIFI:T:WPA;S:OWNED-PICKER-FIXTURE;P:PRIVATE-PICKER-FIXTURE;;"
  val bytes=image(payload)
  val uri=resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,ContentValues().apply{
   put(MediaStore.Images.Media.DISPLAY_NAME,name);put(MediaStore.Images.Media.MIME_TYPE,"image/png")
   put(MediaStore.Images.Media.RELATIVE_PATH,"Download");put(MediaStore.Images.Media.IS_PENDING,1)
  })?:error("No se pudo crear la imagen propia de ensayo")
  val store=EvidenceStore(ui.activity);val before=store.list().map{it.id}.toSet()
  try {
   resolver.openOutputStream(uri)!!.use{it.write(bytes)}
   resolver.update(uri,ContentValues().apply{put(MediaStore.Images.Media.IS_PENDING,0)},null,null)
   ui.openDeck("Enlaces")
   ui.onNodeWithTag("deck-list").performScrollToNode(hasTestTag("qr-panel"))
   ui.onNodeWithTag("qr-pick").performScrollTo().performClick()
   val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
   var selected=false
   val deadline=android.os.SystemClock.uptimeMillis()+15000
   while(!selected && android.os.SystemClock.uptimeMillis()<deadline){
    val root=automation.rootInActiveWindow
    val node=root?.findAccessibilityNodeInfosByText(name)?.firstOrNull{it.text?.toString()?.contains(name)==true || it.contentDescription?.toString()?.contains(name)==true}
    if(node!=null){var target:AccessibilityNodeInfo?=node;while(target!=null && !target.isClickable)target=target.parent;selected=target?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true}
    if(!selected)android.os.SystemClock.sleep(250)
   }
   if(!selected){
    val root=automation.rootInActiveWindow
    File(ui.activity.filesDir,"validation/qr-picker-package.txt").writeText("package=${root?.packageName}; window=${root?.className}; ownName=$name")
    automation.takeScreenshot()?.let{bitmap->File(ui.activity.filesDir,"validation/qr-picker-private.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()}
   }
   assertTrue("El selector no mostró la imagen propia de ensayo",selected)
   val session=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
   ui.waitUntil(15000){session.qr.value!=null}
   assertEquals(payload,session.qr.value!!.payload)
   assertEquals(PeerProtocol.sha256(bytes),session.qr.value!!.imageSha256)
   ui.onAllNodesWithText(payload).assertCountEquals(0)
   ui.onNodeWithTag("qr-save").performScrollTo().performClick()
   ui.waitUntil(10000){store.list().any{it.id !in before && runCatching{store.diagnostic(it).kind=="qr" && store.diagnostic(it).checkedAt==session.qr.value!!.at}.getOrDefault(false)}}
   val saved=store.list().first{it.id !in before && runCatching{store.diagnostic(it).checkedAt==session.qr.value!!.at}.getOrDefault(false)}
   val report=store.diagnostic(saved)
   assertEquals("qr",report.kind);assertFalse(report.toString().contains("PRIVATE-PICKER-FIXTURE"));assertFalse(report.toString().contains("OWNED-PICKER-FIXTURE"))
  } finally {
   val works=AuditWorkStore(ui.activity)
   store.list().filter{it.id !in before && runCatching{store.diagnostic(it).details.any{d->d.second==PeerProtocol.sha256(bytes)}}.getOrDefault(false)}.forEach{saved->
    works.read().works.filter{w->w.evidence.any{it.id==saved.id}}.forEach{works.detach(it.id,setOf(saved.id))};store.delete(saved.id)
   }
   resolver.delete(uri,null,null)
  }
 }
 @Test fun invalidImageFailsWithoutDiagnostic(){assertTrue(runCatching{QrImageReader.readBytes("NOT AN IMAGE".toByteArray())}.isFailure)}
}
