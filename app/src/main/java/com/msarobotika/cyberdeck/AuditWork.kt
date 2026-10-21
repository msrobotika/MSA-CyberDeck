package com.msarobotika.cyberdeck

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

data class WorkEvidence(val id:String,val sha256:String,val bytes:Long,val name:String)
data class AuditWork(val id:String,val name:String,val scope:String,val targets:String,val notes:String,val createdAt:String,val archived:Boolean=false,val evidence:List<WorkEvidence> = emptyList())
data class AuditWorkState(val activeId:String?=null,val works:List<AuditWork> = emptyList())
object AuditWorkPolicy {
 const val MAX_BYTES=2*1024*1024
 fun validId(id:String)=id.length==36 && runCatching{UUID.fromString(id).toString()==id}.getOrDefault(false)
 fun validate(state:AuditWorkState){
  require(state.works.size<=50 && state.works.map{it.id}.distinct().size==state.works.size){"Límite de 50 trabajos o identificadores repetidos"}
  require(state.works.sumOf{it.evidence.size}<=2000){"Límite de 2000 referencias entre trabajos"}
  state.works.forEach{work->
   require(validId(work.id) && work.name.isNotBlank() && work.name.length<=160 && work.scope.length<=2000 && work.targets.length<=2000 && work.notes.length<=6000){"Revisa el nombre o el tamaño de los campos"}
   Instant.parse(work.createdAt)
   require(work.evidence.size<=500 && work.evidence.map{it.id}.distinct().size==work.evidence.size){"Hasta 500 evidencias distintas por trabajo"}
   work.evidence.forEach{require(validId(it.id) && it.sha256.matches(Regex("[a-f0-9]{64}")) && it.bytes>=0 && it.name.length<=512){"Referencia de evidencia inválida"}}
  }
  require(state.activeId==null || state.works.any{it.id==state.activeId && !it.archived}){"El trabajo activo debe existir y no estar archivado"}
 }
 fun attach(work:AuditWork,entries:List<Evidence>):AuditWork {
  require(!work.archived){"Restaura el trabajo antes de asociar evidencias"}
  val added=entries.map{WorkEvidence(it.id,it.sha256.lowercase(),it.bytes,it.name.take(512))}
  require(added.groupBy{it.id}.values.all{group->group.map{it.sha256 to it.bytes}.distinct().size==1}){"El lote contiene referencias contradictorias"}
  val previous=work.evidence.associateBy{it.id}
  added.forEach{ref->previous[ref.id]?.let{require(it.sha256==ref.sha256 && it.bytes==ref.bytes){"La evidencia ya está asociada con otra huella; no se reemplaza la referencia"}}}
  return work.copy(evidence=(work.evidence+added).distinctBy{it.id})
 }
 fun matches(ref:WorkEvidence,entry:Evidence)=ref.id==entry.id && ref.sha256.equals(entry.sha256,true) && ref.bytes==entry.bytes
}
object AuditWorkCodec {
 fun encode(state:AuditWorkState):String {
  AuditWorkPolicy.validate(state)
  val result=JSONObject().put("format","msa.cyberdeck.work.v1").put("activeId",state.activeId?:JSONObject.NULL).put("works",JSONArray().apply{state.works.forEach{w->put(JSONObject().put("id",w.id).put("name",w.name).put("scope",w.scope).put("targets",w.targets).put("notes",w.notes).put("createdAt",w.createdAt).put("archived",w.archived).put("evidence",JSONArray().apply{w.evidence.forEach{put(JSONObject().put("id",it.id).put("sha256",it.sha256).put("bytes",it.bytes).put("name",it.name))}}))}}).toString()
  require(result.toByteArray(Charsets.UTF_8).size<=AuditWorkPolicy.MAX_BYTES){"El archivo de trabajos supera 2 MiB"};return result
 }
 fun decode(raw:String):AuditWorkState {
  require(raw.toByteArray(Charsets.UTF_8).size<=AuditWorkPolicy.MAX_BYTES)
  val root=JSONObject(raw);require(root.getString("format")=="msa.cyberdeck.work.v1")
  val array=root.getJSONArray("works");require(array.length()<=50)
  val works=(0 until array.length()).map{i->val w=array.getJSONObject(i);val refs=w.getJSONArray("evidence");require(refs.length()<=500)
   AuditWork(w.getString("id"),w.getString("name"),w.getString("scope"),w.getString("targets"),w.getString("notes"),w.getString("createdAt"),w.getBoolean("archived"),(0 until refs.length()).map{j->val ref=refs.getJSONObject(j);WorkEvidence(ref.getString("id"),ref.getString("sha256"),ref.getLong("bytes"),ref.getString("name"))})}
  return AuditWorkState(if(root.isNull("activeId"))null else root.getString("activeId"),works).also{AuditWorkPolicy.validate(it)}
 }
}
