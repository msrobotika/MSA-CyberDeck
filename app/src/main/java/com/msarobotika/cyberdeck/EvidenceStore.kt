package com.msarobotika.cyberdeck

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID

data class Evidence(val id: String, val name: String, val sha256: String, val bytes: Long, val importedAt: String)

class EvidenceStore(private val context: Context) {
    private val directory = File(context.filesDir, "evidence").apply { mkdirs() }
    private val index = AtomicFile(File(directory, "index.json"))

    fun list(): List<Evidence> {
        if (!index.baseFile.exists()) return emptyList()
        val array = JSONArray(index.openRead().bufferedReader().use { it.readText() })
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Evidence(o.getString("id"), o.getString("name"), o.getString("sha256"), o.getLong("bytes"), o.getString("importedAt"))
        }
    }

    fun import(uri: Uri): List<Evidence> {
        val id = UUID.randomUUID().toString()
        val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: "evidencia"
        val file = File(directory, id)
        try {
            val result = context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> EvidenceHash.copyAndHash(input, output) }
            } ?: error("No se pudo leer el archivo")
            val entries = list() + Evidence(id, name, result.first, result.second, Instant.now().toString())
            val stream = index.startWrite()
            try {
                stream.write(json(entries).toString(2).toByteArray(Charsets.UTF_8))
                index.finishWrite(stream)
            } catch (e: Exception) { index.failWrite(stream); throw e }
            return entries
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
        }
    }

    private object OutputStreamSink : java.io.OutputStream() {
        override fun write(b: Int) {}
        override fun write(b: ByteArray, off: Int, len: Int) {}
    }
}

