package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.update

data class AgentSession(val request:String="",val modelHash:String="",val planning:Boolean=false,val plan:AgentPlan?=null,val raw:String="",val error:String="",val running:Boolean=false,val results:List<AgentStepResult> = emptyList(),val run:AgentRun?=null,val evidenceScope:String="")

@Composable fun AgentPanel(session:DeckSessionModel,blocked:Boolean,send:(Int,String)->Unit,save:(DiagnosticResult)->Unit){
 val context=LocalContext.current;val scope=rememberCoroutineScope()
 val ai by session.ai.collectAsState();val agent by session.agent.collectAsState();val model by session.aiModel.collectAsState()
 var request by rememberSaveable{mutableStateOf("Revisa mi móvil y el estado de su conexión")}
 var work by remember{mutableStateOf<AuditWork?>(null)};var scopeLoaded by remember{mutableStateOf(false)}
 var scopeError by remember{mutableStateOf("")}
 var executionJob by remember{mutableStateOf<Job?>(null)}
 LaunchedEffect(Unit){try{val works=withContext(Dispatchers.IO){AuditWorkStore(context).read()};work=works.works.firstOrNull{it.id==works.activeId};scopeLoaded=true}catch(e:CancellationException){throw e}catch(e:Exception){scopeError="No se pudo leer el alcance local: ${e.message}"}}
 LaunchedEffect(ai.busy,ai.output,ai.error){
  if(session.agent.value.planning && !ai.busy){
   val plan=runCatching{check(ai.error.isBlank()){ai.error};AgentPolicy.parse(ai.output)}
   session.agent.update{it.copy(planning=false,raw=ai.output,plan=plan.getOrNull(),error=plan.exceptionOrNull()?.message.orEmpty())}
  }
 }
 DisposableEffect(Unit){onDispose{session.agent.update{if(it.planning)it.copy(planning=false,error="Plan interrumpido al salir; vuelve a prepararlo") else it}}}
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("AGENTE LOCAL · EXPERIMENTAL",color=DeckCyan,style=MaterialTheme.typography.labelLarge)
  Text("Pide. Revisa el plan. Ejecuta.",style=MaterialTheme.typography.headlineSmall)
  Text("La IA propone pasos; CiberDeck solo ejecuta las herramientas que ves aquí. Los resultados proceden del móvil o de copias verificadas.")
  OutlinedTextField(request,{request=it.take(800)},label={Text("Qué quieres que haga CiberDeck")},modifier=Modifier.fillMaxWidth(),enabled=!blocked && !agent.running && !agent.planning,maxLines=4)
  TextButton(enabled=!blocked && !agent.running && !agent.planning,onClick={request="Revisa mi móvil, lee su conexión y compara mis lecturas guardadas"}){Text("Usar revisión local completa")}
  Button(enabled=ai.connected && ai.loaded && !blocked && !agent.running && !agent.planning && request.isNotBlank(),onClick={
   session.agent.value=AgentSession(request=request,modelHash=model?.sha256.orEmpty(),planning=true)
   session.aiRequest.value=null
   send(AiWire.PLAN,AgentPolicy.prompt(request,model?.name.orEmpty()))
  }){Text("Preparar plan con IA")}
  if(!ai.loaded)Text("Carga arriba el modelo local para preparar el plan.",style=MaterialTheme.typography.bodySmall)
  if(agent.planning){LinearProgressIndicator(Modifier.fillMaxWidth());OutlinedButton(onClick={send(AiWire.STOP,"")}){Text("Detener planificación")}}
  if(agent.error.isNotBlank())Text(agent.error,color=MaterialTheme.colorScheme.error)
  if(scopeError.isNotBlank())Text(scopeError,color=MaterialTheme.colorScheme.error)
  agent.plan?.let{plan->
   Card(Modifier.fillMaxWidth().testTag("agent-plan")){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
    Text("PLAN PROPUESTO · ${plan.steps.size} PASOS",color=DeckMint,style=MaterialTheme.typography.labelLarge)
    Text(agent.request,style=MaterialTheme.typography.bodyMedium)
    plan.steps.forEachIndexed{i,tool->Text("${i+1}. ${tool.label}")}
    Text("Copias: ${work?.name?:if(scopeLoaded)"Todas las evidencias locales" else "Leyendo alcance…"}",style=MaterialTheme.typography.bodySmall)
    Text("Solo lectura local. No cambia ajustes, no borra archivos ni envía pruebas de red. Guarda la ejecución cuando termine si quieres conservarla.",style=MaterialTheme.typography.bodySmall)
    Button(enabled=scopeLoaded && !blocked && !agent.running && !agent.planning,onClick={
     val workId=work?.id;val workScope=workId?:"Todas las evidencias locales"
     session.agent.update{it.copy(running=true,results=emptyList(),run=null,error="",evidenceScope=workScope)}
     executionJob=scope.launch{try{
      val run=withContext(Dispatchers.IO){executeAgentPlan(plan,perform={tool->ensureActive();performAgentTool(context,tool,workId)},progress={steps->session.agent.update{it.copy(results=steps)}})}
      session.agent.update{it.copy(run=run,results=run.steps)}
     }catch(e:CancellationException){session.agent.update{it.copy(error="Ejecución interrumpida; los pasos sin resultado no se consideran completados")};throw e}
     catch(e:Exception){session.agent.update{it.copy(error=e.message?:"Error de ejecución")}}
     finally{session.agent.update{it.copy(running=false)}}}
    }){Text(if(agent.running)"Ejecutando…" else "Ejecutar este plan")}
   }}
  }
  if(agent.running){LinearProgressIndicator(Modifier.fillMaxWidth());OutlinedButton(onClick={executionJob?.cancel()}){Text("Detener ejecución")}}
  agent.results.forEach{step->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text(step.tool.label,style=MaterialTheme.typography.titleMedium)
   Text(if(step.result!=null)"COMPLETADO · LECTURA REAL" else "ERROR · LOS SIGUIENTES PASOS NO SE EJECUTAN",color=if(step.result!=null)DeckMint else MaterialTheme.colorScheme.error,style=MaterialTheme.typography.labelLarge)
   step.result?.let{r->Text(r.summary);DeckDisclosure("Ver resultado y método",listOf("Destino: ${r.target}","Lectura: ${r.checkedAt}").plus(r.details.map{"${it.first}: ${it.second}"}).joinToString("\n"))}?:Text(step.error)
  }}}
  agent.run?.let{run->
   Text("${run.steps.count{it.result!=null}} de ${run.plan.steps.size} pasos completados",style=MaterialTheme.typography.titleMedium)
   OutlinedButton(enabled=!blocked && !agent.running,onClick={runCatching{run.diagnostic(agent.request,agent.modelHash,agent.evidenceScope)}.onSuccess(save).onFailure{e->session.agent.update{it.copy(error=e.message?:"No se pudo preparar el informe")}}}){Text("Guardar ejecución como evidencia")}
  }
  if(agent.raw.isNotBlank())DeckDisclosure("Plan recibido de la IA",agent.raw)
  DeckDisclosure("Herramientas y límites del agente","Primera versión: revisión del móvil, estado local de conexión y comparación de copias verificadas. La IA no accede a shell, archivos arbitrarios, NFC, USB ni red. Un validador estricto rechaza órdenes desconocidas o repetidas y exige entre uno y tres pasos. Se ejecuta solo al pulsar Ejecutar este plan. Un error detiene los pasos posteriores; salir interrumpe el trabajo. No hay bucle autónomo de reintentos ni acciones en segundo plano. Un plan válido puede no corresponder a lo que pediste: revísalo. Las herramientas no detectan malware ni certifican ausencia de ataques.")
 }
}
