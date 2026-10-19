package com.msarobotika.cyberdeck

import java.net.*
import java.time.Instant
import java.time.Duration
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.concurrent.*
import javax.net.ssl.HttpsURLConnection

data class DiagnosticResult(
    val kind: String, val target: String, val checkedAt: String,
    val summary: String, val details: List<Pair<String, String>>
)

/** Single-target diagnostics. No background scans and no redirect following. */
object Diagnostics {
    private val resolver = ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue<Runnable>(4), { task -> Thread(task, "cyberdeck-dns").apply { isDaemon = true } },
        ThreadPoolExecutor.AbortPolicy())

    fun host(raw: String): String {
        val value = raw.trim().removeSurrounding("[", "]")
        require(value.isNotEmpty() && value.length <= 253 && value.none { it.isWhitespace() || it in "/?#@\\\\%" }) {
            "Introduce un dominio o una IP, sin URL ni credenciales"
        }
        if (':' in value) {
            require(value.all { it in "0123456789abcdefABCDEF:." }) { "IPv6 no válida" }
            require(InetAddress.getByName(value) is Inet6Address) { "IPv6 no válida" }
            return value
        }
        val ascii = IDN.toASCII(value, IDN.USE_STD3_ASCII_RULES).lowercase()
        require(ascii.trimEnd('.').split('.').all { it.isNotEmpty() && it.length <= 63 }) { "Dominio no válido" }
        return ascii
    }

    fun url(raw: String): URL {
        val uri = URI(raw.trim())
        require(uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank()
            && uri.rawUserInfo == null && (uri.port == -1 || uri.port in 1..65535)) {
            "Introduce una URL http:// o https:// sin usuario ni contraseña"
        }
        return URI(uri.toString().substringBefore('#')).toURL()
    }

    internal fun addresses(host: String): List<InetAddress> {
        val pending = resolver.submit<List<InetAddress>> { InetAddress.getAllByName(host).toList() }
        try { return pending.get(5, TimeUnit.SECONDS) }
        finally { pending.cancel(true) }
    }

    fun dns(rawHost: String): DiagnosticResult {
        val name = host(rawHost)
        val started = System.nanoTime()
        val found = addresses(name)
        return DiagnosticResult("dns", name, Instant.now().toString(), "${found.size} direcciones resueltas",
            found.map { (if (it is Inet6Address) "AAAA / IPv6" else "A / IPv4") to (it.hostAddress ?: "") } +
                listOf("Tiempo" to "${(System.nanoTime() - started) / 1_000_000} ms",
                    "Método" to "Resolución del sistema; puede usar caché. No comprueba DNSSEC ni otros tipos de registro."))
    }

    fun tcp(rawHost: String, port: Int): DiagnosticResult {
        require(port in 1..65535) { "El puerto debe estar entre 1 y 65535" }
        val name = host(rawHost)
        val found = addresses(name).take(4)
        val details = found.map { address ->
            val start = System.nanoTime()
            val result = try {
                Socket().use { it.connect(InetSocketAddress(address, port), 2000) }
                "Conexión TCP aceptada (${(System.nanoTime() - start) / 1_000_000} ms)"
            } catch (_: SocketTimeoutException) { "Sin respuesta en 2 s; no determina la causa" }
            catch (_: ConnectException) { "Conexión rechazada o no establecida" }
            catch (e: Exception) { "Error de conexión: ${e.javaClass.simpleName}" }
            (address.hostAddress ?: name) to result
        }
        return DiagnosticResult("tcp", "$name:$port", Instant.now().toString(),
            "Comprobación de un puerto TCP", details + ("Alcance" to "Hasta 4 direcciones. No envía datos de aplicación ni identifica servicios."))
    }

    fun web(rawUrl: String): DiagnosticResult {
        val target = url(rawUrl)
        val connection = target.openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        connection.connectTimeout = 4000
        connection.readTimeout = 4000
        connection.requestMethod = "HEAD"
        connection.useCaches = false
        connection.setRequestProperty("User-Agent", "MSA-CyberDeck/0.2")
        val details = mutableListOf<Pair<String, String>>()
        try {
            val status = connection.responseCode
            details += "HTTP" to "$status · método HEAD"
            if (status == 405) details += "Método" to "El servidor no permite HEAD; no se ha solicitado el cuerpo."
            if (connection is HttpsURLConnection) {
                val certificate = connection.serverCertificates.firstOrNull() as? X509Certificate
                details += "TLS" to "Cadena y nombre aceptados por la configuración TLS del sistema."
                details += "Cifrado negociado" to connection.cipherSuite
                if (certificate != null) {
                    details += "Titular" to certificate.subjectX500Principal.name.take(2000)
                    details += "Emisor" to certificate.issuerX500Principal.name.take(2000)
                    details += "Caducidad" to certificate.notAfter.toInstant().toString()
                    details += "Días hasta caducar" to Duration.between(Instant.now(), certificate.notAfter.toInstant()).toDays().toString()
                    details += "SHA-256 del certificado" to MessageDigest.getInstance("SHA-256").digest(certificate.encoded).joinToString("") { "%02x".format(it) }
                }
            } else details += "Transporte" to "HTTP sin cifrado TLS."
            val location = connection.getHeaderField("Location")
            if (location != null) {
                val resolved = runCatching { target.toURI().resolve(location).toString() }.getOrElse { location }
                details += "Redirección anunciada" to resolved.take(2048)
                details += "Redirecciones" to "No se siguen automáticamente; revisa el destino antes de consultarlo."
            }
            val headers = listOf("Strict-Transport-Security", "Content-Security-Policy", "X-Content-Type-Options",
                "X-Frame-Options", "Referrer-Policy", "Permissions-Policy")
            headers.forEach { key ->
                val value = connection.getHeaderField(key)
                details += key to (value?.take(4096) ?: if (key == "Strict-Transport-Security" && target.protocol == "http")
                    "No evaluada sobre HTTP" else "No observada en esta respuesta HEAD")
            }
            details += "Interpretación" to "Una cabecera ausente no confirma una vulnerabilidad. TLS válido no acredita que el sitio sea seguro."
            return DiagnosticResult("web", target.toExternalForm(), Instant.now().toString(), "Revisión HTTP/TLS completada", details)
        } finally { connection.disconnect() }
    }
}
