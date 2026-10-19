package com.msarobotika.cyberdeck

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

enum class ReviewMark(val label:String){PENDING("Sin revisar"),CURRENT("Marcadas por ti"),STALE("Lectura cambiada")}
data class PhoneReviewItem(val key:String,val fingerprint:String,val check:PhoneCheck,val advice:String)
data class PhoneReviewRecord(val key:String,val fingerprint:String,val reviewed:Boolean,val note:String,val at:String)
object PhoneReviewPolicy {
 const val MAX_BYTES=512*1024
 private fun hash(parts:List<String>):String {
  val bytes=ByteArrayOutputStream();DataOutputStream(bytes).use{out->parts.forEach{part->val raw=part.toByteArray(Charsets.UTF_8);out.writeInt(raw.size);out.write(raw)}}
  return MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()).joinToString(""){"%02x".format(it)}
 }
 fun items(device:String,checks:List<PhoneCheck>):List<PhoneReviewItem>{
  require(device.isNotBlank() && device.length<=200 && checks.size<=64)
  val items=checks.map{check->
   require(check.title.length<=160 && check.section.length<=80 && check.value.length<=4096 && check.explanation.length<=4096)
   val key=hash(listOf(device,check.section,check.title))
   val fingerprint=hash(listOf("phone-review-v1",device,check.section,check.title,check.value,check.explanation,check.tone.name,check.settings.orEmpty()))
   PhoneReviewItem(key,fingerprint,check,advice(check))
  }
  require(items.map{it.key}.distinct().size==items.size){"Hay observaciones duplicadas; no se pueden atribuir marcas manuales"}
  return items.sortedBy{when(it.check.tone){PhoneTone.REVIEW->0;PhoneTone.UNKNOWN->1;PhoneTone.INFO->2;PhoneTone.OK->3}}
 }
 fun status(item:PhoneReviewItem,record:PhoneReviewRecord?)=when{record==null || record.key!=item.key->ReviewMark.PENDING;record.fingerprint!=item.fingerprint->ReviewMark.STALE;record.reviewed->ReviewMark.CURRENT;else->ReviewMark.PENDING}
 fun validate(records:List<PhoneReviewRecord>){
  require(records.size<=128 && records.map{it.key}.distinct().size==records.size){"Hasta 128 observaciones distintas"}
  records.forEach{r->require(r.key.matches(Regex("[a-f0-9]{64}")) && r.fingerprint.matches(Regex("[a-f0-9]{64}")) && r.note.length<=600){"Anotación no válida"};Instant.parse(r.at)}
 }
 fun update(records:List<PhoneReviewRecord>,item:PhoneReviewItem,reviewed:Boolean,note:String):List<PhoneReviewRecord> {
  val next=records.filterNot{it.key==item.key}+PhoneReviewRecord(item.key,item.fingerprint,reviewed,note,Instant.now().toString())
  validate(next);return next
 }
 private fun advice(check:PhoneCheck)=when(check.title){
  "Bloqueo de pantalla"->"Revisa el método de bloqueo en los ajustes de seguridad. La app no comprueba la fortaleza del PIN, patrón o contraseña."
  "Parche de seguridad"->"Consulta las actualizaciones oficiales del fabricante. La fecha declarada no identifica por sí sola vulnerabilidades ni una actualización disponible."
  "Depuración USB"->"Revisa los ordenadores autorizados en las opciones de desarrollo. Puede ser necesaria para trabajar con CiberDeck; no se cambia automáticamente."
  "Cifrado del almacenamiento"->"Contrasta el estado con los ajustes y la política de tu dispositivo. Un estado no disponible requiere comprobarlo por otra vía; no significa ausencia de cifrado."
  "Acceso a Internet"->"Comprueba la conexión desde sus ajustes. La validación de conectividad no prueba que una red sea confiable."
  "VPN en la red activa"->"Si necesitas un túnel, revisa su perfil y alcance en los ajustes VPN. La presencia del transporte no certifica proveedor, configuración ni cobertura de otras apps."
  "DNS privado del enlace"->"Revisa el resolutor y la política que quieres usar. Otras aplicaciones pueden resolver por mecanismos diferentes."
  "Proxy del enlace"->"Contrasta si esperabas un proxy en esta conexión. La app no conoce la intención del propietario ni verifica sus credenciales."
  else->"Contrasta esta observación con la configuración esperada de tu equipo. No se interpreta como una vulnerabilidad."
 }
}
object PhoneReviewCodec {
 fun encode(records:List<PhoneReviewRecord>):String {
  PhoneReviewPolicy.validate(records)
  val raw=JSONObject().put("format","msa.cyberdeck.phone-review.v1").put("records",JSONArray().apply{records.forEach{r->put(JSONObject().put("key",r.key).put("fingerprint",r.fingerprint).put("reviewed",r.reviewed).put("note",r.note).put("at",r.at))}}).toString()
  require(raw.toByteArray(Charsets.UTF_8).size<=PhoneReviewPolicy.MAX_BYTES);return raw
 }
 fun decode(raw:String):List<PhoneReviewRecord>{
  require(raw.toByteArray(Charsets.UTF_8).size<=PhoneReviewPolicy.MAX_BYTES)
  val root=JSONObject(raw);require(root.getString("format")=="msa.cyberdeck.phone-review.v1")
  val list=root.getJSONArray("records");require(list.length()<=128)
  return (0 until list.length()).map{val r=list.getJSONObject(it);PhoneReviewRecord(r.getString("key"),r.getString("fingerprint"),r.getBoolean("reviewed"),r.getString("note"),r.getString("at"))}.also{PhoneReviewPolicy.validate(it)}
 }
}
fun phoneReviewDiagnostic(snapshot:PhoneSnapshot,records:List<PhoneReviewRecord>):DiagnosticResult {
 PhoneReviewPolicy.validate(records)
 val items=PhoneReviewPolicy.items(snapshot.device,snapshot.checks);val byKey=records.associateBy{it.key}
 fun build(noteLimit:Int)=DiagnosticResult("phone-review",snapshot.device,Instant.now().toString(),"Plan de revisión manual · ${items.count{PhoneReviewPolicy.status(it,byKey[it.key])==ReviewMark.CURRENT}} marcas vigentes de ${items.size}; no es una evaluación global de seguridad",listOf(
  "Fecha de la lectura Android" to snapshot.at,"Método" to "Reglas locales sobre APIs Android y anotaciones del usuario. Marcar no cambia ajustes ni verifica una corrección. Fechas del móvil, sin sellado certificado. Notas abreviadas a $noteLimit caracteres.")+
  items.map{item->val record=byKey[item.key];item.check.title to "Observación: ${item.check.value}\nContexto: ${item.check.explanation}\nSiguiente comprobación: ${item.advice}\nMarca: ${PhoneReviewPolicy.status(item,record).label}\nAnotada: ${record?.at?:"Sin anotación"}\nNota del usuario: ${record?.note?.take(noteLimit).orEmpty()}\nHuella al anotar: ${record?.fingerprint?:"Sin anotación"}\nHuella de la observación actual: ${item.fingerprint}"})
 var limit=200;var result=build(limit)
 while(result.toEvidenceJson().toByteArray(Charsets.UTF_8).size>60000 && limit>0){limit/=2;result=build(limit)}
 require(result.toEvidenceJson().toByteArray(Charsets.UTF_8).size<=65536){"El plan supera el tamaño de diagnóstico compatible"};return result
}
