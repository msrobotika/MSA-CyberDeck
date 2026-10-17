package com.msarobotika.cyberdeck

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.net.ServerSocket
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class LanInventoryDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    @Test fun discoversKnownAdvertisementAndSavesObservedPort() {
        val manager = ui.activity.getSystemService(NsdManager::class.java)
        val registered = CountDownLatch(1)
        var name = ""
        var failure: Int? = null
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) { name = info.serviceName; registered.countDown() }
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) { failure = errorCode; registered.countDown() }
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
        }
        ServerSocket(0).use { fixture ->
            val info = NsdServiceInfo().apply {
                serviceName = "CyberDeck-Test-${System.currentTimeMillis()}"
                serviceType = "_http._tcp."
                port = fixture.localPort
            }
            try {
                manager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
                assertTrue("Local mDNS fixture registration timed out", registered.await(12, TimeUnit.SECONDS))
                assertNull("Registration failure: $failure", failure)
                ui.onNodeWithText("Mi red").performScrollTo().performClick()
                ui.onNodeWithText("Sin búsqueda iniciada. No se consulta la red automáticamente.").assertExists()
                ui.onNodeWithText("Descubrir servicios · 15 s").performClick()
                ui.waitUntil(14000) { ui.onAllNodesWithText(name).fetchSemanticsNodes().isNotEmpty() }
                ui.waitUntil(14000) { ui.onAllNodesWithText("Puerto anunciado: ${fixture.localPort}").fetchSemanticsNodes().isNotEmpty() }
                ui.onNodeWithText("Detener descubrimiento").performScrollTo().performClick()
                ui.onNodeWithText("Búsqueda finalizada").assertExists()
                ui.onNodeWithText("Guardar inventario como evidencia").performScrollTo().performClick()
                ui.waitUntil(10000) { ui.onAllNodesWithText("Inventario guardado con SHA-256").fetchSemanticsNodes().isNotEmpty() }
                val store = EvidenceStore(ui.activity)
                val saved = store.list().last { it.name.startsWith("diagnostico-lan-") }
                val json = File(ui.activity.filesDir, "evidence/${saved.id}").readText()
                assertTrue(json.contains(name))
                assertTrue(json.contains("Puerto anunciado: ${fixture.localPort}"))
                assertTrue(store.verify(listOf(saved)).contains("1 copias verificadas"))
                val folder = File(ui.activity.filesDir, "validation").apply { mkdirs() }
                File(folder, "android-lan-private.png").outputStream().use {
                    ui.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                // A new search is bounded even if the user does not press Stop.
                ui.onNodeWithText("Repetir descubrimiento · 15 s").performScrollTo().performClick()
                ui.waitUntil(18000) { ui.onAllNodesWithText("Búsqueda finalizada").fetchSemanticsNodes().isNotEmpty() }
            } finally {
                runCatching { manager.unregisterService(listener) }
            }
        }
    }
}
