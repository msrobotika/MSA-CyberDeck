package com.msarobotika.cyberdeck

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.time.Instant
import java.util.UUID

class AuditWorkStore(context:Context){
 private companion object{val LOCK=Any()}
 private val index=AtomicFile(File(context.filesDir,"audit-work/index.json").apply{parentFile?.mkdirs()})
 fun read():AuditWorkState=synchronized(LOCK){
  if(!index.baseFile.exists() && !File(index.baseFile.path+".bak").exists())return@synchronized AuditWorkState()
  index.openRead().use{require(it.channel.size()<=AuditWorkPolicy.MAX_BYTES){"Archivo de trabajos demasiado grande"};AuditWorkCodec.decode(it.bufferedReader().readText())}
 }
 private fun update(change:(AuditWorkState)->AuditWorkState):AuditWorkState=synchronized(LOCK){
  val next=change(read());val bytes=AuditWorkCodec.encode(next).toByteArray(Charsets.UTF_8)
  val stream=index.startWrite();try{stream.write(bytes);index.finishWrite(stream)}catch(t:Throwable){index.failWrite(stream);throw t};next
 }
 fun create(name:String,scope:String,targets:String)=update{state->val w=AuditWork(UUID.randomUUID().toString(),name.trim(),scope,targets,"",Instant.now().toString());state.copy(activeId=w.id,works=state.works+w)}
 fun activate(id:String?)=update{it.copy(activeId=id)}
 fun edit(work:AuditWork)=update{state->val old=state.works.firstOrNull{it.id==work.id}?:error("Trabajo no encontrado");state.copy(works=state.works.map{if(it.id==work.id)old.copy(name=work.name.trim(),scope=work.scope,targets=work.targets,notes=work.notes) else it})}
 fun archive(id:String,archived:Boolean)=update{state->require(state.works.any{it.id==id});state.copy(activeId=if(archived && state.activeId==id)null else state.activeId,works=state.works.map{if(it.id==id)it.copy(archived=archived) else it})}
 fun attach(id:String,entries:List<Evidence>)=update{state->require(state.works.any{it.id==id}){"Trabajo no encontrado"};state.copy(works=state.works.map{if(it.id==id)AuditWorkPolicy.attach(it,entries) else it})}
 fun detach(id:String,ids:Set<String>)=update{state->require(state.works.any{it.id==id});state.copy(works=state.works.map{if(it.id==id)it.copy(evidence=it.evidence.filterNot{ref->ref.id in ids}) else it})}
 fun report(id:String,store:EvidenceStore):String=synchronized(LOCK){
  val work=read().works.firstOrNull{it.id==id}?:error("Trabajo no encontrado")
  val available=store.list();val valid=work.evidence.mapNotNull{ref->available.firstOrNull{AuditWorkPolicy.matches(ref,it)}}
  val missing=work.evidence.filter{ref->valid.none{it.id==ref.id}}
  require(valid.filter{it.name.startsWith("diagnostico-") && it.bytes<=65536}.sumOf{it.bytes}<=1024*1024){"El informe contiene más de 1 MiB de diagnósticos. Reparte las referencias entre trabajos; las copias se conservan."}
  val reportContext=ReportContext(work.name,work.scope,work.notes)
  buildString{
   append("# Trabajo de auditoría local\n\nID: ${work.id}\n\nCreado: ${work.createdAt}\n\nEstado: ${if(work.archived)"Archivado" else "Abierto"}\n\n")
   if(work.targets.isNotBlank())append("## Objetivos declarados\n\n"+work.targets.replace("\r","").lineSequence().joinToString("\n"){"> "+it.replace("```","ʼʼʼ")}+"\n\n")
   append("${work.evidence.size} referencias fijadas; ${valid.size} coinciden con el índice actual. Cada archivo se verifica por SHA-256 al generar su sección.\n\n")
   missing.forEach{ref->append("Referencia ${ref.id}: AUSENTE O NO COINCIDE con el índice actual; SHA-256 fijado ${ref.sha256}. No se incluye su contenido.\n\n")}
   val summaries=valid.asReversed().mapNotNull{entry->runCatching{store.diagnostic(entry)}.getOrNull()}.take(20)
   if(summaries.isNotEmpty()){
    fun flat(value:String)=value.replace("\r","").replace("\n"," ").replace("```","ʼʼʼ").take(400)
    append("## Resumen de diagnósticos comprobados\n\nHasta 20 resultados recientes; son observaciones guardadas, no una conclusión automática de seguridad.\n\n```text\n")
    summaries.forEach{r->append("${flat(r.checkedAt)} | ${flat(r.kind)} | ${flat(r.target)}\n${flat(r.summary)}\n\n")}
    append("```\n\n")
   }
   append(store.report(valid,reportContext))
  }
 }
}
