package com.msarobotika.cyberdeck

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One request at a time. Results from a cancelled system picker are drained, never reused. */
class AgentAndroidAccess(activity:ComponentActivity) {
 private var waiting:CompletableDeferred<Any?>?=null
 var systemPending=false; private set
 private var dialog:AlertDialog?=null
 private val permissions=activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){finish(it)}
 private val picker=activity.registerForActivityResult(ActivityResultContracts.OpenDocument()){finish(it)}
 private val creator=activity.registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")){finish(it)}
 private val intent=activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()){finish(it.resultCode)}
 private fun finish(value:Any?){val old=waiting;waiting=null;systemPending=false;old?.complete(value)}
 private suspend fun request(system:Boolean,launch:()->Unit):Any?=withContext(Dispatchers.Main.immediate){
  check(waiting==null){"Hay una solicitud Android anterior pendiente; ciérrala antes de volver a ejecutar"}
  val reply=CompletableDeferred<Any?>();waiting=reply;systemPending=system
  try{try{launch()}catch(e:Exception){waiting=null;systemPending=false;throw e};reply.await()}
  finally{if(!system){if(waiting===reply)waiting=null;dialog?.dismiss();dialog=null};if(reply.isActive)reply.cancel()}
 }
 suspend fun permissions(names:Array<String>){request(true){permissions.launch(names)}}
 suspend fun open(types:Array<String> = arrayOf("*/*")):Uri = (request(true){picker.launch(types)} as? Uri)?:throw CancellationException("Selección de archivo cancelada")
 suspend fun create(name:String):Uri = (request(true){creator.launch(name)} as? Uri)?:throw CancellationException("Destino de exportación cancelado")
 suspend fun intent(value:Intent){request(true){intent.launch(value)}}
 suspend fun choose(activity:ComponentActivity,title:String,labels:List<String>):Int {
  require(labels.isNotEmpty()){"No hay elementos disponibles"}
  return request(false){dialog=AlertDialog.Builder(activity).setTitle(title).setItems(labels.toTypedArray()){_,i->finish(i)}.setNegativeButton("Cancelar"){_,_->finish(null)}.setOnCancelListener{finish(null)}.show()} as? Int?:throw CancellationException("Selección cancelada")
 }
 suspend fun confirm(activity:ComponentActivity,title:String,text:String):Boolean = request(false){dialog=AlertDialog.Builder(activity).setTitle(title).setMessage(text).setPositiveButton("Confirmar"){_,_->finish(true)}.setNegativeButton("Cancelar"){_,_->finish(false)}.setOnCancelListener{finish(false)}.show()}==true
 suspend fun text(activity:ComponentActivity,title:String,limit:Int):String {
  var input:EditText?=null
  return (request(false){input=EditText(activity).apply{filters=arrayOf(android.text.InputFilter.LengthFilter(limit))};dialog=AlertDialog.Builder(activity).setTitle(title).setView(input).setPositiveButton("Continuar"){_,_->finish(input!!.text.toString())}.setNegativeButton("Cancelar"){_,_->finish(null)}.setOnCancelListener{finish(null)}.show()} as? String)?.trim()?.takeIf{it.isNotBlank()}?:throw CancellationException("Entrada cancelada")
 }
 fun close(){dialog?.dismiss();dialog=null;waiting?.cancel();waiting=null;systemPending=false}
}
