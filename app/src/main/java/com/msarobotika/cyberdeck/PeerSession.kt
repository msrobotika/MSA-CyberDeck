package com.msarobotika.cyberdeck

import android.content.Context
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.*
import javax.security.auth.x500.X500Principal
import org.json.JSONObject

data class PeerState(val active: Boolean = false, val status: String = "Sin sesión iniciada",
    val invitation: String = "", val offer: PeerOffer? = null, val result: String = "")

/** Local TLS file transfer. The invitation pins one certificate and contains a one-use random token. */
class PeerSession(private val context: Context, private val changed: (PeerState) -> Unit,
    private val received: (String, ByteArray) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    @Volatile private var epoch = 0
    @Volatile private var server: SSLServerSocket? = null
    @Volatile private var socket: SSLSocket? = null
    @Volatile private var choice: CountDownLatch? = null
    private val accepted = AtomicBoolean(false)
    private var state = PeerState()
    private val sessionDeadline = Runnable { stop() }
    private fun post(token: Int, change: (PeerState) -> PeerState) { main.post {
        if (token == epoch) { state = change(state); if (!state.active) main.removeCallbacks(sessionDeadline); changed(state) }
    } }
    private fun tlsProtocols(values: Array<String>) = values.filter { it == "TLSv1.3" || it == "TLSv1.2" }.toTypedArray()
    private fun serverContext(): Pair<SSLContext, String> {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "msa-cyberdeck-local-tls-v2"
        if (!keyStore.containsAlias(alias)) {
            val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
            generator.initialize(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_NONE, KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512)
                .setCertificateSubject(X500Principal("CN=MSA CyberDeck Local Session"))
                .setCertificateSerialNumber(BigInteger(64, SecureRandom()).abs().add(BigInteger.ONE))
                .setCertificateNotBefore(Date(System.currentTimeMillis() - 86400000L))
                .setCertificateNotAfter(Date(System.currentTimeMillis() + 3650L * 86400000L)).build())
            generator.generateKeyPair()
        }
        // Conscrypt performs TLS hashing itself, so AndroidKeyStore must permit NONEwithECDSA.
        // Select exactly this key: other app keys must never alter the certificate in the invitation.
        val entry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
        val manager = object : X509ExtendedKeyManager() {
            private fun serverAlias(type: String?) = alias.takeIf { type?.startsWith("EC") == true }
            override fun getPrivateKey(requested: String?) = entry.privateKey.takeIf { requested == alias }
            override fun getCertificateChain(requested: String?) = if (requested == alias)
                entry.certificateChain.map { it as X509Certificate }.toTypedArray() else null
            override fun getServerAliases(type: String?, issuers: Array<Principal>?) = serverAlias(type)?.let { arrayOf(it) }
            override fun chooseServerAlias(type: String?, issuers: Array<Principal>?, socket: java.net.Socket?) = serverAlias(type)
            override fun chooseEngineServerAlias(type: String?, issuers: Array<Principal>?, engine: SSLEngine?) = serverAlias(type)
            override fun getClientAliases(type: String?, issuers: Array<Principal>?): Array<String>? = null
            override fun chooseClientAlias(types: Array<String>?, issuers: Array<Principal>?, socket: java.net.Socket?): String? = null
            override fun chooseEngineClientAlias(types: Array<String>?, issuers: Array<Principal>?, engine: SSLEngine?): String? = null
        }
        val tls = SSLContext.getInstance("TLS").apply { init(arrayOf(manager), null, SecureRandom()) }
        return tls to PeerProtocol.sha256(keyStore.getCertificate(alias).encoded)
    }
    private fun localAddress(): String {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val addresses = manager.activeNetwork?.let(manager::getLinkProperties)?.linkAddresses ?: emptyList()
        return addresses.map { it.address }.filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress || it.isLinkLocalAddress }?.hostAddress
            ?: error("Conecta ambos móviles a una red local con IPv4; no se encontró una dirección compatible")
    }
    fun receive() {
        stop()
        val token = ++epoch
        state = PeerState(active = true, status = "Preparando sesión TLS local"); changed(state)
        main.postDelayed(sessionDeadline, 180000)
        worker.execute {
            try {
                val host = localAddress()
                val (tls, pin) = serverContext()
                val listener = tls.serverSocketFactory.createServerSocket(0, 1, java.net.InetAddress.getByName(host)) as SSLServerSocket
                if (token != epoch) { listener.close(); return@execute }
                server = listener
                listener.enabledProtocols = tlsProtocols(listener.supportedProtocols)
                listener.soTimeout = 1000
                val secret = ByteArray(32).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
                val expires = System.currentTimeMillis() / 1000 + 120
                val invitation = PeerInvite(host, listener.localPort, pin, secret, expires).encode()
                post(token) { it.copy(status = "Recepción preparada · invitación válida 2 minutos", invitation = invitation) }
                var attempts = 0
                while (token == epoch && System.currentTimeMillis() / 1000 < expires && attempts < 8) {
                    val client = try { listener.accept() as SSLSocket } catch (_: java.net.SocketTimeoutException) { continue }
                    attempts++
                    socket = client
                    var phase = "negociación TLS"
                    try {
                        client.use { connection ->
                            connection.soTimeout = 10000
                            connection.startHandshake()
                            phase = "lectura de metadatos"
                            val header = PeerProtocol.read(connection.inputStream)
                            require(PeerProtocol.same(header.getString("token"), secret)) { "Invitación incorrecta" }
                            require(System.currentTimeMillis() / 1000 < expires) { "Invitación caducada" }
                            val offer = PeerProtocol.offer(header)
                            phase = "confirmación del receptor"
                            val latch = CountDownLatch(1); choice = latch; accepted.set(false)
                            post(token) { it.copy(status = "Solicitud recibida: revisa el archivo antes de aceptar", offer = offer) }
                            val approved = latch.await(45, TimeUnit.SECONDS) && accepted.get() && token == epoch
                            require(approved) { "Recepción rechazada o sin confirmar" }
                            phase = "recepción del archivo"
                            PeerProtocol.write(connection.outputStream, JSONObject().put("status", "ready"))
                            val bytes = PeerProtocol.readFile(connection.inputStream, offer)
                            check(token == epoch) { "Sesión cancelada" }
                            phase = "guardado de la evidencia"
                            received(offer.name, bytes)
                            PeerProtocol.write(connection.outputStream, JSONObject().put("status", "saved").put("sha256", offer.sha256))
                            post(token) { it.copy(active = false, invitation = "", offer = null,
                                status = "Archivo recibido y guardado con SHA-256", result = "${offer.name} · ${offer.size} bytes\nSHA-256 ${offer.sha256}") }
                            return@execute
                        }
                    } catch (error: Exception) {
                        post(token) { it.copy(offer = null, status = "Intento no completado durante $phase: ${error.javaClass.simpleName}: ${error.message}. La sesión sigue disponible hasta caducar.") }
                    } finally { choice = null; socket = null }
                }
                post(token) { it.copy(active = false, invitation = "", offer = null, status = "Sesión de recepción finalizada") }
            } catch (error: Exception) {
                post(token) { it.copy(active = false, invitation = "", offer = null, status = "Recepción: ${error.message}") }
            } finally { runCatching { server?.close() }; server = null }
        }
    }
    fun decide(allow: Boolean) { accepted.set(allow); choice?.countDown() }
    fun send(rawInvitation: String, name: String, bytes: ByteArray) {
        val invitation = PeerProtocol.invite(rawInvitation)
        require(bytes.size <= PeerProtocol.MAX_FILE) { "Límite: 16 MiB por archivo" }
        stop()
        val token = ++epoch
        state = PeerState(active = true, status = "Conectando con el receptor y verificando su certificado"); changed(state)
        main.postDelayed(sessionDeadline, 120000)
        worker.execute {
            var phase = "preparación TLS"
            try {
                val pinned = object : X509TrustManager {
                    override fun getAcceptedIssuers() = emptyArray<X509Certificate>()
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) { throw java.security.cert.CertificateException("No se utiliza autenticación TLS de cliente") }
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                        if (chain.isEmpty() || !PeerProtocol.same(PeerProtocol.sha256(chain[0].encoded), invitation.pin))
                            throw java.security.cert.CertificateException("El certificado no coincide con la invitación")
                        chain[0].checkValidity()
                    }
                }
                val tls = SSLContext.getInstance("TLS").apply { init(null, arrayOf(pinned), SecureRandom()) }
                (tls.socketFactory.createSocket() as SSLSocket).use { connection ->
                    if (token != epoch) return@execute
                    socket = connection
                    connection.enabledProtocols = tlsProtocols(connection.supportedProtocols)
                    connection.soTimeout = 10000
                    phase = "conexión con el receptor"
                    connection.connect(InetSocketAddress(invitation.host, invitation.port), 5000)
                    phase = "verificación del certificado"
                    connection.startHandshake()
                    phase = "envío de metadatos"
                    val hash = PeerProtocol.sha256(bytes)
                    PeerProtocol.write(connection.outputStream, JSONObject().put("token", invitation.token)
                        .put("name", PeerProtocol.safeName(name)).put("size", bytes.size).put("sha256", hash))
                    post(token) { it.copy(status = "Certificado verificado · esperando aceptación del receptor") }
                    phase = "aceptación del receptor"
                    connection.soTimeout = 50000
                    require(PeerProtocol.read(connection.inputStream).optString("status") == "ready") { "El receptor no aceptó el archivo" }
                    connection.soTimeout = 10000
                    phase = "envío del archivo"
                    connection.outputStream.write(bytes); connection.outputStream.flush()
                    val receipt = PeerProtocol.read(connection.inputStream)
                    require(receipt.optString("status") == "saved" && PeerProtocol.same(receipt.optString("sha256"), hash)) { "Falta confirmación de guardado" }
                    post(token) { it.copy(active = false, status = "Entrega confirmada por el receptor",
                        result = "${PeerProtocol.safeName(name)} · ${bytes.size} bytes\nSHA-256 $hash") }
                }
            } catch (error: Exception) {
                post(token) { it.copy(active = false, status = "Envío no completado: $phase · ${error.javaClass.simpleName}: ${error.message}") }
            } finally { socket = null }
        }
    }
    fun stop() {
        main.removeCallbacks(sessionDeadline)
        epoch++
        accepted.set(false); choice?.countDown(); choice = null
        runCatching { socket?.close() }; runCatching { server?.close() }
        socket = null; server = null
        if (state.active) { state = state.copy(active = false, offer = null, invitation = "", status = "Sesión detenida"); changed(state) }
    }
    fun close() { stop(); worker.shutdownNow() }
}
