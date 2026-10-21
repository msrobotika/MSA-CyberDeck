package com.msarobotika.cyberdeck

import org.junit.Test
import org.junit.Assert.*
import org.json.JSONObject
import java.io.*

class PeerProtocolTest {
    private val now = 1700000000L
    private fun invitation(host: String = "192.168.1.20", expires: Long = now + 120) = PeerInvite(host, 33221, "a".repeat(64), "b".repeat(64), expires)
    private fun failure(block: () -> Unit) { try { block(); fail("Expected rejection") } catch (_: IllegalArgumentException) {} }
    @Test fun localInvitationRoundTrip() { assertEquals(invitation(), PeerProtocol.invite(invitation().encode(), now)) }
    @Test fun rejectExpiredInvitation() { failure { PeerProtocol.invite(invitation(expires = now).encode(), now) } }
    @Test fun rejectFutureInvitation() { failure { PeerProtocol.invite(invitation(expires = now + 601).encode(), now) } }
    @Test fun rejectPublicAddressAndHostnames() {
        failure { PeerProtocol.invite(invitation(host = "8.8.8.8").encode(), now) }
        failure { PeerProtocol.invite(invitation(host = "example.org").encode(), now) }
    }
    @Test fun rejectInvalidPinAndPort() {
        failure { PeerProtocol.invite(invitation().copy(pin = "bad").encode(), now) }
        failure { PeerProtocol.invite(invitation().copy(port = 0).encode(), now) }
    }
    @Test fun framedMetadataAndBinaryContentAreSeparate() {
        val bytes = byteArrayOf(0, 10, 13, -1, 42)
        val metadata = JSONObject().put("name", "fixture.bin").put("size", bytes.size).put("sha256", PeerProtocol.sha256(bytes))
        val output = ByteArrayOutputStream()
        PeerProtocol.write(output, metadata); output.write(bytes)
        val input = ByteArrayInputStream(output.toByteArray())
        val offer = PeerProtocol.offer(PeerProtocol.read(input))
        assertArrayEquals(bytes, PeerProtocol.readFile(input, offer))
        assertEquals(-1, input.read())
    }
    @Test fun rejectForgedHash() {
        failure { PeerProtocol.readFile(ByteArrayInputStream("abc".toByteArray()), PeerOffer("fixture.bin", 3, "0".repeat(64))) }
    }
    @Test fun rejectOversizedFrameBeforeAllocating() {
        val output = ByteArrayOutputStream(); DataOutputStream(output).writeInt(Int.MAX_VALUE)
        failure { PeerProtocol.read(ByteArrayInputStream(output.toByteArray())) }
    }
    @Test fun rejectOversizedOfferAndPathNames() {
        val base = JSONObject().put("name", "fixture.bin").put("size", PeerProtocol.MAX_FILE.toLong() + 1).put("sha256", "a".repeat(64))
        failure { PeerProtocol.offer(base) }
        base.put("size", 1).put("name", "../sensitive")
        failure { PeerProtocol.offer(base) }
    }
    @Test fun truncatedContentIsNotAccepted() {
        try { PeerProtocol.readFile(ByteArrayInputStream(byteArrayOf(1)), PeerOffer("fixture", 5, "a".repeat(64))); fail() }
        catch (_: EOFException) { }
    }
    @Test fun selectedFileSizeIsBounded() {
        val input = object : InputStream() {
            override fun read() = 1
            override fun read(b: ByteArray, off: Int, len: Int): Int { java.util.Arrays.fill(b, off, off + len, 1.toByte()); return len }
        }
        failure { PeerProtocol.readSelectedFile(input) }
    }
    @Test fun safeNameCannotEscapeEvidenceStorage() {
        assertEquals("fixture.bin", PeerProtocol.safeName("/folder/fixture.bin"))
        assertEquals("file.bin", PeerProtocol.safeName("folder\\file.bin"))
        assertEquals("archivo.bin", PeerProtocol.safeName("\n\r"))
    }
}
