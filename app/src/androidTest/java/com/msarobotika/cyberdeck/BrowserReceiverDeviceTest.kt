package com.msarobotika.cyberdeck

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.net.Socket
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import android.webkit.WebView
import android.webkit.WebViewClient

class BrowserReceiverDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private fun transfer(approve: Boolean) {
        val ready = CountDownLatch(1)
        val address = AtomicReference("")
        val saves = AtomicInteger()
        val bytes = "Generated browser fixture".toByteArray()
        lateinit var receiver: BrowserReceiver
        ui.runOnUiThread {
            ui.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            receiver = BrowserReceiver(ui.activity, { state ->
                if(state.url.isNotBlank()) { address.set(state.url); ready.countDown() }
                if(state.offer != null) receiver.decide(approve)
            }, { name, data -> assertEquals("fixture.txt",name); assertArrayEquals(bytes,data); saves.incrementAndGet() })
            receiver.start()
        }
        try {
            assertTrue("Receiver did not start on local network",ready.await(10,TimeUnit.SECONDS))
            val url = URI(address.get())
            val authority = url.rawAuthority
            fun request(method: String, path: String, headers: String = "", body: ByteArray = byteArrayOf()): String =
                Socket(url.host,url.port).use { socket ->
                    socket.soTimeout = 10000
                    socket.getOutputStream().apply {
                        write("$method $path HTTP/1.1\r\nHost: $authority\r\n$headers\r\n".toByteArray()); write(body); flush()
                    }
                    val input = socket.getInputStream()
                    val header = ByteArrayOutputStream()
                    var suffix = 0
                    while (suffix != 0x0d0a0d0a) {
                        val byte = input.read()
                        if (byte < 0) return@use ""
                        check(header.size() < 8192)
                        header.write(byte); suffix = (suffix shl 8) or byte
                    }
                    val text = header.toString("US-ASCII")
                    val count = text.lineSequence().first { it.startsWith("Content-Length:") }.substringAfter(':').trim().toInt()
                    val payload = ByteArray(count)
                    DataInputStream(input).readFully(payload)
                    text + payload.toString(Charsets.UTF_8)
                }
            val page = request("GET",url.path)
            assertTrue(page.contains("HTTP/1.1 200")); assertTrue(page.contains("HTTP sin cifrar"))
            request("POST",url.path+"/upload", "Origin: http://evil.invalid\r\nContent-Length: 0\r\n")
            assertEquals(0,saves.get())
            val result = request("POST",url.path+"/upload", "Origin: http://$authority\r\nX-CyberDeck-Upload: selected-file\r\nX-File-Name: fixture.txt\r\nContent-Type: application/octet-stream\r\nContent-Length: ${bytes.size}\r\n",bytes)
            if(approve) { assertTrue(result.contains(PeerProtocol.sha256(bytes))); assertEquals(1,saves.get()) }
            else { assertTrue(result.contains("403")); assertEquals(0,saves.get()) }
        } finally { ui.runOnUiThread { receiver.close() } }
    }
    @Test fun chosenFileRequiresApprovalAndReturnsReceiverHash() { transfer(true) }
    @Test fun rejectionNeverSavesTheFile() { transfer(false) }
    @Test fun actualWebPageUploadsGeneratedFileUsingBrowserFetch() {
        val saved = CountDownLatch(1)
        val status = AtomicReference("")
        val fixture = "Browser engine fixture"
        lateinit var receiver: BrowserReceiver
        lateinit var web: WebView
        ui.runOnUiThread {
            ui.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            web = WebView(ui.activity).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        view.evaluateJavascript("""(()=>{const transfer=new DataTransfer();transfer.items.add(new File(['$fixture'],'browser-fixture.txt',{type:'application/octet-stream'}));document.getElementById('file').files=transfer.files;document.getElementById('send').click()})()""", null)
                    }
                }
            }
            var loaded = false
            receiver = BrowserReceiver(ui.activity, { state ->
                status.set(state.status)
                if (state.url.isNotBlank() && !loaded) { loaded = true; web.loadUrl(state.url) }
                if (state.offer != null) receiver.decide(true)
            }, { name, bytes ->
                assertEquals("browser-fixture.txt",name)
                assertEquals(fixture,bytes.toString(Charsets.UTF_8)); saved.countDown()
            })
            receiver.start()
        }
        try { assertTrue("Browser upload failed: ${status.get()}",saved.await(25,TimeUnit.SECONDS)) }
        finally { ui.runOnUiThread { receiver.close(); web.destroy() } }
    }
}
