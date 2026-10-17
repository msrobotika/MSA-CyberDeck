package com.msarobotika.cyberdeck

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Handler
import android.os.Looper
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

data class NfcSnapshot(val armed: Boolean = false, val status: String = "Lector NFC detenido",
    val result: DiagnosticResult? = null)

class NfcInspector(private val activity: Activity, private val changed: (NfcSnapshot) -> Unit) {
    private val adapter = NfcAdapter.getDefaultAdapter(activity)
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val reading = AtomicBoolean(false)
    private var state = NfcSnapshot()
    private var generation = 0
    private val deadline = Runnable { stop() }
    fun hardwareStatus(): String = when {
        adapter == null -> "Este móvil no dispone de NFC"
        !adapter.isEnabled -> "NFC está apagado; puedes activarlo en los ajustes del móvil"
        else -> "NFC disponible · lectura de etiquetas NDEF"
    }
    fun start() {
        stop()
        if (adapter?.isEnabled != true) { state = state.copy(status = hardwareStatus()); changed(state); return }
        val token = ++generation
        state = NfcSnapshot(armed = true, status = "Acerca una etiqueta propia · ventana de 30 s")
        changed(state)
        runCatching {
            adapter.enableReaderMode(activity, { tag ->
                if (reading.compareAndSet(false, true)) worker.execute {
                    val result = runCatching { inspect(tag) }.getOrElse {
                        DiagnosticResult("nfc", "Etiqueta NFC", Instant.now().toString(), "Lectura NFC no completada",
                            listOf("Error" to "${it.javaClass.simpleName}: ${it.message}"))
                    }
                    main.post {
                        reading.set(false)
                        if (state.armed && token == generation) {
                            stop(); state = state.copy(result = result, status = result.summary); changed(state)
                        }
                    }
                }
            }, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_NFC_BARCODE, null)
            main.postDelayed(deadline, 30000)
        }.onFailure { stop(); state = state.copy(status = "NFC: ${it.message}"); changed(state) }
    }
    private fun inspect(tag: Tag): DiagnosticResult {
        val details = mutableListOf("Identificador observado" to tag.id.joinToString("") { "%02X".format(it) },
            "Tecnologías" to tag.techList.joinToString { it.substringAfterLast('.') },
            "Alcance" to "Lectura de NDEF estándar. No se escriben datos, envían APDU personalizadas ni intentan autenticaciones. El identificador no acredita identidad ni autenticidad.")
        details += "Guardar informe" to "Conserva estos datos como evidencia local. No crea una tarjeta utilizable para fichar ni una credencial móvil."
        var summary = "Etiqueta NFC inspeccionada"
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            details += "NDEF" to "Android no ofrece NDEF para esta etiqueta. No se exploran áreas protegidas."
            details += "Tarjetas de acceso o fichaje" to "Pueden usar un protocolo o credenciales propias. Detectar la tarjeta no permite copiarla. Consulta si el proveedor del sistema admite credenciales móviles."
            summary = "Tarjeta detectada · sin contenido NDEF disponible"
        } else try { ndef.use { tech ->
            tech.connect()
            details += "Tipo NDEF" to tech.type
            details += "Capacidad anunciada" to "${tech.maxSize} bytes"
            details += "Escritura anunciada" to if (tech.isWritable) "Posible; no se ha escrito nada" else "No disponible"
            if (tech.maxSize > 65536) details += "NDEF" to "Capacidad superior al límite de lectura de 64 KiB"
            else {
                val message = tech.ndefMessage
                if (message == null || message.records.isEmpty()) {
                    details += "NDEF" to "No contiene registros NDEF legibles"
                    summary = "Etiqueta detectada · sin registros NDEF"
                }
                message?.let {
                val bytes = message.toByteArray()
                check(bytes.size <= 65536) { "NDEF supera el límite de 64 KiB" }
                details += "SHA-256 NDEF" to MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
                message.records.take(32).forEachIndexed { index, record ->
                    val description = when {
                        record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT) -> {
                            val p = record.payload
                            if (p.isEmpty()) "Texto vacío" else {
                                val languageBytes = p[0].toInt() and 0x3F
                                require(p.size >= 1 + languageBytes) { "Registro de texto NDEF inválido" }
                                val charset = if (p[0].toInt() and 0x80 != 0) Charsets.UTF_16 else Charsets.UTF_8
                                "Texto: " + p.copyOfRange(1 + languageBytes, p.size).toString(charset).take(2048)
                            }
                        }
                        record.toUri() != null -> "URI anunciada (no abierta): ${record.toUri().toString().take(2048)}"
                        else -> "TNF ${record.tnf} · tipo ${record.type.toString(Charsets.US_ASCII).take(100)} · ${record.payload.size} bytes · muestra HEX: " +
                            record.payload.take(64).joinToString("") { "%02X".format(it) }
                    }
                    details += "Registro ${index + 1}" to description
                }
                }
            }
        } } catch (error: Exception) {
            details += "Lectura NDEF" to "${error.javaClass.simpleName}: ${error.message}. Mantén la etiqueta cerca y repite la lectura. Se conserva la identificación observada."
            summary = "Tarjeta detectada · lectura NDEF no completada"
        }
        return DiagnosticResult("nfc", "Etiqueta NFC observada", Instant.now().toString(), summary, details)
    }
    fun stop() {
        generation++
        main.removeCallbacks(deadline)
        runCatching { adapter?.disableReaderMode(activity) }
        if (state.armed) { state = state.copy(armed = false, status = "Ventana NFC finalizada"); changed(state) }
    }
    fun close() { stop(); worker.shutdown() }
}
