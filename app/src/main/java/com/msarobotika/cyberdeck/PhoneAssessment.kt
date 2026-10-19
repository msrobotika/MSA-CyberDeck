package com.msarobotika.cyberdeck

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class PhoneTone { OK, REVIEW, INFO, UNKNOWN }
data class PhoneCheck(val title:String,val value:String,val explanation:String,val tone:PhoneTone,val section:String,val settings:String?=null)
object PhoneAssessment {
    fun lock(secure:Boolean?)=PhoneCheck("Bloqueo de pantalla",when(secure){true->"Configurado";false->"Sin bloqueo seguro";null->"No disponible"},
        "Android informa si hay PIN, patrón o contraseña. No se lee tu credencial ni se comprueba su fortaleza.",
        when(secure){true->PhoneTone.OK;false->PhoneTone.REVIEW;null->PhoneTone.UNKNOWN},"Seguridad","android.settings.SECURITY_SETTINGS")
    fun patch(value:String,today:LocalDate):PhoneCheck {
        val date=runCatching{LocalDate.parse(value)}.getOrNull()
        val age=date?.let{ChronoUnit.DAYS.between(it,today)}
        return PhoneCheck("Parche de seguridad",if(date==null)"No disponible" else value,
            if(age==null || age<0)"Fecha no interpretable o posterior al reloj actual. No se evalúan vulnerabilidades."
            else "$age días desde la fecha declarada. Revisar al superar 180 días es un criterio local, no un catálogo de CVE ni una comprobación de la última versión del fabricante.",
            when{age==null || age<0->PhoneTone.UNKNOWN;age>180->PhoneTone.REVIEW;else->PhoneTone.INFO},"Seguridad","android.settings.SYSTEM_UPDATE_SETTINGS")
    }
    fun debugging(enabled:Int)=PhoneCheck("Depuración USB",when(enabled){1->"Activada";0->"Desactivada";else->"No disponible"},
        "Puede ser necesaria para desarrollar CiberDeck. Revisa los ordenadores autorizados; este ajuste por sí solo no indica una intrusión ni describe la depuración inalámbrica.",
        if(enabled in 0..1)PhoneTone.INFO else PhoneTone.UNKNOWN,"Seguridad","android.settings.APPLICATION_DEVELOPMENT_SETTINGS")
}
