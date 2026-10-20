package com.msarobotika.cyberdeck

import java.text.Normalizer
import java.util.Locale

data class DeckArea(val id:String,val title:String,val description:String,val glyph:String,val routes:List<String>)

object DeckCatalog {
    fun recent(previous:List<String>,opened:String?=null):List<String> {
        val valid=deckTools.map{it.route}.toSet()
        return (listOfNotNull(opened?.takeIf{it in valid})+previous).filter{it in valid}.distinct().take(3)
    }
    val areas=listOf(
        DeckArea("phone","Móvil y protección","Ajustes, APK, enlaces y llamadas","home",listOf("Móvil","APK","Enlaces","Llamadas")),
        DeckArea("network","Red e Internet","Equipos, DNS, puertos y sitios web","radio",listOf("Equipo","Mi red","Red","Web","CIDR")),
        DeckArea("proximity","Bluetooth y NFC","Dispositivos cercanos y etiquetas","radio",listOf("Bluetooth","NFC")),
        DeckArea("hardware","ESP32 y sensores","USB, Wi-Fi del ESP y campo magnético","tools",listOf("USB","Radio","Magnético","Módulos")),
        DeckArea("evidence","Archivos y trabajos","Recibir, organizar y comparar pruebas","files",listOf("Recibir","Sesiones","Trabajos","Comparar")),
        DeckArea("assistant","Agente e IA","Ejecutar tareas o interpretar resultados","report",listOf("Agente","IA"))
    )
    private val aliases=mapOf(
        "Móvil" to "telefono bateria memoria seguridad permisos recursos",
        "APK" to "aplicacion firma permisos paquete",
        "Enlaces" to "url phishing estafa direccion sospechosa",
        "Llamadas" to "telefono spam operador",
        "Equipo" to "auditoria router servidor",
        "Mi red" to "wifi wi-fi dispositivos lan servicios descubrir",
        "Red" to "internet dominio dns puerto conexion",
        "Web" to "https certificado tls cabeceras pagina",
        "CIDR" to "subred mascara ipv4 calculadora",
        "Bluetooth" to "ble bluetooth cercanos",
        "NFC" to "etiqueta ndef proximidad",
        "USB" to "esp esp32 serie consola otg cable",
        "Radio" to "wifi wi-fi radar canales esp32",
        "Magnético" to "emf magnetometro sensor microteslas",
        "Recibir" to "transferir importar audio foto archivo navegador",
        "Sesiones" to "transferir compartir cifrado archivos",
        "Trabajos" to "organizar objetivo informe evidencias",
        "Comparar" to "cambios diferencias diagnosticos",
        "Agente" to "tareas automaticas coordinador asistentes",
        "IA" to "inteligencia artificial modelo local analizar explicar"
    )
    private fun normalize(text:String)=Normalizer.normalize(text,Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT)
    fun tools(area:String,query:String):List<DeckTool> {
        val words=normalize(query).trim().split(Regex("\\s+")).filter{it.isNotBlank()}
        val routes=areas.firstOrNull{it.id==area}?.routes
        return deckTools.filter { tool ->
            if(words.isEmpty()) area=="all" || routes?.contains(tool.route)==true
            else {
                val text=normalize("${tool.title} ${tool.subtitle} ${tool.group} ${aliases[tool.route].orEmpty()}")
                words.all{text.contains(it)}
            }
        }
    }
}
