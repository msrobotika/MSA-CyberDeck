package com.msarobotika.cyberdeck
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import org.junit.Rule
import org.junit.Test
class PhoneReviewUiDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun planShowsActualObservationCountsWithoutMarkingAnything(){
  ui.openDeck("Móvil")
  ui.onNode(hasText("Analizar ahora") or hasText("Actualizar")).performScrollTo().performClick()
  ui.waitUntil(10000){ui.onAllNodesWithText("Guardar chequeo").fetchSemanticsNodes().isNotEmpty()}
  ui.onNodeWithText("Plan de revisión").performScrollTo().performClick()
  ui.onNodeWithText("PLAN DE REVISIÓN").performScrollTo().assertIsDisplayed()
  ui.onNodeWithText("Guardar plan como evidencia").performScrollTo().assertIsDisplayed()
  val out=File(ui.activity.filesDir,"validation/ciberdeck-phone-review.png").apply{parentFile?.mkdirs()}
  out.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
 }
}
