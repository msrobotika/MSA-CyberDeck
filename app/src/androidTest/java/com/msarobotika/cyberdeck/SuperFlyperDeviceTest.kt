package com.msarobotika.cyberdeck

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import org.junit.Rule
import org.junit.Test

class SuperFlyperDeviceTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    @Test fun groupedDrawerExpandsAndNavigatesToUsb(){
        ui.onNodeWithContentDescription("Abrir menú").performClick()
        ui.onNodeWithText("MENÚ DE TRABAJO").assertIsDisplayed()
        ui.onNodeWithTag("drawer-group-Hardware y módulos").performScrollTo().performClick()
        ui.onNodeWithTag("drawer-tool-USB").performScrollTo().assertIsDisplayed()
        val file=File(ui.activity.filesDir,"validation/superflyper-menu.png").apply{parentFile?.mkdirs()}
        file.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
        ui.onNodeWithTag("drawer-tool-USB").performClick()
        ui.onNodeWithText("SUPERFLYPER · ENLACE USB").assertIsDisplayed()
        val usb=File(ui.activity.filesDir,"validation/superflyper-usb.png")
        usb.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun usbMonitorAndModuleStagesAreReachableWithoutMockReadings(){
        ui.onNodeWithText("TU LABORATORIO").assertIsDisplayed()
        ui.openDeck("USB")
        ui.onNodeWithText("SUPERFLYPER · ENLACE USB").assertIsDisplayed()
        ui.onNodeWithText("Guardar registro").performScrollTo().assertIsNotEnabled()
        ui.onNodeWithText("Sin tramas recibidas. No se muestran datos de demostración.").performScrollTo().assertExists()
        ui.openDeck("Módulos")
        ui.onNodeWithText("SUPERFLYPER · MÓDULOS").assertIsDisplayed()
        ui.onNodeWithText("Asistente IA").performScrollTo().assertExists()
        ui.onNodeWithText("Sin proveedor ni modelo configurado. No se envían evidencias a servicios de IA.").assertExists()
        ui.openDeck("NFC")
        ui.onNodeWithText("Leer etiqueta NFC · 30 s").assertIsDisplayed()
    }
}
