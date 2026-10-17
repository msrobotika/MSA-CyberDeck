package com.msarobotika.cyberdeck

import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Test
import org.junit.Rule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*

/** Run explicitly with role=receiver/sender on two owner-authorized physical phones. */
class CrossPhoneDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Test fun transferFixtureAcrossPhones() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val role = InstrumentationRegistry.getArguments().getString("role") ?: error("Set role=receiver or role=sender")
        require(role in listOf("receiver", "sender"))
        val folder = File(context.filesDir, "validation").apply { mkdirs() }
        val fixture = ByteArray(1024 * 1024) { (it % 251).toByte() }
        val hash = PeerProtocol.sha256(fixture)
        val done = CountDownLatch(1)
        val status = AtomicReference("")
        val saved = AtomicBoolean(false)
        lateinit var session: PeerSession
        instrumentation.runOnMainSync {
            ui.activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            session = PeerSession(context, { state ->
                status.set(state.status)
                File(folder, "two-phone-$role.json").writeText(JSONObject()
                    .put("role", role).put("status", state.status).put("invitation", state.invitation)
                    .put("active", state.active).put("saved", saved.get()).put("expectedSha256", hash).toString())
                if (role == "receiver" && state.offer != null) session.decide(true) // fixture-only test consent
                if (!state.active) done.countDown()
            }, { name, bytes ->
                assertEquals("two-phone-fixture.bin", name)
                assertEquals(fixture.size, bytes.size)
                assertEquals(hash, PeerProtocol.sha256(bytes))
                val entry = EvidenceStore(context).saveBytes(name, bytes).last()
                assertEquals(hash, entry.sha256)
                saved.set(true)
            })
            if (role == "receiver") session.receive()
            else {
                val invitation = File(folder, "cross-phone-invite.txt").readText()
                session.send(invitation, "two-phone-fixture.bin", fixture)
            }
        }
        try {
            assertTrue("Session timed out: ${status.get()}", done.await(120, TimeUnit.SECONDS))
            if (role == "receiver") {
                assertEquals("Archivo recibido y guardado con SHA-256", status.get())
                assertTrue(saved.get())
            } else assertEquals("Entrega confirmada por el receptor", status.get())
        } finally { instrumentation.runOnMainSync { session.close() } }
    }
}
