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
import java.time.Instant

@Composable fun AgentPanel(session:DeckSessionModel,blocked:Boolean,send:(Int,String)->Unit,save:(DiagnosticResult)->Unit,navigate:(String)->Unit={}){
 val context=LocalContext.current;val scope=rememberCoroutineScope()
 val ai by session.ai.collectAsState();val agent by session.agent.collectAsState();val model by session.aiModel.collectAsState()
 var request by rememberSaveable{mutableStateOf("Revisa mi móvil y el estado de su conexión")}
 var work by remember{mutableStateOf<AuditWork?>(null)};var scopeLoaded by remember{mutableStateOf(false)}
 var scopeError by remember{mutableStateOf("")};var executionJob by remember{mutableStateOf<Job?>(null)}
 var catalog by rememberSaveable{mutableStateOf(false)};var search by rememberSaveable{mutableStateOf("")}
 var picked by rememberSaveable{mutableStateOf(arrayListOf<String>())};var manualNote by rememberSaveable{mutableStateOf("")}
 val editing=!blocked && !agent.running && !agent.planning
 LaunchedEffect(Unit){try{val works=withContext(Dispatchers.IO){AuditWorkStore(context).read()};work=works.works.firstOrNull{it.id==works.activeId};scopeLoaded=true}catch(e:CancellationException){throw e}catch(e:Exception){scopeError="No se pudo leer el alcance: ${e.message}"}}
 LaunchedEffect(ai.busy,ai.output,ai.error){if(session.agent.value.planning && !ai.busy){val plan=runCatching{check(ai.error.isBlank()){ai.error};AgentPolicy.parse(ai.output)};session.agent.update{it.copy(planning=false,raw=ai.output,plan=plan.getOrNull(),error=plan.exceptionOrNull()?.message.orEmpty())}}}
 DisposableEffect(Unit){onDispose{session.agent.update{if(it.planning)it.copy(planning=false,error="Plan interrumpido al salir") else it}}}
 fun execute(completion:String?=null){
  val snapshot=session.agent.value;val plan=snapshot.plan?:return
  val prior=snapshot.run;val pending=prior?.steps?.lastOrNull()?.takeIf{it.awaiting}
  val id=if(prior==null)work?.id else snapshot.workId
  session.agent.update{it.copy(running=true,error="",evidenceScope=if(prior==null)work?.let{w->"${w.name} [${w.id}]"}?:"Todas las evidencias locales" else snapshot.evidenceScope,workId=id)}
  executionJob=scope.launch{try{
   val run=withContext(Dispatchers.IO){
    validateAgentInputs(plan,snapshot.inputs)
    val started=Instant.now().toString()
    val previous=if(pending==null)emptyList() else{
     val completed=when(completion){
      "saved"->{val entry=snapshot.resumeEvidence?:error("No hay un resultado guardado");loadAgentStepResult(EvidenceStore(context),pending,entry)}
      "manual"->AgentStepResult(pending.tool,manual=true,result=DiagnosticResult("manual-step",pending.tool.label,Instant.now().toString(),"Interacción confirmada por el usuario; no verificada por el agente",listOf("Nota manual" to manualNote.take(400))))
      "skip"->AgentStepResult(pending.tool,skipped=true)
      else->error("El paso está pendiente de interacción")
     }
     prior!!.steps.dropLast(1)+completed
    }
    executeAgentPlan(plan,perform={tool->ensureActive();performAgentTool(context,tool,id,snapshot.inputs){!isActive}},progress={steps->session.agent.update{it.copy(results=steps,run=AgentRun(plan,prior?.startedAt?:started,Instant.now().toString(),steps))}},previous=previous,startedAt=prior?.startedAt?:started)
   }
   session.agent.update{it.copy(run=run,results=run.steps,resumeEvidence=null)}
  }catch(e:CancellationException){session.agent.update{it.copy(error="Ejecución interrumpida; no se completan los pasos pendientes")};throw e}
   catch(e:Exception){session.agent.update{it.copy(error=e.message?:"Error de ejecución")}}
   finally{session.agent.update{it.copy(running=false)}}}
 }
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("AGENTE LOCAL · EXPERIMENTAL",color=DeckCyan,style=MaterialTheme.typography.labelLarge)
  Text("Un plan para todo tu kit",style=MaterialTheme.typography.headlineSmall)
  Text("${AgentTool.entries.count{!it.assisted}} funciones directas y ${AgentTool.entries.count{it.assisted}} asistidas. Los permisos, archivos elegidos e interacciones físicas se gestionan en su herramienta. Hasta 8 pasos por plan.")
  OutlinedTextField(request,{request=it.take(800)},label={Text("Qué quieres que haga CiberDeck")},modifier=Modifier.fillMaxWidth(),enabled=editing,maxLines=4)
  TextButton(enabled=editing,onClick={request="Revisa mi móvil, lee su conexión y compara mis lecturas guardadas"}){Text("Usar revisión local completa")}
  Button(enabled=ai.connected && ai.loaded && editing && request.isNotBlank(),onClick={session.agent.value=AgentSession(request=request,modelHash=model?.sha256.orEmpty(),planning=true);session.aiRequest.value=null;send(AiWire.PLAN,AgentPolicy.prompt(request,model?.name.orEmpty()))}){Text("Preparar plan con IA")}
  if(!ai.loaded)Text("Carga el modelo para planificar con IA, o elige funciones del catálogo sin usar el modelo.",style=MaterialTheme.typography.bodySmall)
  TextButton(onClick={catalog=!catalog},enabled=editing){Text(if(catalog)"Cerrar catálogo" else "Elegir funciones · ${AgentTool.entries.size}")}
  if(catalog){
   OutlinedTextField(search,{search=it.take(100)},label={Text("Buscar función del agente")},modifier=Modifier.fillMaxWidth(),enabled=editing)
   AgentTool.entries.filter{"${it.label} ${it.group} ${it.wire}".contains(search,true)}.groupBy{it.group}.forEach{(group,tools)->
    Text(group,color=DeckCyan,style=MaterialTheme.typography.titleSmall)
    tools.forEach{tool->Row(Modifier.fillMaxWidth()){
     Checkbox(tool.wire in picked,enabled=editing && (picked.size<8 || tool.wire in picked),onCheckedChange={yes->picked=ArrayList(if(yes)picked+tool.wire else picked-tool.wire)},modifier=Modifier.testTag("agent-pick-${tool.wire}"))
     Column(Modifier.weight(1f).padding(top=8.dp)){Text(tool.label);Text(if(tool.assisted)"Asistida · se pausa en su pantalla" else if(tool.network)"Directa · requiere destino autorizado" else "Directa · lectura o cálculo local",style=MaterialTheme.typography.bodySmall)}
    }}
   }
   Button(enabled=editing && picked.isNotEmpty(),onClick={session.agent.value=AgentSession(request="Plan elegido: "+picked.joinToString{wire->AgentTool.entries.first{it.wire==wire}.label},plan=AgentPlan(picked.map{wire->AgentTool.entries.first{it.wire==wire}}),origin="Selección manual");catalog=false}){Text("Preparar selección · ${picked.size}/8")}
  }
  if(agent.planning){LinearProgressIndicator(Modifier.fillMaxWidth());OutlinedButton(onClick={send(AiWire.STOP,"")}){Text("Detener planificación")}}
  if(agent.error.isNotBlank())Text(agent.error,color=MaterialTheme.colorScheme.error)
  if(scopeError.isNotBlank())Text(scopeError,color=MaterialTheme.colorScheme.error)
  agent.plan?.let{plan->
   val input=agent.inputs;val editableInputs=editing && agent.run==null
   fun inputChanged(value:AgentInputs){session.agent.update{it.copy(inputs=value)}}
   if(plan.steps.any{it==AgentTool.DNS || it==AgentTool.TCP})OutlinedTextField(input.host,{inputChanged(input.copy(host=it.take(253),networkApproved=false))},label={Text("Dominio o IP autorizado")},modifier=Modifier.fillMaxWidth(),enabled=editableInputs,singleLine=true)
   if(AgentTool.TCP in plan.steps)OutlinedTextField(input.port,{inputChanged(input.copy(port=it.take(5),networkApproved=false))},label={Text("Puerto TCP")},enabled=editableInputs,singleLine=true)
   if(plan.steps.any{it==AgentTool.WEB || it==AgentTool.LINK})OutlinedTextField(input.url,{inputChanged(input.copy(url=it.take(4096),networkApproved=false))},label={Text("URL elegida")},modifier=Modifier.fillMaxWidth(),enabled=editableInputs)
   if(AgentTool.CIDR in plan.steps)OutlinedTextField(input.cidr,{inputChanged(input.copy(cidr=it.take(32)))},label={Text("IPv4 y prefijo CIDR")},modifier=Modifier.fillMaxWidth(),enabled=editableInputs,singleLine=true)
   if(plan.steps.any{it.network})Row{Checkbox(input.networkApproved,onCheckedChange={inputChanged(input.copy(networkApproved=it))},enabled=editableInputs);Text("Tengo autorización para comprobar los destinos y el puerto indicados. DNS, TCP y HEAD pueden enviar tráfico.",Modifier.weight(1f).padding(top=10.dp),style=MaterialTheme.typography.bodySmall)}
   val inputError=runCatching{validateAgentInputs(plan,input)}.exceptionOrNull()?.message
   Card(Modifier.fillMaxWidth().testTag("agent-plan")){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
    Text("PLAN PROPUESTO · ${plan.steps.size} PASOS",color=DeckMint,style=MaterialTheme.typography.labelLarge)
    Text(agent.request);Text("Origen: ${agent.origin}",style=MaterialTheme.typography.bodySmall)
    plan.steps.forEachIndexed{i,tool->Text("${i+1}. ${tool.label}${if(tool.assisted)" · asistida" else ""}")}
    Text("Copias: ${if(agent.run!=null)agent.evidenceScope else work?.name?:if(scopeLoaded)"Todas las evidencias locales" else "Leyendo alcance…"}",style=MaterialTheme.typography.bodySmall)
    if(inputError!=null)Text(inputError,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
    if(agent.run==null)Button(enabled=scopeLoaded && editing && inputError==null,onClick={execute()}){Text(if(agent.running)"Ejecutando…" else "Ejecutar este plan")}
   }}
  }
  if(agent.running){LinearProgressIndicator(Modifier.fillMaxWidth());OutlinedButton(onClick={executionJob?.cancel()}){Text("Detener ejecución")}}
  agent.results.forEach{step->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text(step.tool.label,style=MaterialTheme.typography.titleMedium)
   Text(when{step.awaiting->"PENDIENTE DE INTERACCIÓN";step.skipped->"OMITIDO";step.manual->"CONFIRMADO POR TI · SIN VERIFICACIÓN";step.result!=null && step.tool==AgentTool.ANALYST->"INTERPRETACIÓN GENERADA · REVISAR";step.result!=null->"COMPLETADO · RESULTADO DE HERRAMIENTA";else->"ERROR · PLAN DETENIDO"},color=if(step.result!=null)DeckMint else DeckCyan,style=MaterialTheme.typography.labelLarge)
   step.result?.let{r->Text(r.summary);DeckDisclosure("Ver resultado y método",listOf("Destino: ${r.target}","Lectura: ${r.checkedAt}").plus(r.details.map{"${it.first}: ${it.second}"}).joinToString("\n"))}
   if(step.error.isNotBlank())Text(step.error,color=MaterialTheme.colorScheme.error)
   if(step.awaiting && !agent.running){
    Text("Abre la herramienta y realiza la operación. Si guarda un diagnóstico, podrás incorporarlo verificando su copia. Abrirla no ejecuta ni completa el paso.",style=MaterialTheme.typography.bodySmall)
    OutlinedButton(onClick={navigate(step.tool.route!!)},enabled=editing,modifier=Modifier.testTag("agent-open-${step.tool.wire}")){Text("Abrir herramienta")}
    if(agent.resumeEvidence!=null)Button(onClick={execute("saved")},enabled=editing){Text("Usar resultado guardado y continuar")}
    OutlinedTextField(manualNote,{manualNote=it.take(400)},label={Text("Nota de la interacción manual")},modifier=Modifier.fillMaxWidth(),enabled=editing)
    TextButton(onClick={execute("manual")},enabled=editing){Text("Confirmar manualmente y continuar")}
    TextButton(onClick={execute("skip")},enabled=editing){Text("Omitir este paso")}
   }
  }}}
  agent.run?.let{run->
   Text("${run.steps.count{it.result!=null}} de ${run.plan.steps.size} pasos completados${if(run.steps.any{it.awaiting})" · en pausa" else ""}",style=MaterialTheme.typography.titleMedium)
   OutlinedButton(enabled=editing,onClick={runCatching{run.diagnostic(agent.request,agent.modelHash,agent.evidenceScope,agent.origin)}.onSuccess(save).onFailure{e->session.agent.update{it.copy(error=e.message?:"No se pudo preparar el informe")}}}){Text("Guardar ejecución como evidencia")}
  }
  if(agent.raw.isNotBlank())DeckDisclosure("Plan recibido de la IA",agent.raw)
  DeckDisclosure("Acceso y límites","El agente utiliza las funciones implementadas de CiberDeck y permisos concedidos por Android. No tiene root, shell ni acceso a datos privados de otras apps. Las herramientas asistidas conservan los selectores, permisos y confirmaciones de su pantalla. Los resultados guardados se vuelven a verificar; confirmaciones manuales y texto generado se distinguen de lecturas. RF/IR y otros módulos del catálogo futuro siguen pendientes. La sesión permanece en memoria hasta guardarla; salir durante una ejecución la interrumpe. Un plan puede no corresponder a tu intención: revísalo.")
 }
}
