package com.msarobotika.cyberdeck

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable fun AgentVoiceInput(enabled:Boolean,receive:(String)->Unit){
 var status by rememberSaveable{mutableStateOf("")}
 val launcher=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
  if(result.resultCode!=Activity.RESULT_OK)status="Dictado cancelado. No se ha enviado ninguna tarea."
  else{
   val raw=result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
   runCatching{AgentVoicePolicy.draft(raw)}.onSuccess{receive(it);status="Petición transcrita. Revisa el texto antes de enviarla."}.onFailure{status="No se recibió un dictado válido. Puedes escribir la petición."}
  }
 }
 Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
  OutlinedButton(enabled=enabled,onClick={
   try{launcher.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"es-ES").putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true).putExtra(RecognizerIntent.EXTRA_PROMPT,"¿Qué quieres comprobar con CiberDeck?"))}
   catch(_:Exception){status="No hay un dictado Android disponible. Escribe la petición."}
  },modifier=Modifier.fillMaxWidth().testTag("agent-dictate")){Text("Dictar una petición")}
  if(status.isNotEmpty())Text(status,style=MaterialTheme.typography.bodySmall)
  DeckDisclosure("Cómo funciona la voz","El dictado abre el servicio de voz de Android y solicita preferencia sin conexión; el proveedor puede necesitar Internet. El texto vuelve como borrador y no se ejecuta hasta que lo envías. CiberDeck no escucha en segundo plano. La lectura de resumen usa una voz española instalada que el motor declara local; no lee campos completos, URL ni códigos QR.")
 }
}
