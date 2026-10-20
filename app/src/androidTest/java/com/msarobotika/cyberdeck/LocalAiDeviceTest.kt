package com.msarobotika.cyberdeck
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import android.content.ComponentName
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
class LocalAiDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun realNativeRuntimeWithoutFakeModelOrResponse(){
  @Suppress("DEPRECATION") val service=ui.activity.packageManager.getServiceInfo(ComponentName(ui.activity,LocalAiService::class.java),0)
  assertFalse(service.exported);assertTrue(service.processName.endsWith(":local_ai"))
  val store=AiModelStore(ui.activity);val before=store.current()
  val invalid=File.createTempFile("invalid-model-fixture-",".litertlm",ui.activity.cacheDir)
  try{invalid.writeBytes(ByteArray(16384));assertTrue(runCatching{store.import(Uri.fromFile(invalid)) {}}.isFailure);assertEquals(before,store.current())}finally{invalid.delete()}
  ui.openDeck("IA")
  ui.waitUntil(20000){ViewModelProvider(ui.activity)[DeckSessionModel::class.java].ai.value.runtime}
  val state=ViewModelProvider(ui.activity)[DeckSessionModel::class.java].ai.value
  assertTrue(state.connected);assertFalse(state.loaded);assertEquals("",state.output)
  if(before==null)ui.onNodeWithText("SIN MODELO IMPORTADO").assertIsDisplayed()
  ui.onNodeWithText("Analizar en este móvil").performScrollTo().assertIsNotEnabled()
  ui.onNodeWithTag("deck-list").performScrollToIndex(0)
  val shot=File(ui.activity.filesDir,"validation/ciberdeck-local-ai.png").apply{parentFile?.mkdirs()}
  shot.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
  ui.openDeck("Resumen")
  ui.waitUntil(10000){!ViewModelProvider(ui.activity)[DeckSessionModel::class.java].ai.value.connected}
  ui.onNodeWithTag("home-phone").assertIsDisplayed()
 }
}
