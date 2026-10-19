package com.msarobotika.cyberdeck

import java.net.*
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

enum class PortState(val label:String){OPEN("TCP aceptado"),UNESTABLISHED("No establecida"),TIMEOUT("Sin respuesta"),ERROR("Error"),CANCELLED("Cancelada")}
data class PortObservation(val port:Int,val state:PortState,val elapsedMs:Long,val detail:String)
data class ExposurePlan(val target:String,val addresses:List<InetAddress>,val ports:List<Int>,val at:String,val networkKey:String?=null)
data class ExposureResult(val target:String,val address:String,val at:String,val durationMs:Long,val probes:List<PortObservation>,val cancelled:Boolean,val networkKey:String?=null){
 fun diagnostic(reference:ExposureResult?=null)=DiagnosticResult("exposure",address,at,"${probes.count{it.state==PortState.OPEN}} conexiones TCP aceptadas de ${probes.size} intentos",listOf(
  "Nombre solicitado" to target,"IP fijada para esta prueba" to address,"Red temporal de Android" to (networkKey?:"No registrada"),"Duración" to "$durationMs ms","Finalización" to if(cancelled)"Interrumpida" else "Completada",
  "Método" to "Un equipo, hasta 8 puertos elegidos, 1 s de espera por puerto, conexión TCP sin datos de aplicación. No se identifica software, versión ni vulnerabilidades.")+
  probes.map{"TCP ${it.port}" to "${it.state.label}; ${it.elapsedMs} ms; ${it.detail}"}+
  (reference?.let{ExposurePolicy.changes(it,this).map{change->"Cambio respecto a ${it.at}" to change}}?:emptyList())+
  ("Límite" to "Un servicio accesible no es una intrusión. Un fallo o timeout no confirma que el puerto esté cerrado o filtrado. El alcance es esta IP y esta red en el momento de la prueba."))
}
object ExposurePolicy {
 fun ports(value:String):List<Int>{val parts=value.trim().split(Regex("[,;\\s]+"));require(parts.isNotEmpty() && parts.size<=32){"Introduce hasta 8 puertos, separados por comas"};val ports=parts.map{it.toIntOrNull()?.takeIf{n->n in 1..65535}?:error("Puerto no válido: usa números entre 1 y 65535, sin rangos")}.distinct();require(ports.size<=8){"El límite es de 8 puertos por equipo"};return ports}
 fun changes(before:ExposureResult,after:ExposureResult):List<String>{
  if(before.address!=after.address || before.probes.map{it.port}.toSet()!=after.probes.map{it.port}.toSet())return listOf("No comparable: IP o conjunto de puertos diferentes")
  if(before.cancelled || after.cancelled)return listOf("No comparable: alguna comprobación se interrumpió")
  if(before.networkKey==null || before.networkKey!=after.networkKey)return listOf("No comparable: red diferente o no registrada")
  return after.probes.mapNotNull{p->before.probes.firstOrNull{it.port==p.port}?.takeIf{it.state!=p.state}?.let{"TCP ${p.port}: ${it.state.label} → ${p.state.label}"}}
 }
 fun use(port:Int)=when(port){22->"SSH";80->"HTTP";443->"HTTPS";445->"SMB";3389->"RDP";5900->"VNC";1883->"MQTT";8883->"MQTT TLS";502->"Modbus TCP";8080->"HTTP alternativo";else->null}
}
object ExposureAudit {
 fun resolve(raw:String,ports:List<Int>):ExposurePlan {val target=Diagnostics.host(raw);require(ports.size in 1..8 && ports.all{it in 1..65535});return ExposurePlan(target,Diagnostics.addresses(target).take(8),ports,Instant.now().toString()).also{require(it.addresses.isNotEmpty()){"No hay direcciones resueltas"}}}
}
class ExposureRunner {
 private val cancelled=AtomicBoolean(false)
 private val active=AtomicReference<Socket?>(null)
 fun cancel(){cancelled.set(true);runCatching{active.getAndSet(null)?.close()}}
 fun run(plan:ExposurePlan,address:InetAddress,configureSocket:(Socket)->Unit={},networkUnchanged:()->Boolean={true},progress:(List<PortObservation>)->Unit):ExposureResult {
  require(address in plan.addresses){"La IP no pertenece al plan resuelto"};require(plan.ports.size in 1..8)
  val at=Instant.now().toString();val start=System.nanoTime();val rows=mutableListOf<PortObservation>()
  for(port in plan.ports){if(!networkUnchanged())cancel();if(cancelled.get())break;val began=System.nanoTime();val socket=Socket();active.set(socket)
   val result=try{if(cancelled.get())throw SocketException("Cancelada");configureSocket(socket);if(!networkUnchanged()){cancel();throw SocketException("Red modificada")};socket.connect(InetSocketAddress(address,port),1000);PortState.OPEN to "El destino aceptó la conexión; no se enviaron datos de aplicación"}
    catch(e:SocketTimeoutException){PortState.TIMEOUT to "No respondió dentro de 1 s; no se conoce la causa"}
    catch(e:ConnectException){PortState.UNESTABLISHED to "La conexión fue rechazada o no pudo establecerse"}
    catch(e:Exception){(if(cancelled.get())PortState.CANCELLED else PortState.ERROR) to if(cancelled.get())"Detenida por el usuario o al salir" else e.javaClass.simpleName}
    finally{runCatching{socket.close()};active.compareAndSet(socket,null)}
   rows+=PortObservation(port,result.first,(System.nanoTime()-began)/1000000,result.second);progress(rows.toList())
  }
  if(!networkUnchanged())cancel()
  return ExposureResult(plan.target,address.hostAddress.orEmpty(),at,(System.nanoTime()-start)/1000000,rows.toList(),cancelled.get(),plan.networkKey)
 }
}
data class ExposureState(val plan:ExposurePlan?=null,val result:ExposureResult?=null,val reference:ExposureResult?=null,val probes:List<PortObservation> = emptyList(),val busy:Boolean=false,val phase:String="Elige un equipo y un alcance",val error:String="")
