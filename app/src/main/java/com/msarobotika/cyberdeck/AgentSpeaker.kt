package com.msarobotika.cyberdeck

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow

/** Explicit playback only. Selects an installed Spanish voice declared non-network by the engine. */
class AgentSpeaker(context:Context):AutoCloseable {
 val ready=MutableStateFlow(false);val speaking=MutableStateFlow(false);val status=MutableStateFlow("Preparando voz del sistema…")
 @Volatile private var closed=false
 private var engine:TextToSpeech?=null
 init{runCatching{TextToSpeech(context.applicationContext){code->Handler(Looper.getMainLooper()).post{initialize(code)}}}.onSuccess{engine=it}.onFailure{status.value="Voz no disponible; el resumen sigue en pantalla."}}
 private fun initialize(code:Int){
  if(closed)return
  val tts=engine?:return
  if(code!=TextToSpeech.SUCCESS){status.value="Voz no disponible; el resumen sigue en pantalla.";return}
  val voice=runCatching{tts.voices?.filter{it.locale.language=="es" && !it.isNetworkConnectionRequired && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty()}?.sortedBy{if(it.locale.country=="ES")0 else 1}?.firstOrNull()}.getOrNull()
  if(voice==null || runCatching{tts.setVoice(voice)}.getOrDefault(TextToSpeech.ERROR)!=TextToSpeech.SUCCESS){status.value="No hay una voz española local instalada. Puedes leer el resumen.";return}
  tts.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
   override fun onStart(id:String?){if(!closed){speaking.value=true;status.value="Leyendo el resumen…"}}
   override fun onDone(id:String?){if(!closed){speaking.value=false;status.value="Lectura finalizada."}}
   @Deprecated("Android callback") override fun onError(id:String?){if(!closed){speaking.value=false;status.value="No se pudo leer el resumen."}}
   override fun onStop(id:String?,interrupted:Boolean){if(!closed){speaking.value=false;status.value="Lectura detenida."}}
  })
  ready.value=true;status.value="Voz española disponible."
 }
 fun speak(text:String){
  if(closed || !ready.value){status.value="Voz local no disponible; consulta el resumen en pantalla.";return}
  if(text.isBlank() || text.length>1500){status.value="Resumen de voz no compatible.";return}
  if(runCatching{engine?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"ciberdeck-agent")}.getOrNull()!=TextToSpeech.SUCCESS)status.value="No se pudo iniciar la lectura."
 }
 fun stop(){runCatching{engine?.stop()};speaking.value=false}
 override fun close(){closed=true;ready.value=false;runCatching{engine?.stop();engine?.shutdown()};engine=null;speaking.value=false}
}
