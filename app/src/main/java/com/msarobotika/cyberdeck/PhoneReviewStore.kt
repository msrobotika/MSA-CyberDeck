package com.msarobotika.cyberdeck

import android.content.Context
import android.util.AtomicFile
import java.io.File

class PhoneReviewStore(context:Context){
 private companion object{val LOCK=Any()}
 private val index=AtomicFile(File(context.filesDir,"phone-review/index.json").apply{parentFile?.mkdirs()})
 fun read():List<PhoneReviewRecord> = synchronized(LOCK){
  if(!index.baseFile.exists() && !File(index.baseFile.path+".bak").exists())return@synchronized emptyList()
  index.openRead().use{require(it.channel.size()<=PhoneReviewPolicy.MAX_BYTES){"Archivo de revisión demasiado grande"};PhoneReviewCodec.decode(it.bufferedReader().readText())}
 }
 fun save(item:PhoneReviewItem,reviewed:Boolean,note:String):List<PhoneReviewRecord> = synchronized(LOCK){
  val next=PhoneReviewPolicy.update(read(),item,reviewed,note);val data=PhoneReviewCodec.encode(next).toByteArray(Charsets.UTF_8)
  val stream=index.startWrite();try{stream.write(data);index.finishWrite(stream)}catch(t:Throwable){index.failWrite(stream);throw t};next
 }
}
