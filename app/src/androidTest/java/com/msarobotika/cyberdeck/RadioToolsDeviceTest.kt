package com.msarobotika.cyberdeck

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import org.junit.Rule
import org.junit.Test

class RadioToolsDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private fun shot(name: String) {
        val folder = File(ui.activity.filesDir, "validation").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun newModulesHaveExplicitActionsAndNoAutomaticScans() {
        ui.onNodeWithText("Bluetooth").performScrollTo().performClick()
        ui.onNodeWithText("BLUETOOTH · INSPECTOR BLE").assertIsDisplayed()
        ui.onNodeWithText("Sin búsqueda iniciada").assertExists()
        shot("android-bluetooth-idle")
        ui.onNodeWithText("NFC").performScrollTo().performClick()
        ui.onNodeWithText("NFC · INSPECTOR DE ETIQUETAS").assertIsDisplayed()
        ui.onNodeWithText("Lector NFC detenido").assertExists()
        shot("android-nfc-idle")
        ui.onNodeWithText("Sesiones").performScrollTo().performClick()
        ui.onNodeWithText("MÓVIL A MÓVIL · SESIÓN LOCAL").assertIsDisplayed()
        ui.onNodeWithText("Sin sesión iniciada").assertExists()
        ui.onNodeWithText("Elegir archivo y enviar").performScrollTo().assertIsNotEnabled()
        shot("android-peer-idle")
    }
}
