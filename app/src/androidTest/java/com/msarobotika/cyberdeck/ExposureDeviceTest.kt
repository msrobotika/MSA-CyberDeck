package com.msarobotika.cyberdeck
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import java.net.ServerSocket
import java.net.InetAddress
import java.io.File
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import kotlin.concurrent.thread
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
class ExposureDeviceTest {
 @get:Rule val ui=createAndroidComposeRule<MainActivity>()
 @Test fun actualTcpToOwnedLoopbackFixtureAndPinnedIp(){
  ServerSocket(0,1,InetAddress.getByName("127.0.0.1")).use{server->
   server.soTimeout=20000;var accepted=false;val fixture=thread{server.accept().use{accepted=true}}
   ui.openDeck("Equipo")
   ui.onNodeWithText("Equipo: dominio o IP").performTextInput("127.0.0.1")
   ui.onNodeWithText("Puertos TCP").performTextClearance();ui.onNodeWithText("Puertos TCP").performTextInput(server.localPort.toString())
   ui.onNodeWithText("Resolver destino").performScrollTo().performClick()
   val session=ViewModelProvider(ui.activity)[DeckSessionModel::class.java]
   ui.waitUntil(15000){session.exposure.value.plan!=null && !session.exposure.value.busy}
   ui.onNodeWithText("IP elegida: 127.0.0.1").performScrollTo().assertIsDisplayed()
   ui.onNodeWithText("Tengo autorización para comprobar este equipo").performScrollTo().performClick()
   ui.onNodeWithText("Comprobar servicios").performScrollTo().performClick()
   ui.waitUntil(15000){session.exposure.value.result!=null && !session.exposure.value.busy}
   fixture.join(3000);assertTrue(accepted)
   val result=session.exposure.value.result!!;assertEquals("127.0.0.1",result.address);assertEquals(PortState.OPEN,result.probes.single().state)
   ui.onNodeWithText("Fijar referencia").performScrollTo().performClick();assertEquals(result,session.exposure.value.reference)
   ui.onNodeWithText("RESULTADOS REALES").performScrollTo().assertIsDisplayed()
   val shot=File(ui.activity.filesDir,"validation/ciberdeck-equipo.png").apply{parentFile?.mkdirs()}
   shot.outputStream().use{ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}
   ui.activityRule.scenario.recreate();assertEquals(result.at,ViewModelProvider(ui.activity)[DeckSessionModel::class.java].exposure.value.result?.at)
  }
 }
}
