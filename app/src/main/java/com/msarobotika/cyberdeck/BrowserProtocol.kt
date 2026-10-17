package com.msarobotika.cyberdeck

import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.net.URLDecoder

data class BrowserRequest(val method: String, val path: String, val headers: Map<String, String>)

object BrowserProtocol {
    const val MAX_BYTES = 16 * 1024 * 1024
    fun request(input: InputStream): BrowserRequest {
        val buffer = ByteArrayOutputStream()
        var suffix = 0
        while (suffix != 0x0d0a0d0a) {
            require(buffer.size() < 8192) { "Cabeceras demasiado grandes" }
            val byte = input.read()
            require(byte >= 0) { "Petición incompleta" }
            require(byte in 0x20..0x7e || byte == 13 || byte == 10) { "Cabecera no válida" }
            buffer.write(byte); suffix = (suffix shl 8) or byte
        }
        val lines = buffer.toString("US-ASCII").split("\r\n")
        val first = lines.first().split(' ')
        require(first.size == 3 && first[2] == "HTTP/1.1" && first[0] in listOf("GET", "POST")) { "Petición no compatible" }
        val headers = linkedMapOf<String, String>()
        lines.drop(1).filter { it.isNotEmpty() }.forEach { line ->
            val colon = line.indexOf(':')
            require(colon > 0 && line[0] != ' ') { "Cabecera no válida" }
            val key = line.substring(0, colon).lowercase()
            require(key.all { it.isLetterOrDigit() || it == '-' } && key !in headers) { "Cabecera repetida o no válida" }
            headers[key] = line.substring(colon + 1).trim()
        }
        require("transfer-encoding" !in headers) { "Transfer-Encoding no admitido" }
        return BrowserRequest(first[0], first[1], headers)
    }
    fun upload(request: BrowserRequest, host: String, route: String): Pair<String, Int> {
        require(request.method == "POST" && request.path == "$route/upload") { "Ruta no autorizada" }
        require(request.headers["host"] == host && request.headers["origin"] == "http://$host") { "Origen no autorizado" }
        require(request.headers["x-cyberdeck-upload"] == "selected-file") { "Acción de envío no válida" }
        require(request.headers["content-type"] == "application/octet-stream") { "Tipo de envío no válido" }
        val length = request.headers["content-length"]?.toLongOrNull() ?: error("Falta tamaño")
        require(length in 0..MAX_BYTES.toLong()) { "El archivo supera 16 MiB" }
        val name = URLDecoder.decode(request.headers["x-file-name"] ?: error("Falta nombre"), "UTF-8")
        require(name.isNotBlank() && name.length <= 200) { "Nombre no válido" }
        return PeerProtocol.safeName(name) to length.toInt()
    }
    val page = """<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Enviar a MSA CyberDeck</title><style>body{background:#071018;color:#eaf5fa;font:18px system-ui;margin:24px;max-width:650px}h1{color:#5de7ff}button,input{font:inherit;padding:16px;margin:12px 0;width:100%;box-sizing:border-box}button{background:#5de7ff;border:0;border-radius:12px}aside{border-left:4px solid #ffbc58;padding:12px}#status{white-space:pre-wrap;overflow-wrap:anywhere}</style>
<h1>MSA CyberDeck</h1><h2>Enviar al móvil receptor</h2><aside>Sesión local HTTP sin cifrar. Usa una red propia de confianza. Para archivos confidenciales utiliza la sesión TLS entre dos CyberDeck.</aside>
<p>Solo se envía el archivo que selecciones. El receptor confirma antes de guardarlo. Máximo 16 MiB; un archivo por sesión.</p>
<input id="file" type="file" aria-label="Elegir archivo"><button id="send">Proponer archivo al receptor</button><p id="status" role="status">Elige un archivo en este móvil.</p>
<script>const file=document.getElementById('file'),button=document.getElementById('send'),status=document.getElementById('status');button.onclick=async()=>{const selected=file.files[0];if(!selected){status.textContent='Elige un archivo primero.';return}if(selected.size>16777216){status.textContent='El archivo supera 16 MiB.';return}button.disabled=true;status.textContent='Esperando aprobación en el receptor. Mantén ambos móviles abiertos.';try{const response=await fetch(location.pathname+'/upload',{method:'POST',credentials:'omit',headers:{'Content-Type':'application/octet-stream','X-CyberDeck-Upload':'selected-file','X-File-Name':encodeURIComponent(selected.name)},body:selected});status.textContent=await response.text();}catch(error){status.textContent='Sesión cerrada o red no disponible. Comprueba el resultado en el receptor.'}finally{button.disabled=false}};</script></html>"""
}
