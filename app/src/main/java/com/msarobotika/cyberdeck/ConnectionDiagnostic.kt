package com.msarobotika.cyberdeck

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.time.Instant

/** Local Android state only: no network probes, Wi-Fi scans or location access. */
object ConnectionDiagnostic {
    fun read(context: Context): DiagnosticResult {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork
        val capabilities = network?.let(manager::getNetworkCapabilities)
        val properties = network?.let(manager::getLinkProperties)
        val transports = (listOf(
            NetworkCapabilities.TRANSPORT_WIFI to "Wi-Fi",
            NetworkCapabilities.TRANSPORT_CELLULAR to "Datos móviles",
            NetworkCapabilities.TRANSPORT_ETHERNET to "Ethernet",
            NetworkCapabilities.TRANSPORT_VPN to "VPN"
        ) + if (android.os.Build.VERSION.SDK_INT >= 31)
            listOf(NetworkCapabilities.TRANSPORT_USB to "USB") else emptyList()).filter { capabilities?.hasTransport(it.first) == true }.map { it.second }
        val validated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val captive = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true
        return DiagnosticResult("connection", "Red activa de Android", Instant.now().toString(),
            when {
                network == null -> "Sin red activa"
                captive -> "Android detecta un portal cautivo"
                validated -> "Android ha validado acceso a Internet"
                else -> "Red presente; Internet no validado por Android"
            }, listOf(
                "Transporte" to transports.joinToString().ifEmpty { "No identificado" },
                "Interfaz" to (properties?.interfaceName ?: "Sin interfaz"),
                "Direcciones locales" to (properties?.linkAddresses?.joinToString { it.toString() }?.ifEmpty { "No disponibles" } ?: "No disponibles"),
                "DNS configurados" to (properties?.dnsServers?.joinToString { it.hostAddress ?: "" }?.ifEmpty { "No disponibles" } ?: "No disponibles"),
                "Método" to "Estado local del sistema. No mide velocidad ni demuestra que la red sea segura."
            ))
    }
}
