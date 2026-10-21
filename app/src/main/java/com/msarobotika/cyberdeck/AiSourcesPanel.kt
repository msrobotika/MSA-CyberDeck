package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable fun AiSourcesPanel(packet:AiSourcePacket,enabled:Boolean,save:(DiagnosticResult)->Unit){
 Card(Modifier.fillMaxWidth().testTag("ai-sources")){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Text("FUENTES · SIN INTERPRETACIÓN",color=DeckMint,style=MaterialTheme.typography.labelLarge)
  if(packet.phoneAt.isNotBlank()){
   Text("Android · ${displayTime(packet.phoneAt)}",style=MaterialTheme.typography.bodySmall)
   packet.phoneFields.take(3).forEach{(key,value)->Text("$key: $value",style=MaterialTheme.typography.bodyMedium)}
   if(packet.phoneFields.size>3)DeckDisclosure("Más datos de Android",packet.phoneFields.drop(3).joinToString("\n"){"${it.first}: ${it.second}"})
  }else Text("Sin estado Android adjunto",style=MaterialTheme.typography.bodySmall)
  Text(AiSources.nature(packet),style=MaterialTheme.typography.bodySmall)
  if(packet.sourceKind.isNotBlank()){
   Text("${packet.sourceTarget} · ${displayTime(packet.sourceAt)}",style=MaterialTheme.typography.bodySmall)
   DeckDisclosure("Ver fragmento de la fuente",packet.excerpt)
  }
  DeckDisclosure("Entorno y objetivo de esta consulta",packet.environment)
  OutlinedButton(enabled=enabled,onClick={save(AiSources.report(packet))},modifier=Modifier.testTag("ai-save-sources")){Text("Guardar fuentes")}
 }}
}
