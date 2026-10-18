package com.msarobotika.cyberdeck

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

interface SensorTransport {
    fun write(bytes: ByteArray)
    fun read(buffer: ByteArray, timeoutMs: Int): Int
}

data class SensorSnapshot(val firmware: String, val mac: String, val running: Boolean,
    val channel: Int, val frames: Long, val deauth: Long, val disassoc: Long, val mismatches: Long,
    val networks: List<AccessPoint>)
data class AccessPoint(val id: Int, val ssid: String, val bssid: String,
    val channel: Int, val rssi: Int, val security: String)

/** One caller at a time; commands are bounded and never flash or write NVS. */
class SentinelProtocol(private val transport: SensorTransport) {
    private val pending = ArrayDeque<Int>()
    private val readBuffer = ByteArray(4096)

    private fun nextJson(deadline: Long): JSONObject {
        val line = ByteArrayOutputStream()
        var discard = false
        while (System.nanoTime() < deadline) {
            if (pending.isEmpty()) {
                val n = transport.read(readBuffer, 200)
                if (n > 0) for (i in 0 until n) pending.addLast(readBuffer[i].toInt() and 255)
                continue
            }
            val b = pending.removeFirst()
            if (b == 10) {
                if (!discard && line.size() > 0) {
                    val parsed = runCatching { JSONObject(line.toString("UTF-8")) }.getOrNull()
                    if (parsed != null) return parsed
                }
                line.reset()
                discard = false
            } else if (!discard) {
                if (line.size() >= 16384) { discard = true; line.reset() } else line.write(b)
            }
        }
        throw IOException("Sin respuesta compatible del sensor (tiempo agotado)")
    }

    private fun command(name: String) {
        transport.write((JSONObject().put("cmd", name).toString() + "\n").toByteArray(Charsets.UTF_8))
    }

    fun snapshot(): SensorSnapshot {
        command("status")
        val statusDeadline = System.nanoTime() + 8_000_000_000L
        var status: JSONObject
        do { status = nextJson(statusDeadline) } while (status.optString("result") != "status")
        require(status.optString("firmware").startsWith("MSA-WiFi-Sentinel-")) {
            "Firmware incompatible: se necesita WiFi Sentinel. No se ha modificado la placa."
        }
        require(status.optBoolean("passive") && !status.optBoolean("injection_enabled", true)) {
            "El sensor no anuncia el modo pasivo esperado"
        }
        command("aps")
        val deadline = System.nanoTime() + 10_000_000_000L
        require(nextJson(deadline).optString("result") == "aps_begin") { "Inicio de exportación inválido" }
        val aps = mutableListOf<AccessPoint>()
        while (true) {
            val record = nextJson(deadline)
            if (record.optString("result") == "aps_end") break
            require(record.optString("record") == "ap" && aps.size < 32) { "Exportación de redes inválida o excesiva" }
            aps.add(AccessPoint(record.getInt("id"), record.getString("ssid"), record.getString("bssid"),
                record.getInt("channel"), record.getInt("rssi"), record.getString("security")))
        }
        return SensorSnapshot(status.getString("firmware"), status.getString("mac"), status.getBoolean("running"),
            status.getInt("channel"), status.getLong("frames"), status.getLong("deauth"),
            status.getLong("disassoc"), status.getLong("security_mismatches"), aps)
    }
}
