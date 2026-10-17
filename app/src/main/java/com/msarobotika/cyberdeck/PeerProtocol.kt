package com.msarobotika.cyberdeck

import org.json.JSONObject
import java.io.*
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.Base64

data class PeerInvite(val host: String, val port: Int, val pin: String, val token: String, val expires: Long) {
    fun encode(): String = "msacyberdeck://session/" + Base64.getUrlEncoder().withoutPadding().encodeToString(
        JSONObject().put("v", 1).put("host", host).put("port", port).put("pin", pin).put("token", token).put("expires", expires)
            .toString().toByteArray(Charsets.UTF_8))
}
data class PeerOffer(val name: String, val size: Int, val sha256: String)

object PeerProtocol {
    const val MAX_FILE = 16 * 1024 * 1024
    const val MAX_FRAME = 4096
    fun sha256(data: ByteArray) = MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
    fun same(a: String, b: String) = MessageDigest.isEqual(a.toByteArray(Charsets.US_ASCII), b.toByteArray(Charsets.US_ASCII))
    fun invite(raw: String, now: Long = System.currentTimeMillis() / 1000): PeerInvite {
        val text = raw.trim()
        require(text.length <= 4096 && text.startsWith("msacyberdeck://session/")) { "Invitación no válida" }
        val json = JSONObject(Base64.getUrlDecoder().decode(text.substringAfter("msacyberdeck://session/")).toString(Charsets.UTF_8))
        require(json.getInt("v") == 1) { "Versión de sesión no compatible" }
        val host = json.getString("host")
        require(host.split('.').size == 4 && host.split('.').all { p -> p.matches(Regex("[0-9]{1,3}")) && p.toInt() in 0..255 }) { "Se requiere una IPv4 local literal" }
        val address = InetAddress.getByName(host)
        require(address.isSiteLocalAddress || address.isLinkLocalAddress || address.isLoopbackAddress) { "La invitación debe apuntar a una red local" }
        val port = json.getInt("port"); require(port in 1..65535) { "Puerto inválido" }
        val pin = json.getString("pin").lowercase(); val token = json.getString("token").lowercase()
        require(pin.matches(Regex("[a-f0-9]{64}")) && token.matches(Regex("[a-f0-9]{64}"))) { "Credenciales de sesión inválidas" }
        val expires = json.getLong("expires")
        require(expires > now && expires <= now + 600) { "Invitación caducada o reloj del móvil desajustado" }
        return PeerInvite(host, port, pin, token, expires)
    }
    fun offer(json: JSONObject): PeerOffer {
        val name = json.getString("name")
        require(name.isNotBlank() && name.length <= 160 && name.none { it.isISOControl() || it in "/\\" }) { "Nombre de archivo inválido" }
        val size = json.getLong("size"); require(size in 0..MAX_FILE.toLong()) { "Límite de archivo: 16 MiB" }
        val sha = json.getString("sha256").lowercase(); require(sha.matches(Regex("[a-f0-9]{64}"))) { "Hash inválido" }
        return PeerOffer(name, size.toInt(), sha)
    }
    fun safeName(raw: String) = raw.substringAfterLast('/').substringAfterLast('\\').filterNot { it.isISOControl() }.take(160).ifBlank { "archivo.bin" }
    fun write(output: OutputStream, value: JSONObject) {
        val bytes = value.toString().toByteArray(Charsets.UTF_8)
        require(bytes.size in 1..MAX_FRAME) { "Metadatos demasiado grandes" }
        DataOutputStream(output).apply { writeInt(bytes.size); write(bytes); flush() }
    }
    fun read(input: InputStream): JSONObject {
        val reader = DataInputStream(input)
        val length = reader.readInt(); require(length in 1..MAX_FRAME) { "Marco de sesión inválido" }
        val bytes = ByteArray(length); reader.readFully(bytes)
        val decoded = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        return JSONObject(decoded)
    }
    fun readSelectedFile(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16384)
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            require(output.size() + read <= MAX_FILE) { "Límite: 16 MiB por archivo" }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
    fun readFile(input: InputStream, offer: PeerOffer): ByteArray {
        require(offer.size in 0..MAX_FILE) { "Archivo demasiado grande" }
        val bytes = ByteArray(offer.size)
        DataInputStream(input).readFully(bytes)
        require(same(sha256(bytes), offer.sha256)) { "El SHA-256 recibido no coincide; archivo descartado" }
        return bytes
    }
}
