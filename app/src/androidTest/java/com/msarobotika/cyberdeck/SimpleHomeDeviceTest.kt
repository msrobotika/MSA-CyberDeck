package com.msarobotika.cyberdeck
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.io.File
import org.junit.Rule
import org.junit.Test
class SimpleHomeDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun threeMainActionsAreVisibleWithoutOpeningAdvancedContent(){
  ui.activity.runOnUiThread{ui.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}
  ui.openDeck("Resumen");ui.onNodeWithTag("deck-list").performScrollToIndex(0)
  ui.onNodeWithTag("home-phone").assertIsDisplayed()
  ui.onNodeWithTag("home-agent").assertIsDisplayed()
  ui.onNodeWithTag("home-investigation").assertIsDisplayed()
  ui.onAllNodesWithTag("home-area-proximity").assertCountEquals(0)
  File(ui.activity.filesDir,"validation/ciberdeck-home-031.png").apply{parentFile?.mkdirs()}.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
  ui.onNodeWithTag("deck-list").performScrollToNode(hasTestTag("home-more"))
  ui.onNodeWithTag("home-more").performClick()
  ui.onNodeWithTag("deck-list").performScrollToNode(hasTestTag("home-area-proximity"))
  ui.onNodeWithTag("home-area-proximity").assertIsDisplayed()
  ui.onNodeWithTag("deck-list").performScrollToNode(hasTestTag("home-more"))
  ui.onNodeWithTag("home-more").performClick()
  ui.onAllNodesWithTag("home-area-proximity").assertCountEquals(0)
 }
}
