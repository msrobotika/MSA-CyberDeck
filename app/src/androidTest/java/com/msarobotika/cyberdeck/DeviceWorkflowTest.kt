package com.msarobotika.cyberdeck

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import java.net.ServerSocket
import java.net.InetAddress
import kotlin.concurrent.thread
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/** Exercises real controls and storage on Android against loopback fixtures only. */
class DeviceWorkflowTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    private fun shot(name: String) {
        val image = ui.onRoot().captureToImage().asAndroidBitmap()
        val folder = File(ui.activity.filesDir, "validation").apply { mkdirs() }
        File(folder, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun connectionDnsWebEvidenceAndPersistentNavigation() {
        ui.waitUntil(10000) { ui.onAllNodesWithText("0 evidencias guardadas").fetchSemanticsNodes().isNotEmpty() ||
            ui.onAllNodesWithText("Comprobar mi conexión").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Comprobar mi conexión").performClick()
        ui.waitForIdle()
        ui.onNodeWithText("CONEXIÓN DEL MÓVIL").performScrollTo().assertIsDisplayed()
        shot("android-connection")
        ui.onNodeWithText("Red", useUnmergedTree = false).performClick()
        ui.onNodeWithText("Dominio o IP").performTextInput("127.0.0.1")
        ui.onNodeWithText("Resolver DNS / IP").performClick()
        ui.waitUntil(10000) { ui.onAllNodesWithText("1 direcciones resueltas").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Guardar resultado como evidencia").performScrollTo().performClick()
        ui.waitUntil(10000) { ui.onAllNodesWithText("Resultado guardado con SHA-256").fetchSemanticsNodes().isNotEmpty() }
        shot("android-dns-evidence")

        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            server.soTimeout = 15000
            var accepted = false
            val fixture = thread { server.accept().use { accepted = true } }
            ui.onNodeWithText("Puerto TCP").performScrollTo().performTextClearance()
            ui.onNodeWithText("Puerto TCP").performTextInput(server.localPort.toString())
            ui.onNodeWithText("Comprobar puerto").performScrollTo().performClick()
            fixture.join(15000)
            assertTrue("TCP connection to loopback fixture", accepted)
            ui.waitUntil(10000) { ui.onAllNodes(hasText("Conexión TCP aceptada", substring = true)).fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Guardar resultado como evidencia").performScrollTo().performClick()
            ui.waitUntil(10000) { ui.onAllNodesWithText("Resultado guardado con SHA-256").fetchSemanticsNodes().isNotEmpty() }
        }
        // One HEAD request to a local server; no third-party targets.
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            server.soTimeout = 15000
            var requestLine = ""
            val fixture = thread {
                server.accept().use { socket ->
                    socket.soTimeout = 5000
                    val reader = socket.getInputStream().bufferedReader()
                    requestLine = reader.readLine()
                    while (!reader.readLine().isNullOrEmpty()) { }
                    socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Length: 0\r\n" +
                        "X-Content-Type-Options: nosniff\r\nConnection: close\r\n\r\n").toByteArray())
                }
            }
            ui.onNodeWithText("Web").performClick()
            ui.onNodeWithText("URL http:// o https://").performTextInput("http://127.0.0.1:${server.localPort}/cyberdeck-test")
            ui.onNodeWithText("Revisar URL").performClick()
            fixture.join(15000)
            assertFalse("Loopback fixture timed out", fixture.isAlive)
            assertTrue(requestLine, requestLine.startsWith("HEAD /cyberdeck-test "))
            ui.waitUntil(10000) { ui.onAllNodesWithText("Revisión HTTP/TLS completada").fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Guardar resultado como evidencia").performScrollTo().performClick()
            ui.waitUntil(10000) { ui.onAllNodesWithText("Resultado guardado con SHA-256").fetchSemanticsNodes().isNotEmpty() }
            shot("android-web-evidence")
        }
        // Tabs remain reachable after a long result is scrolled to the bottom.
        ui.onNodeWithText("Evidencias").performScrollTo().performClick()
        ui.onNodeWithText("Verificar copias").performClick()
        ui.waitUntil(10000) { ui.onAllNodes(hasText("copias verificadas con SHA-256", substring = true)).fetchSemanticsNodes().isNotEmpty() }
        shot("android-evidence-verification")
        val store = EvidenceStore(ui.activity)
        val saved = store.list()
        assertTrue(saved.any { it.name.startsWith("diagnostico-dns-") })
        assertTrue(saved.any { it.name.startsWith("diagnostico-web-") })
        assertTrue(store.report(saved).contains("cyberdeck-test"))
        ui.activityRule.scenario.recreate()
        ui.waitForIdle()
        assertEquals(saved.size, EvidenceStore(ui.activity).list().size)
        ui.onNodeWithText("Resumen").performScrollTo().performClick()
        ui.onNodeWithText("EMPIEZA AQUÍ").assertIsDisplayed()
        shot("android-home-actions")
    }
}
