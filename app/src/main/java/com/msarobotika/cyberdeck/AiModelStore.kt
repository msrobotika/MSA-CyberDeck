package com.msarobotika.cyberdeck

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID

data class AiModelInfo(val file:String,val name:String,val bytes:Long,val sha256:String,val importedAt:String)
class AiModelStore(private val context:Context) {
 private val dir=File(context.filesDir,"local-ai").apply{mkdirs()}
 private val index=AtomicFile(File(dir,"model.json"))
 fun current():AiModelInfo?=runCatching{
  val j=JSONObject(index.openRead().bufferedReader().use{it.readText()})
  val file=j.getString("file")
  require(file.matches(Regex("model-[a-f0-9-]{36}\\.litertlm")))
  val m=AiModelInfo(file,j.getString("name").take(120),j.getLong("bytes"),j.getString("sha256"),j.getString("importedAt"))
  require(m.sha256.matches(Regex("[a-f0-9]{64}")) && path(m).length()==m.bytes);m
 }.getOrNull()
 fun path(info:AiModelInfo):File {
  require(info.file.matches(Regex("model-[a-f0-9-]{36}\\.litertlm")))
  return File(dir,info.file)
 }
 fun import(uri:Uri,progress:(Long)->Unit):AiModelInfo {
  var name=if(uri.scheme=="file")uri.lastPathSegment.orEmpty().take(120).ifBlank{"modelo.litertlm"} else "modelo.litertlm";var knownSize:Long?=null
  context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE),null,null,null)?.use{c->if(c.moveToFirst()){name=c.getString(0).orEmpty().take(120);if(!c.isNull(1))knownSize=c.getLong(1)}}
  require(name.endsWith(".litertlm",true)){"Selecciona el archivo .litertlm, no GGUF, TASK, ZIP ni APK"}
  knownSize?.let{require(it in 16384..AiPolicy.MAX_MODEL_BYTES){"El modelo debe ocupar entre 16 KiB y 2 GiB"};require(dir.usableSpace>it+128*1024*1024){"No hay espacio suficiente para importar sin reemplazar el modelo actual"}}
  val previous=current();val target=File(dir,"model-${UUID.randomUUID()}.litertlm")
  try{
   val fp=context.contentResolver.openInputStream(uri)?.use{input->target.outputStream().use{output->AiPolicy.copyModel(input,output){size->require(dir.usableSpace>128*1024*1024){"Espacio insuficiente"};progress(size)}}}?:error("No se pudo leer el modelo seleccionado")
   val m=AiModelInfo(target.name,name,fp.bytes,fp.sha256,Instant.now().toString())
   val json=JSONObject().put("file",m.file).put("name",m.name).put("bytes",m.bytes).put("sha256",m.sha256).put("importedAt",m.importedAt).toString()
   val stream=index.startWrite();try{stream.write(json.toByteArray());index.finishWrite(stream)}catch(t:Throwable){index.failWrite(stream);throw t}
   previous?.let{path(it).delete()};return m
  }catch(t:Throwable){target.delete();throw t}
 }
 fun delete(){val previous=current();previous?.let{val file=path(it);check(!file.exists() || file.delete()){"No se pudo borrar la copia privada; inténtalo de nuevo"}};index.delete()}
}
