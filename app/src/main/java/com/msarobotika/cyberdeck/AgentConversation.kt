package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

data class AgentExchange(val id:String,val request:String,val answer:String,val status:String,val at:String)
/** Bounded session memory of actual executions, never a generated chat or an execution queue. */
object AgentConversationPolicy {
 fun record(previous:List<AgentExchange>,session:AgentSession):List<AgentExchange>{
  val run=session.run?:return previous
  if(session.running || session.planning)return previous
  val summary=AgentSummary.from(session)?:return previous
  val answer=AgentAnswerPolicy.primary(session)?.result?.summary?:summary.title
  val row=AgentExchange(run.startedAt,session.request.take(800),answer.take(600),summary.title,run.finishedAt)
  return (listOf(row)+previous.filterNot{it.id==row.id}).take(5)
 }
 fun shouldSpeak(session:AgentSession,enabled:Boolean,ready:Boolean,foreground:Boolean,lastId:String):Boolean {
  val run=session.run?:return false
  val plan=session.plan?:return false
  return enabled && ready && foreground && !session.running && !session.planning && session.error.isBlank() && session.notice.isBlank() && run.startedAt!=lastId && run.steps.size==plan.steps.size && run.steps.none{it.awaiting || it.error.isNotBlank()}
 }
}
@Composable fun AgentConversationPanel(rows:List<AgentExchange>,use:(String)->Unit,clear:()->Unit,enabled:Boolean){
 var expanded by rememberSaveable{mutableStateOf(false)}
 if(rows.isNotEmpty()){
  TextButton(onClick={expanded=!expanded},modifier=Modifier.testTag("agent-recent")){Text(if(expanded)"Cerrar conversación reciente" else "Conversación reciente · ${rows.size}")}
  if(expanded){
   Text("Solo esta sesión · hasta cinco tareas. Son resultados anteriores, no nuevas lecturas.",style=MaterialTheme.typography.bodySmall)
   rows.forEach{row->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
    Text("TÚ · ${displayTime(row.at)}",color=DeckCyan,style=MaterialTheme.typography.labelSmall)
    Text(row.request,style=MaterialTheme.typography.bodyMedium,maxLines=3)
    Text("CIBERDECK · ${row.status}",color=DeckMint,style=MaterialTheme.typography.labelSmall)
    Text(row.answer,style=MaterialTheme.typography.bodyMedium,maxLines=4)
    TextButton(enabled=enabled,onClick={use(row.request)},modifier=Modifier.testTag("agent-reuse-${row.id}")){Text("Usar esta petición como borrador")}
   }}}
   TextButton(enabled=enabled,onClick=clear,modifier=Modifier.testTag("agent-clear-recent")){Text("Vaciar conversación de esta sesión")}
  }
 }
}

@Composable fun AgentSpeechPreference(checked:Boolean,change:(Boolean)->Unit){
 Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
  Switch(checked=checked,onCheckedChange=change,modifier=Modifier.testTag("agent-auto-voice"))
  Column(Modifier.weight(1f)){Text("Responder en voz alta",style=MaterialTheme.typography.titleSmall);Text("Opcional · al terminar · voz del sistema",style=MaterialTheme.typography.bodySmall)}
 }
}
