package com.msarobotika.cyberdeck

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import java.time.Instant
import java.util.ArrayDeque

data class LanService(
    val name: String, val type: String, val addresses: List<String> = emptyList(),
    val port: Int? = null, val available: Boolean = true, val resolution: String = "Resolución pendiente"
) {
    val key get() = "$type\u0000$name"
}

data class LanSnapshot(
    val running: Boolean = false, val startedAt: String = "", val finishedAt: String = "",
    val services: List<LanService> = emptyList(), val issues: List<String> = emptyList()
) {
    fun evidence() = DiagnosticResult("lan", "Servicios anunciados en la red local", finishedAt.ifEmpty { startedAt },
        "${services.size} servicios observados · ${if (running) "búsqueda en curso" else "búsqueda finalizada"}",
        listOf("Inicio" to startedAt, "Fin" to finishedAt,
            "Tipos consultados" to LanInventory.TYPES.joinToString(),
            "Método" to "DNS-SD/mDNS de Android. Sin barrido de IP ni conexión a los puertos anunciados. Los anuncios no autentican la identidad del equipo.",
            "Límites" to "Hasta 64 servicios durante 15 segundos. No es una lista completa de equipos. Varios servicios pueden pertenecer al mismo equipo.") +
            services.map { it.name to "Tipo: ${it.type}\nDirecciones: ${it.addresses.joinToString().ifEmpty { "No resueltas" }}\nPuerto anunciado: ${it.port ?: "No resuelto"}\n${it.resolution}\n${if (it.available) "Observado durante esta sesión" else "Dejó de anunciarse durante esta sesión"}" } +
            issues.map { "Incidencia de descubrimiento" to it })
}

/** Manual, bounded discovery of advertised services. Never opens a discovered endpoint. */
class LanInventory(context: Context, private val changed: (LanSnapshot) -> Unit) {
    companion object {
        val TYPES = listOf("_http._tcp.", "_https._tcp.", "_ipp._tcp.", "_ipps._tcp.", "_googlecast._tcp.", "_workstation._tcp.")
    }
    private val nsd = context.applicationContext.getSystemService(NsdManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val discoveries = mutableListOf<NsdManager.DiscoveryListener>()
    private val pending = ArrayDeque<Pair<String, NsdServiceInfo>>()
    private var resolving = false
    private var generation = 0
    private var snapshot = LanSnapshot()
    private val deadline = Runnable { stop() }

    fun start() {
        stop()
        val token = ++generation
        snapshot = LanSnapshot(running = true, startedAt = Instant.now().toString())
        publish()
        main.postDelayed(deadline, 15000)
        TYPES.forEach { type ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) {}
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    main.post {
                        if (valid(token)) issue("$type: Android no pudo iniciar la búsqueda (código $errorCode)")
                    }
                }
                override fun onServiceFound(info: NsdServiceInfo) { main.post {
                    if (!valid(token)) return@post
                    val row = LanService(info.serviceName.take(160), type)
                    if (snapshot.services.any { it.key == row.key }) return@post
                    if (snapshot.services.size >= 64) {
                        if (snapshot.issues.none { it.contains("64") }) issue("Límite de 64 servicios alcanzado; resultado parcial")
                        return@post
                    }
                    snapshot = snapshot.copy(services = snapshot.services + row)
                    pending.add(row.key to info)
                    publish()
                    resolveNext(token)
                } }
                override fun onServiceLost(info: NsdServiceInfo) { main.post {
                    if (!valid(token)) return@post
                    val key = LanService(info.serviceName.take(160), type).key
                    update(key) { it.copy(available = false) }
                } }
            }
            discoveries += listener
            runCatching { nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener) }
                .onFailure { issue("$type: ${it.javaClass.simpleName}") }
        }
    }

    private fun valid(token: Int) = snapshot.running && token == generation
    private fun publish() = changed(snapshot)
    private fun issue(text: String) {
        snapshot = snapshot.copy(issues = (snapshot.issues + text).distinct().take(16))
        publish()
    }
    private fun update(key: String, transform: (LanService) -> LanService) {
        snapshot = snapshot.copy(services = snapshot.services.map { if (it.key == key) transform(it) else it })
        publish()
    }

    @Suppress("DEPRECATION")
    private fun resolveNext(token: Int) {
        if (!valid(token) || resolving || pending.isEmpty()) return
        val (key, info) = pending.removeFirst()
        resolving = true
        val listener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) { main.post {
                if (!valid(token)) return@post
                resolving = false
                update(key) { it.copy(resolution = "Anuncio observado; resolución fallida (código $errorCode)") }
                resolveNext(token)
            } }
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) { main.post {
                if (!valid(token)) return@post
                resolving = false
                val addresses = if (android.os.Build.VERSION.SDK_INT >= 34)
                    serviceInfo.hostAddresses.mapNotNull { it.hostAddress }.distinct()
                else listOfNotNull(serviceInfo.host?.hostAddress)
                update(key) { it.copy(addresses = addresses, port = serviceInfo.port.takeIf { p -> p in 1..65535 },
                    resolution = "Datos anunciados resueltos; servicio sin comprobar") }
                resolveNext(token)
            } }
        }
        runCatching { nsd.resolveService(info, listener) }.onFailure {
            resolving = false
            update(key) { row -> row.copy(resolution = "Resolución no disponible: ${it.javaClass.simpleName}") }
            resolveNext(token)
        }
    }

    fun stop() {
        generation++
        main.removeCallbacks(deadline)
        discoveries.forEach { runCatching { nsd.stopServiceDiscovery(it) } }
        discoveries.clear()
        pending.clear()
        resolving = false
        if (snapshot.running) {
            snapshot = snapshot.copy(running = false, finishedAt = Instant.now().toString(),
                services = snapshot.services.map { if (it.resolution == "Resolución pendiente")
                    it.copy(resolution = "Anuncio observado; resolución no completada antes de detener") else it })
            publish()
        }
    }
}
