package com.msarobotika.cyberdeck

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import org.json.JSONObject

class SentinelProtocolTest {
    private val status = """{"result":"status","firmware":"MSA-WiFi-Sentinel-0.1","mac":"00:11:22:33:44:55","passive":true,"injection_enabled":false,"running":true,"channel":6,"frames":123,"deauth":2,"disassoc":1,"security_mismatches":0}"""
    private val ap = """{"record":"ap","id":0,"ssid":"Lab autorizado","bssid":"00:11:22:33:44:66","channel":6,"rssi":-48,"security":"WPA2"}"""
    private val begin = """{"result":"aps_begin"}"""
    private val end = """{"result":"aps_end"}"""

    private class Fake(text: String) : SensorTransport {
        val writes = mutableListOf<String>()
        private val bytes = text.toByteArray()
        private var position = 0
        override fun write(bytes: ByteArray) { writes.add(JSONObject(bytes.toString(Charsets.UTF_8)).getString("cmd")) }
        override fun read(buffer: ByteArray, timeoutMs: Int): Int {
            val count = minOf(7, bytes.size - position)
            if (count == 0) return 0
            bytes.copyInto(buffer, 0, position, position + count)
            position += count
            return count
        }
    }
    private fun stream(vararg lines: String) = lines.joinToString("\n", postfix = "\n")

    @Test fun fragmentedResponsesIgnoreBootNoiseAndReadOnlyCommands() {
        val io = Fake(stream("boot noise", """{"result":"ready"}""", status, begin, ap, end))
        val result = SentinelProtocol(io).snapshot()
        assertEquals(123L, result.frames)
        assertEquals("Lab autorizado", result.networks.single().ssid)
        assertEquals(listOf("status", "aps"), io.writes)
    }
    @Test fun acceptsEmptyNetworkList() {
        assertTrue(SentinelProtocol(Fake(stream(status, begin, end))).snapshot().networks.isEmpty())
    }
    @Test fun rejectsAnotherFirmwareBeforeRequestingNetworks() {
        val io = Fake(stream(status.replace("MSA-WiFi-Sentinel-0.1", "MSA-Other-0.1")))
        assertThrows(IllegalArgumentException::class.java) { SentinelProtocol(io).snapshot() }
        assertEquals(listOf("status"), io.writes)
    }
    @Test fun rejectsNonPassiveSensor() {
        assertThrows(IllegalArgumentException::class.java) {
            SentinelProtocol(Fake(stream(status.replace("\"passive\":true", "\"passive\":false")))).snapshot()
        }
    }
    @Test fun rejectsExcessiveExport() {
        val text = stream(status, begin) + List(33) { ap + "\n" }.joinToString("")
        assertThrows(IllegalArgumentException::class.java) { SentinelProtocol(Fake(text)).snapshot() }
    }
    @Test fun oversizedNoiseLineDoesNotPoisonNextFrame() {
        val text = stream("x".repeat(17000), status, begin, end)
        assertEquals(6, SentinelProtocol(Fake(text)).snapshot().channel)
    }
    @Test fun rejectsMalformedExportMarker() {
        assertThrows(IllegalArgumentException::class.java) {
            SentinelProtocol(Fake(stream(status, """{"result":"unsupported_command"}"""))).snapshot()
        }
    }
}
