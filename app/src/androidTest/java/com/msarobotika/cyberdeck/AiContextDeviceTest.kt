package com.msarobotika.cyberdeck

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

class AiContextDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun realModelReceivesDatedPhoneContextAndUserCanExcludeIt(){
  assumeTrue(InstrumentationRegistry.getArguments().getString("agentAi")=="qwen3")
  ui.runOnIdle{ui.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
  ui.openDeck("IA")
  val session=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
  ui.waitUntil(20000){session.ai.value.runtime && session.aiModel.value!=null && !session.ai.value.busy}
  ui.onNodeWithText("Cargar IA local").performScrollTo().performClick()
  ui.waitUntil(95000){!session.ai.value.busy && (session.ai.value.loaded || session.ai.value.error.isNotBlank())}
  assertTrue(session.ai.value.error,session.ai.value.loaded)
  ui.onNodeWithText("Situación y objetivo · opcional").performScrollTo().performTextInput("Revisión local de mi teléfono; orientar próximos pasos.")
  File(ui.activity.filesDir,"validation/ciberdeck-ai-context-settings.png").apply{parentFile?.mkdirs()}.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
  ui.onNodeWithText("Pregunta al analista").performScrollTo().performTextClearance()
  ui.onNodeWithText("Pregunta al analista").performTextInput("Indica la versión Android y el modelo que aparecen en la instantánea; después una limitación. Tres frases.")
  ui.onNodeWithText("Analizar en este móvil").performScrollTo().performClick()
  ui.waitUntil(95000){!session.ai.value.busy && session.aiRequest.value!=null && (session.ai.value.output.isNotBlank() || session.ai.value.error.isNotBlank())}
  assertEquals("",session.ai.value.error);assertTrue(session.ai.value.output.isNotBlank())
  val request=session.aiRequest.value!!
  assertNotEquals("Ninguno",request.context)
  val folder=File(ui.activity.filesDir,"validation").apply{mkdirs()}
  File(folder,"ai-context-phone-private.txt").writeText(request.environment+"\nRESPONSE:\n"+session.ai.value.output)
  fun compact(value:String)=java.text.Normalizer.normalize(value,java.text.Normalizer.Form.NFKC).filter{it.isLetterOrDigit()}.lowercase(java.util.Locale.ROOT)
  val output=compact(session.ai.value.output);val actualModel=compact(android.os.Build.MODEL)
  val version=Regex.escape(compact(android.os.Build.VERSION.RELEASE))
  assertTrue("Debe utilizar un dato real; salida: "+session.ai.value.output.take(1800),output.contains(actualModel) || Regex("android[a-z]{0,12}"+version+"(?![0-9])").containsMatchIn(output))
  assertTrue(request.prompt.contains(request.environment));assertEquals(PeerProtocol.sha256(request.prompt.toByteArray(Charsets.UTF_8)),request.promptHash)
  assertTrue(request.environment.contains(android.os.Build.MODEL));assertTrue(request.environment.contains("Instantánea Android tomada"));assertTrue(request.environment.contains("orientar próximos pasos"));assertEquals(AiContextSnapshot(request.environment).sha256,request.environmentHash)
  ui.onNodeWithText("Contexto usado en esta respuesta").performScrollTo().performClick()
  File(folder,"ciberdeck-ai-context.png").outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
  val store=EvidenceStore(ui.activity);val before=store.list().size
  ui.onNodeWithText("Guardar interpretación").performScrollTo().performClick()
  ui.waitUntil(10000){store.list().size>before}
  val report=store.diagnostic(store.list().last());assertTrue(report.details.any{it.first=="SHA-256 del contexto de entorno" && it.second==request.environmentHash})
  ui.onNodeWithTag("ai-include-phone").performScrollTo().performClick()
  ui.onNodeWithText("Situación y objetivo · opcional").performScrollTo().performTextClearance()
  ui.onNodeWithText("Situación y objetivo · opcional").performTextInput("Ejemplo: taller junto a centro de transformación tras un panel sándwich. No estoy midiendo allí.")
  ui.onNodeWithText("Pregunta al analista").performScrollTo().performTextClearance()
  ui.onNodeWithText("Pregunta al analista").performTextInput("Qué puede medir el móvil y qué falta para evaluar la exposición del taller? Tres frases.")
  ui.onNodeWithText("Alcance para campos y salud").performScrollTo().assertExists()
  ui.onNodeWithText("Analizar en este móvil").performScrollTo().performClick()
  ui.waitUntil(95000){!session.ai.value.busy && session.aiRequest.value?.environmentHash!=request.environmentHash && (session.ai.value.output.isNotBlank() || session.ai.value.error.isNotBlank())}
  assertEquals("",session.ai.value.error)
  val excluded=session.aiRequest.value!!;assertFalse(excluded.environment.contains("Equipo Android:"));assertTrue(excluded.environment.contains("sin lectura adjunta"));assertTrue(excluded.environment.contains("No estoy midiendo allí"))
  File(folder,"ai-context-workshop-private.txt").writeText(excluded.environment+"\nRESPONSE:\n"+session.ai.value.output)
 }
}
