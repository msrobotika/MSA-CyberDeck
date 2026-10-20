package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable fun AgentTeamPanel(session:AgentSession){
 Card(Modifier.fillMaxWidth().testTag("agent-team")){
  Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("COORDINADOR · EQUIPO LOCAL",style=MaterialTheme.typography.labelLarge,color=DeckMint)
   Text(if(session.plan==null)"Una misión, los especialistas necesarios" else "${AgentTeam.states(session).size} especialistas asignados",style=MaterialTheme.typography.titleMedium)
   Text("El coordinador reparte las herramientas del plan. Se ejecutan por turnos; comparten resultados fechados.",style=MaterialTheme.typography.bodySmall)
   AgentTeam.states(session).forEach{state->
    HorizontalDivider()
    Text(state.worker.label,style=MaterialTheme.typography.titleSmall,color=if(session.running && AgentTeam.workerFor(session.activeTool?:AgentTool.MODULES)==state.worker && session.activeTool!=null)DeckMint else DeckCyan)
    Text(state.status,style=MaterialTheme.typography.bodySmall,modifier=Modifier.testTag("agent-worker-${state.worker.name.lowercase()}"))
    Text(state.tools.joinToString(" · "){it.label},style=MaterialTheme.typography.bodySmall)
   }
   session.run?.takeIf{!session.running}?.let{run->DeckDisclosure("Balance del coordinador · sin IA",AgentTeam.review(run))}
   DeckDisclosure("Cómo trabaja el equipo","Especialistas de herramientas, no seis modelos independientes. Un único modelo local prepara planes e interpretaciones cuando lo solicitas; las tareas directas funcionan sin cargarlo. El coordinador conserva el orden y detiene la misión ante un error o cancelación. No verifica por sí solo que una interpretación sea cierta. "+AiGenerationPolicy.DESCRIPTION)
  }
 }
}
