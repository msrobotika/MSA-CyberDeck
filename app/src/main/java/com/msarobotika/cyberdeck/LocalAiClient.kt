package com.msarobotika.cyberdeck

import android.content.*
import android.os.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

data class AiRequest(val question:String,val context:String,val model:String,val modelHash:String,val at:String,val sourceId:String="",val sourceHash:String="",val sourceVerifiedAt:String="")
data class LocalAiState(val connected:Boolean=false,val runtime:Boolean=false,val loaded:Boolean=false,val busy:Boolean=false,
 val phase:String="Sin modelo cargado",val error:String="",val output:String="",val elapsed:Long=0,val chunks:Int=0)
class LocalAiClient(context:Context,private val state:MutableStateFlow<LocalAiState>) {
 private val app=context.applicationContext
 private var remote:Messenger?=null
 private var bound=false
 private val replies=Messenger(object:Handler(Looper.getMainLooper()){
  override fun handleMessage(msg:Message){if(msg.what==AiWire.STATE){val d=msg.data;state.update{it.copy(connected=true,runtime=d.getBoolean("runtime"),loaded=d.getBoolean("loaded"),busy=d.getBoolean("busy"),phase=d.getString("phase").orEmpty(),error=d.getString("error").orEmpty(),output=if(d.getInt("chunks")>0)d.getString("output").orEmpty() else it.output,elapsed=d.getLong("elapsed"),chunks=d.getInt("chunks"))}}}
 })
 private val connection=object:ServiceConnection {
  override fun onServiceConnected(name:ComponentName,binder:IBinder){remote=Messenger(binder);send(AiWire.PING)}
  override fun onServiceDisconnected(name:ComponentName){remote=null;state.update{it.copy(connected=false,loaded=false,busy=false,phase="Motor desconectado",error="El proceso de IA terminó. Las otras herramientas siguen disponibles.")}}
  override fun onBindingDied(name:ComponentName){onServiceDisconnected(name)}
 }
 fun connect(){if(bound)return;state.update{it.copy(busy=true,error="",phase="Comprobando motor local")};bound=app.bindService(Intent(app,LocalAiService::class.java),connection,Context.BIND_AUTO_CREATE);if(!bound)state.update{it.copy(busy=false,error="No se pudo iniciar el motor local")}}
 fun disconnect(){remote=null;if(bound){runCatching{app.unbindService(connection)};bound=false};state.update{it.copy(connected=false,loaded=false,busy=false,phase="Modelo descargado de memoria",error=if(it.busy && it.loaded)"La generación se interrumpió al salir" else it.error)}}
 fun send(what:Int,prompt:String=""){val endpoint=remote?:return;try{val msg=Message.obtain(null,what);msg.replyTo=replies;msg.data=Bundle().apply{putString("prompt",prompt)};if(what in listOf(AiWire.LOAD,AiWire.ASK,AiWire.PLAN,AiWire.UNLOAD))state.update{it.copy(busy=true,error="",output=if(what in listOf(AiWire.ASK,AiWire.PLAN))"" else it.output)};endpoint.send(msg)}catch(e:RemoteException){state.update{it.copy(loaded=false,busy=false,connected=false,error="El motor dejó de responder; vuelve a conectarlo")}}}
}
