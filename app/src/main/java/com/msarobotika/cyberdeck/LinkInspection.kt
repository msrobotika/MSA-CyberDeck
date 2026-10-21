package com.msarobotika.cyberdeck

import java.net.IDN
import java.net.URI
import java.time.Instant

data class LinkObservation(val host:String,val asciiHost:String,val checks:List<Pair<String,String>>,val signals:Int) {
    fun diagnostic()=DiagnosticResult("link",asciiHost,Instant.now().toString(),"$signals señales para revisar; sin consultar reputación",checks+listOf("Privacidad" to "No se guarda la dirección completa, credenciales, ruta, consulta ni fragmento", "Límite" to "Análisis sintáctico local; no determina si un sitio es legítimo y no abre enlaces"))
}
object LinkInspection {
    fun inspect(raw:String):LinkObservation {
        val value=raw.trim()
        require(value.length in 1..4096 && value.none{it.isWhitespace() || it.isISOControl()}){"Introduce una URL HTTP(S) de hasta 4096 caracteres, sin espacios"}
        val uri=runCatching{URI(value)}.getOrElse{throw IllegalArgumentException("La dirección no tiene un formato válido")}
        val scheme=uri.scheme?.lowercase()
        require(scheme in setOf("http","https")){"Solo se analizan direcciones HTTP o HTTPS"}
        val authority=uri.rawAuthority?:throw IllegalArgumentException("Falta el dominio")
        val credentials='@' in authority
        val hostPort=authority.substringAfterLast('@')
        val host=if(hostPort.startsWith("["))hostPort.substringBefore(']')+"]" else hostPort.substringBefore(':')
        require(host.isNotEmpty() && '%' !in host && '\\' !in host){"Dominio no válido"}
        val ascii=if(host.startsWith("[")){
            require(uri.host!=null){"IPv6 no válida"};host.lowercase()
        }else runCatching{IDN.toASCII(host,IDN.USE_STD3_ASCII_RULES).lowercase()}.getOrElse{throw IllegalArgumentException("Dominio no válido")}
        require(ascii.length<=253 && (ascii.startsWith("[") || ascii.trimEnd('.').split('.').all{it.isNotEmpty() && it.length<=63})){"Dominio no válido"}
        val suffix=if(hostPort.startsWith("["))hostPort.substringAfter(']',"") else hostPort.substringAfter(':',"")
        val port=if(suffix.isEmpty())null else suffix.removePrefix(":").toIntOrNull()?.takeIf{it in 1..65535}?:throw IllegalArgumentException("Puerto no válido")
        var count=0
        val checks=mutableListOf<Pair<String,String>>()
        fun add(title:String,text:String,flag:Boolean=false){checks+=title to text;if(flag)count++}
        add("Destino real",ascii)
        add("Transporte",if(scheme=="http")"HTTP: la URL no pide cifrado TLS" else "HTTPS solicitado; certificado y servidor no comprobados",scheme=="http")
        add("Usuario en URL",if(credentials)"Contiene @ antes del destino; el texto previo no identifica al servidor" else "No aparece",credentials)
        val idn=host.any{it.code>127} || ascii.split('.').any{it.startsWith("xn--")}
        add("Dominio internacional",if(idn)"Unicode/punycode: compara el dominio ASCII; no implica fraude" else "Sin codificación internacional detectada",idn)
        val ip=ascii.startsWith("[") || ascii.matches(Regex("[0-9.]+")) || ascii.matches(Regex("(?i)0x[0-9a-f]+"))
        add("Dirección numérica",if(ip)"IP o formato numérico: no muestra un dominio reconocible" else "Nombre de dominio",ip)
        val unusual=port!=null && port!=(if(scheme=="https")443 else 80)
        add("Puerto",port?.toString()?:"Predeterminado",unusual)
        val shortener=ascii.trimEnd('.') in setOf("bit.ly","t.co","tinyurl.com","is.gd","cutt.ly","goo.gl")
        add("Acortador conocido",if(shortener)"Destino final oculto; no se sigue la redirección" else "No coincide con la lista local limitada",shortener)
        add("Consulta",if(uri.rawQuery!=null)"Tiene parámetros; pueden contener información sensible" else "Sin parámetros")
        add("Interpretación","Las señales explican la forma del enlace; su ausencia no garantiza seguridad")
        return LinkObservation(host,ascii,checks,count)
    }
}
