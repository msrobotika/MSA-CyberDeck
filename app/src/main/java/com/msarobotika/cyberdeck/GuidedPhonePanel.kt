package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable fun GuidedPhonePanel(ui:PhoneUiModel,snapshot:PhoneSnapshot,busy:Boolean,refresh:()->Unit,save:(DiagnosticResult)->Unit,settings:(String)->Unit){
 val selected by ui.selected.collectAsState()
 val before by ui.before.collectAsState();val observedAt by ui.observedAt.collectAsState()
 val showAll by ui.showAll.collectAsState()
 val priorities=PhoneBriefPolicy.priorities(snapshot.checks)
 fun open(check:PhoneCheck){ui.open(check,snapshot.at)}
 Column(Modifier.fillMaxWidth().testTag("phone-guided"),verticalArrangement=Arrangement.spacedBy(12.dp)){
  if(selected==null){
   val review=snapshot.checks.count{it.tone==PhoneTone.REVIEW};val unknown=snapshot.checks.count{it.tone==PhoneTone.UNKNOWN}
   Card(Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text("TU REVISIÓN",color=DeckCyan,style=MaterialTheme.typography.labelLarge)
    Text(when{review>0->"$review ajustes para revisar";unknown>0->"$unknown datos por comprobar";else->"Sin ajustes destacados"},style=MaterialTheme.typography.headlineSmall)
    Text("${snapshot.device} · ${displayTime(snapshot.at)}",style=MaterialTheme.typography.bodySmall)
    Text("${snapshot.checks.size} observaciones de Android",style=MaterialTheme.typography.bodySmall)
    DeckDisclosure("Qué incluye la revisión","Ajustes observados mediante APIs públicas de Android. Las prioridades usan reglas locales. No comprueba malware ni garantiza ausencia de ataques. Un dato no disponible no se interpreta como amenaza.")
    OutlinedButton(enabled=!busy,onClick=refresh){Text("Actualizar")}
   }}
   Text(if(priorities.isEmpty())"¿Qué quieres comprobar?" else "Empieza por aquí",style=MaterialTheme.typography.titleMedium)
   val visible=if(showAll)snapshot.checks.sortedBy{when(it.tone){PhoneTone.REVIEW->0;PhoneTone.UNKNOWN->1;else->2}} else (priorities.ifEmpty{snapshot.checks}).take(3)
   visible.forEach{check->Card(onClick={open(check)},enabled=!busy,modifier=Modifier.fillMaxWidth().testTag("guided-check-${PhoneBriefPolicy.key(check)}")){
    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
     Text(check.title,style=MaterialTheme.typography.titleMedium)
     Text(check.value,color=if(check.tone==PhoneTone.REVIEW)DeckAmber else DeckCyan)
     Text(PhoneBriefPolicy.label(check.tone),style=MaterialTheme.typography.bodySmall)
     Text("Ver qué hacer →",style=MaterialTheme.typography.labelLarge,color=DeckCyan)
    }
   }}
   TextButton(onClick={ui.showAll(!showAll)},modifier=Modifier.testTag("guided-show-all")){Text(if(showAll)"Ver solo lo principal" else "Ver todas las observaciones · ${snapshot.checks.size}")}
   OutlinedButton(enabled=!busy,onClick={save(snapshot.diagnostic())},modifier=Modifier.fillMaxWidth()){Text("Guardar chequeo")}
  }else{
   val current=snapshot.checks.firstOrNull{PhoneBriefPolicy.key(it)==selected}
   TextButton(onClick={ui.back()},modifier=Modifier.testTag("guided-back")){Text("← Volver a mi revisión")}
   if(current==null){Text("Android no ha devuelto esta observación en la nueva lectura.");Button(enabled=!busy,onClick=refresh){Text("Comprobar de nuevo")}}
   else{
    Card(Modifier.fillMaxWidth().testTag("guided-next-step")){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
     Text(current.title,style=MaterialTheme.typography.headlineSmall)
     Text(current.value,style=MaterialTheme.typography.titleLarge,color=DeckCyan)
     Text(PhoneBriefPolicy.label(current.tone),style=MaterialTheme.typography.bodySmall)
     Text("Qué hacer ahora",style=MaterialTheme.typography.titleMedium)
     val item=PhoneReviewPolicy.items(snapshot.device,listOf(current)).single()
     Text(item.advice.substringBefore(". ").trimEnd('.')+".",style=MaterialTheme.typography.bodyMedium)
     current.settings?.let{action->Button(enabled=!busy,onClick={settings(action)},modifier=Modifier.fillMaxWidth().testTag("guided-open-settings")){Text(PhoneBriefPolicy.action(current))}}
     Text("Después de revisar el ajuste, vuelve y pulsa «Comprobar de nuevo».",style=MaterialTheme.typography.bodySmall)
     OutlinedButton(enabled=!busy,onClick=refresh,modifier=Modifier.fillMaxWidth().testTag("guided-recheck")){Text(if(busy)"Leyendo Android…" else "Comprobar de nuevo")}
     if(snapshot.at!=observedAt){
      Text("Nueva lectura · ${displayTime(snapshot.at)}",color=DeckMint,style=MaterialTheme.typography.labelLarge)
      Text(if(current.value==before)"El valor observado no ha cambiado." else "Antes: $before\nAhora: ${current.value}",modifier=Modifier.testTag("guided-recheck-result"))
     }
     DeckDisclosure("Dato técnico y límites",current.explanation+"\n\n"+item.advice+"\n\nCiberDeck no cambia la configuración por ti. Una nueva lectura no verifica una corrección ni descarta otros riesgos.")
    }}
   }
  }
 }
}
