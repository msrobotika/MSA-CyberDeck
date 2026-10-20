package com.msarobotika.cyberdeck

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

data class AiContextSnapshot(val text:String,val hasPhone:Boolean=false) {
 init { require(text.length<=900){"Contexto demasiado largo"} }
 val sha256:String get()=MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
}

/** Compact, dated Android facts. No IPs, SSIDs, identifiers or arbitrary diagnostic fields. */
object AiContext {
 const val CAPABILITIES="CiberDeck es una app Android local. Diagnóstico del móvil, conexión, BLE, NFC, APK, archivos e informes funcionan sin ESP32. USB/radar requieren un periférico compatible por OTG; RF/IR externos siguen pendientes. El analista explica; Agente ejecuta herramientas tipadas. Una capacidad anunciada no acredita permiso ni conexión."
 private val fields=setOf("Android","Bluetooth LE","NFC","USB Host","VPN en la red activa","DNS privado del enlace","Batería")
 fun observedFields(phone:DiagnosticResult):List<Pair<String,String>> = if(phone.kind!="phone")emptyList() else fields.mapNotNull{key->phone.details.firstOrNull{it.first==key}}.map{it.first to it.second.replace('\n',' ').replace('\r',' ').take(90)}
 fun agentResult(session:AgentSession):DiagnosticResult? {
  val run=session.run?:return null
  if(session.results.isEmpty())return null
  return DiagnosticResult("agent-context","Última sesión del agente",run.finishedAt,"Resumen en memoria; no es una nueva medición ni verifica sus fuentes",session.results.map{step->step.tool.wire to when{
   step.error.isNotBlank()->"Error: ${step.error.take(90)}"
   step.awaiting->"Pendiente de interacción; sin lectura"
   step.skipped->"Omitido; sin lectura"
   step.manual->"Confirmación manual, sin lectura verificada"
   step.result!=null->"${if(step.tool==AgentTool.ANALYST)"Texto IA" else "Resultado"} · ${step.result.checkedAt.take(30)} · ${step.result.summary.take(70)}"
   else->"Sin resultado"
  }})
 }
 fun domainGuidance(question:String):String {
  val text=Normalizer.normalize(question,Normalizer.Form.NFD).replace(Regex("""\p{M}+"""),"").lowercase(Locale.ROOT)
  if(!Regex("radiaci|electromagnet|transformador|centro de transform|\\bemf\\b|riesgo.*salud").containsMatchIn(text))return ""
  return "Alcance CEM: el magnetómetro Android mide campo magnético local, no dosis de radiación, campo eléctrico ni exposición sanitaria. La serie de CiberDeck (2 muestras/s) no caracteriza 50 Hz. Ni un panel separador ni una cifra del móvil permiten declarar riesgo o seguridad. Para el taller: evaluación del servicio de prevención con equipo adecuado/calibrado, frecuencia, carga, distancias y puestos. Guía: INSST, campos electromagnéticos en los lugares de trabajo (2019)."
 }
 fun snapshot(phone:DiagnosticResult?,objective:String=""):AiContextSnapshot {
  val lines=mutableListOf<String>()
  if(objective.isNotBlank())lines+="Objetivo declarado por el usuario (no una observación): ${objective.trim().take(240)}"
  if(phone==null || phone.kind!="phone")lines+="Estado Android: sin lectura adjunta. No asumir modelo, conexión, permisos ni presencia de ESP32."
  else {
   lines+="Instantánea Android tomada en ${phone.checkedAt.take(80)}; no es monitorización continua."
   lines+="Equipo Android: ${phone.target.take(60)}"
   observedFields(phone).forEach{(key,value)->lines+="$key: $value"}
   lines+="USB Host anuncia capacidad/dispositivos; no identifica un ESP32. Sin datos sobre otros móviles."
  }
  val text=lines.joinToString("\n")
  return AiContextSnapshot(if(text.length>900)text.take(870)+"\n[Contexto abreviado]" else text,phone?.kind=="phone")
 }
}
