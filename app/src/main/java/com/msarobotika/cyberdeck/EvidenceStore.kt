package com.msarobotika.cyberdeck

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.UUID

data class Evidence(val id: String, val name: String, val sha256: String, val bytes: Long, val importedAt: String)

class EvidenceStore(private val context: Context) {
    private companion object { val STORE_LOCK = Any() }
    private val directory = File(context.filesDir, "evidence").apply { mkdirs() }
    private val index = AtomicFile(File(directory, "index.json"))

    fun list(): List<Evidence> = synchronized(STORE_LOCK) {
        if (!index.baseFile.exists()) return@synchronized emptyList()
        val array = JSONArray(index.openRead().bufferedReader().use { it.readText() })
        val entries = (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Evidence(o.getString("id"), o.getString("name"), o.getString("sha256"), o.getLong("bytes"), o.getString("importedAt"))
        }
        directory.listFiles()?.filter { it.name.startsWith(".deleted-") }?.forEach { staged ->
            val id = staged.name.removePrefix(".deleted-")
            if (runCatching { UUID.fromString(id) }.isSuccess) {
                if (entries.any { it.id == id }) {
                    val original = File(directory, id)
                    if (!original.exists()) check(staged.renameTo(original)) { "No se pudo recuperar un borrado interrumpido" }
                } else check(staged.delete()) { "No se pudo completar un borrado interrumpido" }
            }
        }
        entries
    }

    fun import(uri: Uri): List<Evidence> {
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "evidencia"
        return context.contentResolver.openInputStream(uri)?.use { persist(name, it) }
            ?: error("No se pudo leer el archivo")
    }

    fun saveBytes(name: String, bytes: ByteArray): List<Evidence> =
        ByteArrayInputStream(bytes).use { persist(name, it) }

    /** Only removes app-private copies. The originally selected file is never modified. */
    fun delete(id: String): List<Evidence> = synchronized(STORE_LOCK) {
        val before = list()
        val entry = before.firstOrNull { it.id == id } ?: error("La evidencia ya no está en el índice")
        val source = File(directory, entry.id)
        val staged = File(directory, ".deleted-${entry.id}")
        val hadFile = source.exists()
        if (hadFile) check(source.renameTo(staged)) { "No se pudo preparar el borrado" }
        val remaining = before.filterNot { it.id == id }
        try {
            writeIndex(remaining)
        } catch (error: Exception) {
            if (hadFile) check(staged.renameTo(source)) { "No se pudo restaurar la copia; permanece en ${staged.name}" }
            throw error
        }
        if (hadFile && !staged.delete()) {
            check(staged.renameTo(source)) { "No se pudo eliminar la copia; permanece en ${staged.name}" }
            writeIndex(before)
            error("No se pudo liberar el archivo; se conserva la evidencia")
        }
        remaining
    }

    private fun writeIndex(entries: List<Evidence>) {
        val stream = index.startWrite()
        try { stream.write(json(entries).toString(2).toByteArray(Charsets.UTF_8)); index.finishWrite(stream) }
        catch (error: Exception) { index.failWrite(stream); throw error }
    }

    fun diagnostic(entry: Evidence): DiagnosticResult = synchronized(STORE_LOCK) {
        require(list().any { it == entry }) { "La evidencia no pertenece al índice local" }
        require(entry.name.startsWith("diagnostico-") && entry.bytes <= 65536) { "No es un diagnóstico compatible" }
        val source = File(directory, entry.id)
        require(source.length() == entry.bytes) { "Tamaño de evidencia modificado" }
        val raw = source.readBytes()
        require(PeerProtocol.sha256(raw).equals(entry.sha256, ignoreCase = true)) { "SHA-256 no coincide: no se abre la evidencia" }
        diagnosticFromJson(raw.toString(Charsets.UTF_8))
    }

    fun verifiedFile(entry: Evidence): File = synchronized(STORE_LOCK) {
        require(list().any { it == entry }) { "La evidencia ya no está en el índice" }
        val file = File(directory, entry.id)
        require(file.isFile && file.length() == entry.bytes) { "La copia no está disponible o su tamaño ha cambiado" }
        val hash = file.inputStream().use { EvidenceHash.copyAndHash(it, OutputStreamSink).first }
        require(hash.equals(entry.sha256, ignoreCase = true)) { "SHA-256 no coincide: no se abre ni exporta la copia" }
        file
    }

    private fun persist(name: String, input: InputStream): List<Evidence> = synchronized(STORE_LOCK) {
        val id = UUID.randomUUID().toString()
        val file = File(directory, id)
        try {
            val result = file.outputStream().use { output -> EvidenceHash.copyAndHash(input, output) }
            val entries = list() + Evidence(id, name, result.first, result.second, Instant.now().toString())
            val stream = index.startWrite()
            try {
                stream.write(json(entries).toString(2).toByteArray(Charsets.UTF_8))
                index.finishWrite(stream)
            } catch (e: Exception) { index.failWrite(stream); throw e }
            entries
        } catch (e: Exception) { file.delete(); throw e }
    }

    private fun json(entries: List<Evidence>) = JSONArray().apply {
        entries.forEach { e -> put(JSONObject().put("id", e.id).put("name", e.name).put("sha256", e.sha256)
            .put("bytes", e.bytes).put("importedAt", e.importedAt)) }
    }

    fun verify(entries: List<Evidence>): String {
        val changed = entries.filter { e ->
            val file = File(directory, e.id)
            !file.exists() || file.inputStream().use { EvidenceHash.copyAndHash(it, OutputStreamSink).first } != e.sha256
        }
        return if (changed.isEmpty()) "${entries.size} copias verificadas con SHA-256" else "ATENCIÓN: ${changed.size} copias ausentes o modificadas"
    }

    fun report(entries: List<Evidence>): String = buildString {
        append("# MSA CyberDeck — Informe local\n\nGenerado: ${Instant.now()}\n\n")
        append("SHA-256 permite comparar la integridad de la copia importada. La fecha procede del dispositivo; no constituye un sellado de tiempo certificado.\n\n")
        entries.forEach { e ->
            append("## Evidencia ${e.id}\n\nNombre: ${e.name.replace('\n', ' ').replace('\r', ' ')}\n\n")
            append("Importada: ${e.importedAt}\n\nBytes: ${e.bytes}\n\nSHA-256: ${e.sha256}\n\n")
            val stored = File(directory, e.id)
            if (e.name.startsWith("diagnostico-") && stored.exists() && stored.length() <= 65536) {
                val content = stored.readText(Charsets.UTF_8)
                val diagnostic = runCatching { JSONObject(content) }.getOrNull()
                if (diagnostic?.optString("format") == "msa.cyberdeck.diagnostic.v1") {
                    append("Resultado de diagnóstico (contenido guardado):\n\n```json\n")
                    append(content.replace("```", "\\u0060\\u0060\\u0060"))
                    append("\n```\n\n")
                }
            }
        }
    }

    private object OutputStreamSink : java.io.OutputStream() {
        override fun write(b: Int) {}
        override fun write(b: ByteArray, off: Int, len: Int) {}
    }
}

