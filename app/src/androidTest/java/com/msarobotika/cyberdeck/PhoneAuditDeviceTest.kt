package com.msarobotika.cyberdeck

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import androidx.lifecycle.ViewModelProvider
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class PhoneAuditDeviceTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private fun shot(name:String){val f=File(ui.activity.filesDir,"validation/ciberdeck-$name.png").apply{parentFile?.mkdirs()};f.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}}
    @Test fun oneTapReadsRealPhoneAndKeepsObservationAcrossRecreation(){
        ui.onNodeWithTag("home-phone").assertIsDisplayed()
        shot("home")
        ui.onNodeWithTag("home-phone").performClick()
        ui.waitUntil(10000){ui.onAllNodesWithText("Guardar chequeo").fetchSemanticsNodes().isNotEmpty()}
        val snapshot=ViewModelProvider(ui.activity)[DeckSessionModel::class.java].phone.value!!
        assertEquals("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",snapshot.device)
        assertTrue(snapshot.ramTotal==null || snapshot.ramTotal>0)
        assertTrue(snapshot.battery==null || snapshot.battery in 0..100)
        assertTrue(snapshot.checks.any{it.title=="Bloqueo de pantalla"})
        ui.onNodeWithTag("phone-advanced").performScrollTo().performClick()
        ui.onNodeWithText("Bloqueo de pantalla").performScrollTo().assertIsDisplayed()
        shot("phone")
        ui.activityRule.scenario.recreate()
        assertEquals(snapshot.at,ViewModelProvider(ui.activity)[DeckSessionModel::class.java].phone.value?.at)
        try{ui.onNodeWithText("Recursos").performScrollTo().performClick()}
        catch(e:Throwable){File(ui.activity.filesDir,"validation/phone-mode-recreation-private.txt").writeText(ui.onRoot().printToString());shot("mode-recreation-private");throw e}
        ui.onNodeWithText("Iniciar gráfica en vivo").performScrollTo().performClick()
        ui.waitUntil(10000){ViewModelProvider(ui.activity)[DeckSessionModel::class.java].samples.value.size>=2}
        ui.onNodeWithText("Pausar gráfica").performScrollTo().performClick()
        ui.onNodeWithText("RAM disponible").performScrollTo().assertIsDisplayed()
        ui.onNodeWithTag("deck-list").performTouchInput{swipeUp(startY=height*.7f,endY=height*.4f)}
        shot("resources")
        assertFalse(ViewModelProvider(ui.activity)[DeckSessionModel::class.java].phoneLive.value)
        assertTrue(ViewModelProvider(ui.activity)[DeckSessionModel::class.java].samples.value.all{it.ramTotal==null || it.ramTotal>0})
        val store=EvidenceStore(ui.activity)
        val entry=store.saveBytes("diagnostico-phone-validation.json",snapshot.diagnostic().toEvidenceJson().toByteArray()).last()
        try{assertEquals("phone",store.diagnostic(entry).kind);assertEquals(snapshot.device,store.diagnostic(entry).target)}finally{store.delete(entry.id)}
    }
}
