package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable fun AgentTeamPanel(session:AgentSession){
 var details by rememberSaveable{mutableStateOf(false)}
 Card(Modifier.fillMaxWidth().testTag("agent-team")){
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("COORDINADOR · EQUIPO LOCAL",style=MaterialTheme.typography.labelLarge,color=DeckMint)
   Text(if(session.plan==null)"Elige una tarea para empezar" else "${AgentTeam.states(session).size} especialistas asignados",style=MaterialTheme.typography.titleMedium)
   AgentTeam.states(session).forEach{state->
    HorizontalDivider()
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
     Text(state.worker.label,modifier=Modifier.weight(0.35f),style=MaterialTheme.typography.titleSmall,color=if(session.running && session.activeTool?.let{AgentTeam.workerFor(it)}==state.worker)DeckMint else DeckCyan)
     Text(state.status,modifier=Modifier.weight(0.65f).testTag("agent-worker-${state.worker.name.lowercase()}"),style=MaterialTheme.typography.bodySmall)
    }
    if(details)Text(state.tools.joinToString(" · "){it.label},style=MaterialTheme.typography.bodySmall)
   }
   if(session.plan!=null)TextButton(onClick={details=!details},modifier=Modifier.testTag("agent-team-details")){Text(if(details)"Ocultar tareas de especialistas" else "Ver tareas de especialistas")}
   session.run?.takeIf{!session.running}?.let{run->DeckDisclosure("Balance del coordinador · sin IA",AgentTeam.review(run))}
   DeckDisclosure("Cómo trabaja el equipo","Especialistas de herramientas, no seis modelos independientes. Un único modelo local prepara planes e interpretaciones cuando lo solicitas; las tareas directas funcionan sin cargarlo. El coordinador conserva el orden y detiene la misión ante un error o cancelación. No verifica por sí solo que una interpretación sea cierta. "+AiGenerationPolicy.DESCRIPTION)
  }
 }
}
