package com.msarobotika.cyberdeck

/** Presentation rules over observed Android values. No global security verdict. */
object PhoneBriefPolicy {
 fun key(check:PhoneCheck)="${check.section}::${check.title}"
 fun priorities(checks:List<PhoneCheck>)=checks.filter{it.tone==PhoneTone.REVIEW || it.tone==PhoneTone.UNKNOWN}.sortedBy{if(it.tone==PhoneTone.REVIEW)0 else 1}
 fun label(tone:PhoneTone)=when(tone){PhoneTone.REVIEW->"Conviene revisarlo";PhoneTone.UNKNOWN->"Dato no disponible";PhoneTone.OK->"Estado informado por Android";PhoneTone.INFO->"Para tu información"}
 fun action(check:PhoneCheck)=when(check.settings){
  "android.settings.SYSTEM_UPDATE_SETTINGS"->"Ver actualizaciones"
  "android.settings.SECURITY_SETTINGS"->"Abrir seguridad de Android"
  "android.settings.APPLICATION_DEVELOPMENT_SETTINGS"->"Ver opciones de desarrollo"
  "android.settings.VPN_SETTINGS"->"Revisar la VPN"
  "android.settings.WIFI_SETTINGS"->"Revisar la conexión"
  else->"Abrir ajuste relacionado"
 }
}
