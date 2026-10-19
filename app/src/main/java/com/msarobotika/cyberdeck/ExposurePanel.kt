package com.msarobotika.cyberdeck

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.update

@Composable fun ExposurePanel(session:DeckSessionModel,save:(DiagnosticResult)->Unit) {
 val state by session.exposure.collectAsState()
 var target by rememberSaveable{mutableStateOf("")}
 var ports by rememberSaveable{mutableStateOf("80,443")}
 var index by rememberSaveable{mutableIntStateOf(0)}
 var authorized by rememberSaveable{mutableStateOf(false)}
 val scope=rememberCoroutineScope()
 val context=androidx.compose.ui.platform.LocalContext.current
 val connectivity=remember(context){context.getSystemService(android.net.ConnectivityManager::class.java)}
 fun networkKey()=connectivity.activeNetwork?.networkHandle?.toString()?:"sin-red-activa"
 val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
 fun invalidate(){session.exposure.update{it.copy(plan=null,result=null,probes=emptyList(),error="")};authorized=false;index=0}
 DisposableEffect(owner){val listener=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP)session.exposureRunner?.cancel()};owner.lifecycle.addObserver(listener);onDispose{session.exposureRunner?.cancel();owner.lifecycle.removeObserver(listener)}}
 fun resolve(){if(state.busy)return;session.exposure.update{it.copy(busy=true,error="",phase="Resolviendo destino",result=null,probes=emptyList())};scope.launch{
  try{val key=networkKey();val plan=withContext(Dispatchers.IO){ExposureAudit.resolve(target,ExposurePolicy.ports(ports)).copy(networkKey=key)};require(key==networkKey()){"La red cambió al resolver; vuelve a resolver el destino"};index=0;authorized=false;session.exposure.update{it.copy(plan=plan,phase="Confirma la IP y el alcance")}}
  catch(e:CancellationException){session.exposure.update{it.copy(phase="Resolución detenida")};throw e}
  catch(e:Exception){session.exposure.update{it.copy(error=e.message?:"No se pudo resolver",phase="Revisa el destino")}}
  finally{session.exposure.update{it.copy(busy=false)}}
 }}
 fun probe(){val plan=state.plan?:return;if(state.busy || !authorized)return;val address=plan.addresses.getOrNull(index)?:return
  val network=connectivity.activeNetwork
  if(plan.networkKey!=networkKey()){session.exposure.update{it.copy(error="La red ha cambiado. Resuelve de nuevo el destino para confirmar el alcance.")};authorized=false;return}
  val runner=ExposureRunner();session.exposureRunner=runner;session.exposure.update{it.copy(busy=true,error="",result=null,probes=emptyList(),phase="Comprobando servicios elegidos")}
  scope.launch{try{val result=withContext(Dispatchers.IO){runner.run(plan,address,configureSocket={socket->network?.bindSocket(socket)},networkUnchanged={plan.networkKey==networkKey()}){rows->session.exposure.update{it.copy(probes=rows)}}};session.exposure.update{it.copy(result=result,probes=result.probes,phase=if(result.cancelled)"Comprobación detenida: salida o cambio de red" else "Comprobación completada")}}
   catch(e:CancellationException){runner.cancel();session.exposure.update{it.copy(phase="Comprobación interrumpida al salir")};throw e}
   catch(e:Exception){session.exposure.update{it.copy(error=e.message?:"Error de comprobación")}}
   finally{session.exposure.update{it.copy(busy=false)};if(session.exposureRunner===runner)session.exposureRunner=null}
  }
 }
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text("AUDITORÍA DE UN EQUIPO",style=MaterialTheme.typography.labelLarge,color=DeckCyan)
  Text("De la pregunta a la evidencia",style=MaterialTheme.typography.headlineSmall)
  Text("Comprueba qué conexiones TCP acepta tu equipo desde esta red, fija una referencia y detecta cambios al repetir.")
  OutlinedTextField(target,{target=it.take(253);invalidate()},label={Text("Equipo: dominio o IP")},enabled=!state.busy,singleLine=true,modifier=Modifier.fillMaxWidth())
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Web" to "80,443","PC" to "22,445,3389,5900","IoT" to "1883,8883,502,8080").forEach{(name,value)->FilterChip(selected=ports==value,onClick={ports=value;invalidate()},label={Text(name)},enabled=!state.busy)}}
  OutlinedTextField(ports,{ports=it.take(100);invalidate()},label={Text("Puertos TCP")},supportingText={Text("Hasta 8 números, separados por comas; sin rangos")},enabled=!state.busy,singleLine=true,modifier=Modifier.fillMaxWidth())
  Button(enabled=!state.busy && target.isNotBlank(),onClick={resolve()}){Text("Resolver destino")}
  Text(state.phase,color=DeckCyan,style=MaterialTheme.typography.bodySmall)
  if(state.error.isNotEmpty())Text(state.error,color=MaterialTheme.colorScheme.error)
  state.plan?.let{plan->
   Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
    Text("IP elegida: ${plan.addresses.getOrNull(index)?.hostAddress.orEmpty()}",style=MaterialTheme.typography.titleMedium)
    Text("Destino fijado al resolver; no se vuelve a consultar DNS para cada puerto.",style=MaterialTheme.typography.bodySmall)
    Text("Red de Android fijada para esta prueba. Si cambia, hay que resolver de nuevo.",style=MaterialTheme.typography.bodySmall)
    if(plan.addresses.size>1)plan.addresses.forEachIndexed{i,address->Row(Modifier.fillMaxWidth().clickable(enabled=!state.busy){index=i;authorized=false}){RadioButton(selected=index==i,onClick={index=i;authorized=false},enabled=!state.busy);Text(address.hostAddress.orEmpty(),Modifier.padding(top=12.dp))}}
    Text("Alcance: ${plan.ports.joinToString()} · una IP · espera máxima de 1 s por puerto",style=MaterialTheme.typography.bodySmall)
   }}
   Row(Modifier.fillMaxWidth().clickable(enabled=!state.busy){authorized=!authorized}){Checkbox(checked=authorized,onCheckedChange={authorized=it},enabled=!state.busy);Text("Tengo autorización para comprobar este equipo",Modifier.weight(1f).padding(top=12.dp))}
   Button(enabled=!state.busy && authorized,onClick={probe()}){Text(if(state.result==null)"Comprobar servicios" else "Repetir comprobación")}
  }
  if(state.busy){LinearProgressIndicator(Modifier.fillMaxWidth());if(session.exposureRunner!=null)OutlinedButton(onClick={session.exposureRunner?.cancel()}){Text("Detener comprobación")}}
  if(state.probes.isNotEmpty()){
   ExposureSummary(state.probes)
   Text("${state.probes.size} intentos terminados. El gráfico representa resultados TCP, no un nivel de peligro.",style=MaterialTheme.typography.bodySmall)
   state.probes.forEach{p->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
    Row(Modifier.fillMaxWidth()){Text("TCP ${p.port}",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium);Text(p.state.label,color=portColor(p.state))}
    Text("${p.elapsedMs} ms · ${p.detail}",style=MaterialTheme.typography.bodySmall)
    ExposurePolicy.use(p.port)?.let{Text("Uso habitual: $it. El servicio real no se ha identificado.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
   }}}
  }
  state.result?.let{result->
   Text("Lectura: ${displayTime(result.at)} · duración ${result.durationMs} ms",style=MaterialTheme.typography.bodySmall)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    OutlinedButton(enabled=!state.busy,onClick={save(result.diagnostic(state.reference))}){Text("Guardar informe")}
    OutlinedButton(enabled=!state.busy && !result.cancelled,onClick={session.exposure.update{it.copy(reference=result)}}){Text("Fijar referencia")}
   }
   state.reference?.let{reference->val changes=ExposurePolicy.changes(reference,result)
    Text("Comparación con referencia",style=MaterialTheme.typography.titleMedium)
    Text(if(changes.isEmpty())"Mismos estados observados en los puertos comprobados" else changes.joinToString("\n"))
    Text("Un cambio puede deberse al servicio o firewall; no prueba un ataque. Una IP reutilizada tampoco acredita la identidad del equipo.",style=MaterialTheme.typography.bodySmall)
    TextButton(onClick={session.exposure.update{it.copy(reference=null)}}){Text("Quitar referencia temporal")}
   }
  }
  DeckDisclosure("Método y límites","Solo establece conexiones TCP con una IP y hasta ocho puertos solicitados. No envía cargas, contraseñas ni datos de aplicación, no explota fallos y no recorre subredes. Un puerto que acepta TCP puede requerir autenticación. Error y timeout no determinan si un puerto está cerrado o filtrado. Los resultados y la referencia son temporales hasta que guardes un informe. La comprobación se cancela al salir o bloquear la pantalla.")
 }
}
private fun portColor(state:PortState)=when(state){PortState.OPEN->DeckMint;PortState.UNESTABLISHED->DeckCyan;PortState.TIMEOUT->Color(0xFFFFD36B);PortState.ERROR->Color(0xFFFF99A6);PortState.CANCELLED->Color(0xFFB4C2D8)}
@Composable private fun ExposureSummary(rows:List<PortObservation>){Card(Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(16.dp)){
 Canvas(Modifier.size(96.dp)){var start=-90f;val stroke=18f;PortState.entries.forEach{state->val count=rows.count{it.state==state};if(count>0){val angle=count*360f/rows.size;drawArc(portColor(state),start,angle,false,Offset(stroke/2,stroke/2),Size(size.width-stroke,size.height-stroke),style=Stroke(stroke));start+=angle}}}
 Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){Text("RESULTADOS REALES",style=MaterialTheme.typography.labelSmall);PortState.entries.filter{state->rows.any{it.state==state}}.forEach{state->Text("${rows.count{it.state==state}} · ${state.label}",color=portColor(state),style=MaterialTheme.typography.bodySmall)}}
}}}
