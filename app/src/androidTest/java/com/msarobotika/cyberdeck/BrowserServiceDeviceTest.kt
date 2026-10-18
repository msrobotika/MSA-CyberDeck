package com.msarobotika.cyberdeck

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import java.net.URI
import java.net.URL
import java.net.HttpURLConnection
import java.net.Socket
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class BrowserServiceDeviceTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private fun start():String {
        ui.runOnUiThread{BrowserTransferService.start(ui.activity)}
        ui.waitUntil(10000){BrowserTransferService.state.value.url.isNotBlank()}
        return BrowserTransferService.state.value.url
    }
    @Test fun receiverSurvivesActivityStopAndManualStopClosesItsSocket() {
        val context=ui.activity.applicationContext
        try {
            val address=start()
            val parsed=URI(address)
            ui.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            val connection=URL(address).openConnection() as HttpURLConnection
            connection.connectTimeout=3000;connection.readTimeout=3000
            try {assertEquals(200,connection.responseCode);assertTrue(connection.inputStream.bufferedReader().readText().contains("MSA CiberDeck"))}
            finally{connection.disconnect()}
            assertTrue(BrowserTransferService.state.value.active)
            ui.runOnUiThread{BrowserTransferService.stop(context)}
            ui.waitUntil(5000){!BrowserTransferService.state.value.active}
            assertTrue(runCatching{Socket().use{it.connect(InetSocketAddress(parsed.host,parsed.port),1500)}}.isFailure)
        } finally {
            ui.runOnUiThread{BrowserTransferService.stop(context)}
            ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        }
    }
    @Test fun uploadWhileActivityStoppedWaitsForApprovalThenPersistsExactBytes() {
        val context=ui.activity.applicationContext
        val store=EvidenceStore(context)
        val name="service-upload-validation-${System.currentTimeMillis()}.txt"
        val bytes="Generated foreground-service transfer fixture".toByteArray()
        val executor=Executors.newSingleThreadExecutor()
        try {
            val address=start()
            val parsed=URI(address)
            ui.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            val response=executor.submit<String>{
                val connection=URL("$address/upload").openConnection() as HttpURLConnection
                connection.requestMethod="POST";connection.doOutput=true;connection.connectTimeout=5000;connection.readTimeout=15000
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.setRequestProperty("Origin","http://${parsed.rawAuthority}")
                connection.setRequestProperty("X-CyberDeck-Upload","selected-file")
                connection.setRequestProperty("X-File-Name",name)
                connection.setRequestProperty("Content-Type","application/octet-stream")
                try {connection.outputStream.use{it.write(bytes)};connection.inputStream.bufferedReader().readText()}finally{connection.disconnect()}
            }
            ui.waitUntil(10000){BrowserTransferService.state.value.offer?.name==name}
            assertFalse(store.list().any{it.name==name})
            ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            ui.runOnUiThread{BrowserTransferService.decide(true)}
            assertTrue(response.get(15,TimeUnit.SECONDS).contains(PeerProtocol.sha256(bytes)))
            val entry=store.list().single{it.name==name}
            assertArrayEquals(bytes,store.verifiedFile(entry).readBytes())
            ui.waitUntil(5000){!BrowserTransferService.state.value.active}
        } finally {
            ui.runOnUiThread{BrowserTransferService.stop(context)}
            executor.shutdownNow()
            ui.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            store.list().filter{it.name==name}.forEach{store.delete(it.id)}
        }
    }
}
