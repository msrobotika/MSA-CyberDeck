package com.msarobotika.cyberdeck

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import java.net.*
import java.io.DataInputStream
import java.security.SecureRandom
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class BrowserOffer(val name: String, val size: Int)
data class BrowserState(val active: Boolean = false, val status: String = "Recepción desde navegador detenida",
    val url: String = "", val offer: BrowserOffer? = null)

/** Opt-in HTTP on one local Wi-Fi/Ethernet address. No remote directory or download endpoints. */
class BrowserReceiver(private val context: Context, private val changed: (BrowserState) -> Unit,
    private val received: (String, ByteArray) -> Unit) {
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    @Volatile private var epoch = 0
    @Volatile private var listener: ServerSocket? = null
    @Volatile private var client: Socket? = null
    @Volatile private var decision: CountDownLatch? = null
    private val approved = AtomicBoolean(false)
    private var state = BrowserState()
    private val deadline = Runnable { stop() }
    private fun post(token: Int, update: BrowserState) { main.post {
        if (epoch == token) { state = update; changed(state); if (!state.active) main.removeCallbacks(deadline) }
    } }
    fun start() {
        stop()
        val token = ++epoch
        state = BrowserState(active = true, status = "Preparando recepción HTTP local"); changed(state)
        main.postDelayed(deadline, 120000)
        worker.execute {
            try {
                val manager = context.getSystemService(ConnectivityManager::class.java)
                val network = manager.activeNetwork ?: error("Conecta el receptor a tu Wi-Fi")
                val caps = manager.getNetworkCapabilities(network) ?: error("Red no disponible")
                check(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    "Usa Wi-Fi o Ethernet local; no se abre el receptor en datos móviles"
                }
                val address = manager.getLinkProperties(network)?.linkAddresses?.map { it.address }?.filterIsInstance<Inet4Address>()
                    ?.firstOrNull { it.isSiteLocalAddress || it.isLinkLocalAddress } ?: error("No hay IPv4 local compatible")
                val server = ServerSocket().apply { bind(InetSocketAddress(address, 0), 2); soTimeout = 1000 }
                if (token != epoch) { server.close(); return@execute }
                listener = server
                val authority = "${address.hostAddress}:${server.localPort}"
                val secret = ByteArray(24).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
                val route = "/s/$secret"
                val url = "http://$authority$route"
                post(token, BrowserState(true, "Abre la dirección en el navegador del otro móvil · caduca en 2 min", url))
                val expires = System.currentTimeMillis() + 120000
                var attempts = 0
                var finished = false
                while (token == epoch && !finished && System.currentTimeMillis() < expires && attempts < 64) {
                    val socket = try { server.accept() } catch (_: SocketTimeoutException) { continue }
                    attempts++
                    client = socket
                    try { socket.use { connection ->
                        connection.soTimeout = 15000
                        val input = connection.getInputStream()
                        val request = BrowserProtocol.request(input)
                        require(request.headers["host"] == authority) { "Host no autorizado" }
                        if (request.method == "GET" && request.path == route) {
                            response(connection, 200, BrowserProtocol.page, "text/html; charset=utf-8")
                        } else if (request.method == "POST" && request.path == "$route/upload") {
                            val (name, length) = BrowserProtocol.upload(request, authority, route)
                            val latch = CountDownLatch(1); decision = latch; approved.set(false)
                            post(token, BrowserState(true, "Confirma el archivo elegido en el otro móvil", url, BrowserOffer(name, length)))
                            val accepted = latch.await(45, TimeUnit.SECONDS) && approved.get() && token == epoch
                            decision = null
                            finished = true
                            if (!accepted) {
                                post(token, BrowserState(status = "Archivo rechazado · sesión cerrada"))
                                runCatching { response(connection, 403, "Archivo rechazado o aprobación caducada. No se ha guardado.") }
                            } else {
                                val bytes = ByteArray(length)
                                DataInputStream(input).readFully(bytes)
                                check(token == epoch) { "Sesión cerrada" }
                                val hash = PeerProtocol.sha256(bytes)
                                received(name, bytes)
                                post(token, BrowserState(status = "Archivo recibido y guardado · $length bytes · SHA-256 $hash"))
                                runCatching { response(connection, 200, "Archivo guardado en CyberDeck.\n$length bytes\nSHA-256 calculado en el receptor:\n$hash") }
                            }
                        } else response(connection, 404, "Sesión o ruta no disponible")
                    } } catch (error: Exception) {
                        runCatching { response(socket, 400, "Petición no completada") }
                        // A malformed unauthenticated request cannot overwrite the UI or save data.
                        if (decision != null || approved.get()) {
                            post(token, BrowserState(status = "Envío no completado: ${error.javaClass.simpleName}")); finished = true
                        }
                    } finally { runCatching { socket.close() }; client = null; decision = null }
                }
                if (!finished) post(token, BrowserState(status = "Sesión de navegador caducada o cerrada"))
            } catch (error: Exception) { post(token, BrowserState(status = "Recepción: ${error.message}")) }
            finally { if (token == epoch) { runCatching { listener?.close() }; listener = null } }
        }
    }
    private fun response(socket: Socket, code: Int, body: String, type: String = "text/plain; charset=utf-8") {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 $code ${if(code == 200) "OK" else "Error"}\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\nCache-Control: no-store\r\nReferrer-Policy: no-referrer\r\nX-Content-Type-Options: nosniff\r\nX-Frame-Options: DENY\r\nContent-Security-Policy: default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'\r\n\r\n"
        socket.getOutputStream().apply { write(headers.toByteArray(Charsets.US_ASCII)); write(bytes); flush() }
    }
    fun decide(accept: Boolean) { approved.set(accept); decision?.countDown() }
    fun stop(reason: String = "Recepción desde navegador cerrada · el enlace anterior ya no funciona") {
        epoch++; main.removeCallbacks(deadline); approved.set(false); decision?.countDown(); decision = null
        runCatching { client?.close() }; runCatching { listener?.close() }; client = null; listener = null
        if (state.active) { state = BrowserState(status = reason); changed(state) }
    }
    fun close() { stop(); worker.shutdown() }
}
