package com.msarobotika.cyberdeck

import org.junit.Assert.*
import org.junit.Test
import java.net.*
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.*
import java.security.KeyStore

class DiagnosticsTest {
    private class HttpFixture(private val response: String) : AutoCloseable {
        private val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val port get() = server.localPort
        val request = AtomicReference("")
        private val thread = Thread {
            runCatching {
                server.accept().use { socket ->
                    socket.soTimeout = 3000
                    val input = socket.getInputStream().bufferedReader()
                    request.set(input.readLine())
                    while (!input.readLine().isNullOrEmpty()) {}
                    socket.getOutputStream().write(response.toByteArray(Charsets.US_ASCII))
                    socket.getOutputStream().flush()
                }
            }
        }.apply { isDaemon = true; start() }
        override fun close() { server.close(); thread.join(2000) }
    }

    @Test fun rejectsCredentialsAndNonHttpSchemes() {
        listOf("file:///etc/passwd", "ftp://example.org/a", "https://user:secret@example.org/", "http://example.org:0/").forEach { target ->
            assertThrows(IllegalArgumentException::class.java) { Diagnostics.url(target) }
        }
    }
    @Test fun preservesEscapedQueryAndRemovesFragment() {
        assertEquals("https://example.org/a%2Fb?value=x%26y",
            Diagnostics.url("https://example.org/a%2Fb?value=x%26y#local").toString())
    }
    @Test fun hostInputHasNoImplicitUrlOrCredentials() {
        listOf("https://example.org", "user@example.org", "host name", "127.0.0.1/24").forEach { value ->
            assertThrows(IllegalArgumentException::class.java) { Diagnostics.host(value) }
        }
        assertEquals("xn--bcher-kva.example", Diagnostics.host("bücher.example"))
    }
    @Test fun resolvesLocalAddressAndRejectsInvalidPort() {
        assertTrue(Diagnostics.dns("127.0.0.1").details.any { it.second == "127.0.0.1" })
        assertThrows(IllegalArgumentException::class.java) { Diagnostics.tcp("127.0.0.1", 65536) }
    }
    @Test fun tcpConnectDoesNotSendApplicationPayload() {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            val result = Diagnostics.tcp("127.0.0.1", server.localPort)
            assertTrue(result.details.first().second.startsWith("Conexión TCP aceptada"))
            server.soTimeout = 2000
            server.accept().use { connection ->
                connection.soTimeout = 2000
                assertEquals(-1, connection.getInputStream().read())
            }
        }
    }
    @Test fun headReadsHeadersWithoutFollowingRedirect() {
        HttpFixture("HTTP/1.1 302 Found\r\nLocation: http://127.0.0.1:1/not-followed\r\nX-Frame-Options: DENY\r\nContent-Length: 0\r\nConnection: close\r\n\r\n").use { site ->
            val result = Diagnostics.web("http://127.0.0.1:${site.port}/")
            assertTrue(site.request.get().startsWith("HEAD / "))
            assertTrue(result.details.any { it.first == "HTTP" && it.second.startsWith("302") })
            assertEquals("DENY", result.details.first { it.first == "X-Frame-Options" }.second)
            assertTrue(result.details.any { it.first == "Redirección anunciada" && it.second.contains("not-followed") })
            assertTrue(result.details.any { it.first == "Content-Security-Policy" && it.second.startsWith("No observada") })
        }
    }
    @Test fun methodNotAllowedIsReportedWithoutGetFallback() {
        HttpFixture("HTTP/1.1 405 Method Not Allowed\r\nContent-Length: 0\r\nConnection: close\r\n\r\n").use { site ->
            val result = Diagnostics.web("http://127.0.0.1:${site.port}/")
            assertTrue(result.details.any { it.second.startsWith("405") })
            assertTrue(site.request.get().startsWith("HEAD "))
        }
    }
    @Test fun rejectsSelfSignedTlsWithoutInstallingTrustOverride() {
        val keys = KeyStore.getInstance("PKCS12")
        javaClass.getResourceAsStream("/local-selfsigned.p12").use {
            requireNotNull(it) { "Missing test TLS fixture" }
            keys.load(it, "local-test-only".toCharArray())
        }
        val managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        managers.init(keys, "local-test-only".toCharArray())
        val context = SSLContext.getInstance("TLS")
        context.init(managers.keyManagers, null, null)
        val server = context.serverSocketFactory.createServerSocket(0, 1,
            InetAddress.getByName("127.0.0.1")) as SSLServerSocket
        val thread = Thread {
            runCatching { server.accept().use { (it as SSLSocket).apply { soTimeout = 3000 }.startHandshake() } }
        }.apply { isDaemon = true; start() }
        try {
            assertThrows(SSLException::class.java) { Diagnostics.web("https://127.0.0.1:${server.localPort}/") }
        } finally { server.close(); thread.join(4000) }
    }
    @Test fun serializesResultsAsStructuredEvidence() {
        val input = DiagnosticResult("dns", "127.0.0.1", "2026-10-17T00:00:00Z",
            "Observación", listOf("A" to "127.0.0.1"))
        val json = org.json.JSONObject(input.toEvidenceJson())
        assertEquals("msa.cyberdeck.diagnostic.v1", json.getString("format"))
        assertEquals("127.0.0.1", json.getJSONArray("details").getJSONObject(0).getString("value"))
    }

    @Test fun readsCertificateFromExplicitlyTrustedLocalTlsFixture() {
        val keys = KeyStore.getInstance("PKCS12")
        javaClass.getResourceAsStream("/local-selfsigned.p12").use { keys.load(it, "local-test-only".toCharArray()) }
        val km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        km.init(keys, "local-test-only".toCharArray())
        val serverContext = SSLContext.getInstance("TLS").apply { init(km.keyManagers, null, null) }
        val server = serverContext.serverSocketFactory.createServerSocket(0, 1,
            InetAddress.getByName("127.0.0.1")) as SSLServerSocket
        val thread = Thread {
            runCatching { server.accept().use { socket ->
                socket.soTimeout = 3000
                val reader = socket.getInputStream().bufferedReader()
                while (!reader.readLine().isNullOrEmpty()) {}
                socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                socket.getOutputStream().flush()
            } }
        }.apply { isDaemon = true; start() }
        // Pin only this fixture in this test JVM; never disable certificate or host checks.
        val trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(keys) }
        val clientContext = SSLContext.getInstance("TLS").apply { init(null, trust.trustManagers, null) }
        val original = HttpsURLConnection.getDefaultSSLSocketFactory()
        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(clientContext.socketFactory)
            val result = Diagnostics.web("https://127.0.0.1:${server.localPort}/")
            assertTrue(result.details.any { it.first == "Titular" && it.second.contains("CyberDeck Local Test Only") })
            assertTrue(result.details.first { it.first == "SHA-256 del certificado" }.second.matches(Regex("[a-f0-9]{64}")))
        } finally {
            HttpsURLConnection.setDefaultSSLSocketFactory(original)
            server.close(); thread.join(4000)
        }
    }
}
