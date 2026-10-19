package com.msarobotika.cyberdeck

import android.app.Service
import android.app.ActivityManager
import android.content.Intent
import android.os.*
import com.google.ai.edge.litertlm.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object AiWire {const val PING=1;const val LOAD=2;const val ASK=3;const val STOP=4;const val UNLOAD=5;const val PLAN=6;const val STATE=10}

/** App-private worker process. No tools, network requests, shell or autonomous device actions. */
class LocalAiService:Service() {
 private val main=Handler(Looper.getMainLooper())
 private val worker=Executors.newSingleThreadExecutor()
 private var client:Messenger?=null
 private var engine:Engine?=null
 @Volatile private var conversation:Conversation?=null
 private var busy=false
 private var runtime=false
 private var text=""
 private var started=0L
 private var chunks=0
 private val finalized=AtomicBoolean(true)
 private var watchdog:Runnable?=null
 private fun report(phase:String,error:String="") {val message=android.os.Message.obtain(null,AiWire.STATE);message.data=Bundle().apply{putString("phase",phase);putString("error",error.take(400));putString("output",text);putBoolean("loaded",engine!=null);putBoolean("runtime",runtime);putBoolean("busy",busy);putLong("elapsed",if(started==0L)0 else SystemClock.elapsedRealtime()-started);putInt("chunks",chunks)};runCatching{client?.send(message)}}
 private fun fail(t:Throwable){main.post{busy=false;report("Error del motor",t.message?:t.javaClass.simpleName)}}
 private fun armWatchdog(){watchdog?.let{main.removeCallbacks(it)};val task=Runnable{if(busy){report("Tiempo de IA agotado","El proceso se libera tras 90 s; puedes volver a cargar el modelo");android.os.Process.killProcess(android.os.Process.myPid())}};watchdog=task;main.postDelayed(task,90000)}
 private val incoming=Messenger(object:Handler(Looper.getMainLooper()) {
  override fun handleMessage(msg:android.os.Message) {
   if(msg.replyTo!=null)client=msg.replyTo
   when(msg.what){
    AiWire.PING->{worker.execute{try{Engine.setNativeMinLogSeverity(LogSeverity.ERROR);main.post{runtime=true;report(if(engine==null)"Motor disponible · modelo sin cargar" else "Modelo cargado")}}catch(t:Throwable){fail(t)}}}
    AiWire.LOAD->{if(busy || engine!=null)return;busy=true;started=SystemClock.elapsedRealtime();report("Cargando modelo local");armWatchdog();worker.execute{
     var candidate:Engine?=null
     try{
      val info=AiModelStore(this@LocalAiService).current()?:error("Importa antes un modelo .litertlm")
      val memory=ActivityManager.MemoryInfo().also{getSystemService(ActivityManager::class.java).getMemoryInfo(it)}
      require(memory.availMem>=1536L*1024*1024){"Hay menos de 1,5 GiB de RAM disponible; libera memoria antes de cargar"}
      val folder=java.io.File(cacheDir,"local-ai").apply{mkdirs()}
      candidate=Engine(EngineConfig(modelPath=AiModelStore(this@LocalAiService).path(info).absolutePath,backend=Backend.CPU(numOfThreads=2),maxNumTokens=2048,cacheDir=folder.absolutePath))
      candidate.initialize();val ready=candidate
      main.post{engine=ready;busy=false;report("Modelo cargado · CPU local")}
     }catch(t:Throwable){runCatching{candidate?.takeIf{it.isInitialized()}?.close()};fail(t)}
    }}
    AiWire.ASK,AiWire.PLAN->{if(busy || engine==null)return
     val prompt=msg.data.getString("prompt").orEmpty();if(prompt.isBlank() || prompt.length>4000){report("Pregunta no válida");return}
     busy=true;text="";chunks=0;started=SystemClock.elapsedRealtime();finalized.set(false);report("Generando en este móvil");armWatchdog()
     val system=if(msg.what==AiWire.PLAN)AgentPolicy.SYSTEM else AiPolicy.CONTEXT_SYSTEM
     worker.execute{try{
      val c=engine!!.createConversation(ConversationConfig(systemInstruction=Contents.of(system),tools=emptyList(),automaticToolCalling=false,extraContext=mapOf("enable_thinking" to false),samplerConfig=SamplerConfig(topK=20,topP=.9,temperature=.2)))
      conversation=c
      val builder=StringBuilder();var last=0L
      c.sendMessageAsync(prompt,object:MessageCallback{
       override fun onMessage(message:com.google.ai.edge.litertlm.Message){synchronized(builder){
        builder.append(message.contents.contents.filterIsInstance<Content.Text>().joinToString(""){it.text});chunks++
        val snapshot=AiPolicy.answerText(builder.toString()).take(AiPolicy.MAX_OUTPUT_CHARS)
        if(builder.length>=AiPolicy.MAX_OUTPUT_CHARS)runCatching{c.cancelProcess()}
        val now=SystemClock.elapsedRealtime();if(now-last>=100){last=now;main.post{text=snapshot;report("Generando en este móvil")}}
       }}
       override fun onDone(){finish("")}
       override fun onError(throwable:Throwable){finish(if(throwable is java.util.concurrent.CancellationException)"Generación detenida" else throwable.message?:"Error al generar")}
       private fun finish(error:String){if(!finalized.compareAndSet(false,true))return;val output=synchronized(builder){AiPolicy.answerText(builder.toString()).take(AiPolicy.MAX_OUTPUT_CHARS)}
        val problem=if(error.isEmpty() && output.isBlank())"El modelo no devolvió una respuesta final. Prueba una pregunta más breve." else error
        worker.execute{runCatching{c.close()};conversation=null;main.post{text=output;busy=false;report(if(problem.isEmpty())"Respuesta generada localmente" else "Generación interrumpida",problem)}}
       }
      })
     }catch(t:Throwable){finalized.set(true);runCatching{conversation?.close()};conversation=null;fail(t)}}
    }
    AiWire.STOP->{runCatching{conversation?.cancelProcess()};report("Deteniendo generación")}
    AiWire.UNLOAD->{if(busy)return;busy=true;report("Liberando modelo");worker.execute{try{engine?.close();main.post{engine=null;busy=false;report("Memoria de IA liberada")}}catch(t:Throwable){fail(t)}}}
   }
  }
 })
 override fun onBind(intent:Intent)=incoming.binder
 override fun onUnbind(intent:Intent):Boolean {stopSelf();return false}
 override fun onDestroy(){watchdog?.let{main.removeCallbacks(it)};worker.shutdownNow();super.onDestroy();android.os.Process.killProcess(android.os.Process.myPid())}
}
