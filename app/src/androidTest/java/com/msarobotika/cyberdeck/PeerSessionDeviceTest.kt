package com.msarobotika.cyberdeck

import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.io.File
import org.junit.Test
import org.junit.Assert.*

/** Two real TLS endpoints in one Android process. Does not substitute for two-phone validation. */
class PeerSessionDeviceTest {
    private fun transfer(approve: Boolean, wrongPin: Boolean): Pair<String, Boolean> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val invitationReady = CountDownLatch(1)
        val senderDone = CountDownLatch(1)
        val invitation = AtomicReference("")
        val senderStatus = AtomicReference("")
        val receiverStatus = AtomicReference("")
        val wasSaved = AtomicBoolean(false)
        val fixture = "CyberDeck TLS fixture\n".toByteArray()
        lateinit var receiver: PeerSession
        lateinit var sender: PeerSession
        instrumentation.runOnMainSync {
            receiver = PeerSession(context, { state ->
                receiverStatus.set(state.status)
                File(context.filesDir, "validation").apply { mkdirs() }.resolve("local-tls-receiver-status.txt").appendText(state.status + "\n")
                if (state.invitation.isNotBlank()) { invitation.set(state.invitation); invitationReady.countDown() }
                if (state.offer != null) receiver.decide(approve)
            }, { name, bytes ->
                assertEquals("peer-tls-fixture.txt", name)
                assertArrayEquals(fixture, bytes)
                val store = EvidenceStore(context)
                val entry = store.saveBytes(name, bytes).last()
                assertEquals(PeerProtocol.sha256(fixture), entry.sha256)
                wasSaved.set(true)
            })
            sender = PeerSession(context, { state ->
                senderStatus.set(state.status)
                if (!state.active) senderDone.countDown()
            }, { _, _ -> error("Sender must not receive") })
            receiver.receive()
        }
        try {
            assertTrue("Receiver did not prepare: ${receiverStatus.get()}", invitationReady.await(15, TimeUnit.SECONDS))
            val original = PeerProtocol.invite(invitation.get())
            val target = if (wrongPin) original.copy(pin = "0".repeat(64)).encode() else original.encode()
            instrumentation.runOnMainSync { sender.send(target, "peer-tls-fixture.txt", fixture) }
            assertTrue("Sender did not finish: ${senderStatus.get()} / ${receiverStatus.get()}", senderDone.await(25, TimeUnit.SECONDS))
            return senderStatus.get() to wasSaved.get()
        } finally {
            instrumentation.runOnMainSync { sender.close(); receiver.close() }
        }
    }
    @Test fun pinnedTlsAndApprovedFileAreSaved() {
        val (status, saved) = transfer(approve = true, wrongPin = false)
        assertEquals("Entrega confirmada por el receptor", status)
        assertTrue(saved)
    }
    @Test fun wrongCertificatePinCannotSaveFile() {
        val (status, saved) = transfer(approve = true, wrongPin = true)
        assertTrue(status, status.startsWith("Envío no completado:"))
        assertFalse(saved)
    }
    @Test fun receiverRejectionCannotSaveFile() {
        val (status, saved) = transfer(approve = false, wrongPin = false)
        assertTrue(status, status.startsWith("Envío no completado:"))
        assertFalse(saved)
    }
    @Test fun concurrentEvidenceWritersPreserveBothEntries() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = System.nanoTime()
        val names = listOf("concurrency-$suffix-a.txt", "concurrency-$suffix-b.txt")
        val threads = names.map { name -> Thread { EvidenceStore(context).saveBytes(name, name.toByteArray()) }.apply { start() } }
        threads.forEach { it.join(10000); assertFalse(it.isAlive) }
        val entries = EvidenceStore(context).list().filter { it.name in names }
        assertEquals(2, entries.size)
        assertEquals("2 copias verificadas con SHA-256", EvidenceStore(context).verify(entries))
    }
}
