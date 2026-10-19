package com.msarobotika.cyberdeck

import java.text.Normalizer
import java.util.Locale

data class AgentTask(val id:String,val title:String,val hint:String,val steps:List<AgentTool>,val instant:Boolean=false,val inputs:AgentInputs=AgentInputs()) {
 fun session(request:String=title)=AgentSession(request=request,plan=AgentPlan(steps),inputs=inputs,origin="Tarea del móvil")
}
object AgentTasks {
 val phone=AgentTask("phone","Revisar mi móvil","Ajustes, conexión, RAM, batería y APK de CiberDeck",listOf(AgentTool.PHONE,AgentTool.CONNECTION,AgentTool.RESOURCES,AgentTool.REVIEW,AgentTool.SELF_APK),true)
 val connection=AgentTask("connection","Comprobar conexión","Red actual y estado de Internet según Android",listOf(AgentTool.CONNECTION,AgentTool.RESOURCES),true)
 val ble=AgentTask("ble","Buscar Bluetooth","Anuncios reales durante diez segundos",listOf(AgentTool.BLE),true)
 val nfc=AgentTask("nfc","Leer NFC","Acerca una etiqueta al móvil",listOf(AgentTool.NFC),true)
 val apk=AgentTask("apk","Inspeccionar una APK","Elige el archivo; no se instala",listOf(AgentTool.APK),true)
 val link=AgentTask("link","Revisar un enlace","Comprobación local sin visitar la dirección",listOf(AgentTool.LINK))
 val lan=AgentTask("lan","Servicios de mi red","Anuncios de servicios, sin barrer equipos",listOf(AgentTool.LAN))
 val all=listOf(phone,connection,ble,nfc,apk,link,lan)
 private fun normalized(raw:String)=Normalizer.normalize(raw,Normalizer.Form.NFD).replace(Regex("""\p{M}+"""),"").lowercase(Locale.ROOT)
 /** Small explicit grammar, not an LLM: unmatched/negated/compound requests are left for review. */
 fun match(raw:String):AgentTask? {
  val t=normalized(raw).trim().trimEnd('.','!','?')
  if(t.length !in 3..800 || Regex("""\b(no|nunca|evita|excepto)\b""").containsMatchIn(t))return null
  if(Regex("""\b(borra|elimina|envia|transfiere|clona|instala|desactiva)\b""").containsMatchIn(t))return null
  val topics=listOf("bluetooth","nfc","apk","enlace","conexion","movil").count{t.contains(it)}
  if(topics>1 && !(t.contains("movil") && (t.contains("conexion") || t.contains("apk"))))return null
  val cleaned=t.replace(Regex("""\bsin (?:el |usar |necesitar )?(?:esp32|esp|usb|placa)\b"""),"").trim()
  return when {
   Regex("""(?:revisa|revisar|analiza|analizar|comprueba|comprobar|chequea|chequear) (?:mi |el |este )?(?:movil|telefono)(?: y (?:su |mi |la |el estado de su )?conexion)?""").matches(cleaned)->phone
   Regex("""(?:revisa|revisar|analiza|analizar|comprueba|comprobar|ver|muestra) (?:mi |la |el estado de (?:mi |la )?)?(?:conexion|internet|wifi)""").matches(cleaned)->connection
   Regex("""(?:busca|buscar|escanea|escanear|explora|explorar) (?:dispositivos |anuncios )?(?:bluetooth|ble)(?: cercanos)?""").matches(cleaned)->ble
   Regex("""(?:lee|leer|escanea|escanear) (?:una |mi |la )?(?:etiqueta )?nfc""").matches(cleaned)->nfc
   Regex("""(?:revisa|revisar|analiza|analizar|inspecciona|inspeccionar) (?:una |mi |la )?apk""").matches(cleaned)->apk
   Regex("""(?:revisa|revisar|analiza|analizar|comprueba|comprobar) (?:este |el |un )?(?:enlace|url) https?://\S+""").matches(cleaned)->{
    val url=Regex("""https?://\S+""",RegexOption.IGNORE_CASE).find(raw)?.value?:return null
    if(runCatching{LinkInspection.inspect(url)}.isFailure)return null
    link.copy(inputs=AgentInputs(url=url))
   }
   else->null
  }
 }
 fun checkHardwareRequest(request:String,plan:AgentPlan):AgentPlan {
  if(plan.steps.none{it==AgentTool.USB || it==AgentTool.RADIO})return plan
  val t=normalized(request)
  val explicit=Regex("""\b(esp32|esp|usb|placa|radar|sentinel)\b""").containsMatchIn(t)
  val negated=Regex("""\b(sin|no)\b.{0,35}\b(esp32|esp|usb|placa|radar|sentinel)\b""").containsMatchIn(t)
  require(explicit && !negated){"La IA añadió hardware que no has pedido. Prueba «Revisar mi móvil» o elige una tarea directa."}
  return plan
 }
}
