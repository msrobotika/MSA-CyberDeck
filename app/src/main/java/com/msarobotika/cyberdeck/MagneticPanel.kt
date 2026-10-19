package com.msarobotika.cyberdeck

import android.hardware.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.util.Locale
import kotlin.math.sqrt

data class MagneticSample(val at:String,val sensorNanos:Long,val x:Double,val y:Double,val z:Double,val accuracy:Int){val magnitude get()=MagneticPolicy.magnitude(x,y,z)}
object MagneticPolicy {
 fun magnitude(x:Double,y:Double,z:Double):Double {require(listOf(x,y,z).all{it.isFinite()});return sqrt(x*x+y*y+z*z).also{require(it.isFinite())}}
 fun accuracy(value:Int)=when(value){SensorManager.SENSOR_STATUS_ACCURACY_HIGH->"Alta";SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM->"Media";SensorManager.SENSOR_STATUS_ACCURACY_LOW->"Baja";else->"No fiable o sin indicar"}
 fun report(sensor:String,samples:List<MagneticSample>,reference:Double?)=DiagnosticResult("magnetic",sensor,Instant.now().toString(),"${samples.size} lecturas reales del magnetómetro",listOf(
  "Método" to "Sensor Android TYPE_MAGNETIC_FIELD, ejes del dispositivo, µT. Hasta 60 muestras, máximo 2 por segundo. Pausa al salir.",
  "Referencia temporal" to (reference?.let{"$it µT"}?:"No fijada"),
  "Límites" to "Campo magnético local, no radiación RF, detector de ataques, certificación sanitaria ni identificación de la fuente. Imanes, orientación, metales y calibración afectan a la lectura.")+samples.map{it.at to "X=${it.x}; Y=${it.y}; Z=${it.z}; módulo=${it.magnitude} µT; calidad=${accuracy(it.accuracy)}; reloj sensor=${it.sensorNanos} ns"})
}

@Composable fun MagneticPanel(session:DeckSessionModel,save:(DiagnosticResult)->Unit){
 val context=LocalContext.current
 val manager=remember(context){context.getSystemService(SensorManager::class.java)}
 val sensor=remember(manager){manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)}
 val samples by session.magnetic.collectAsState()
 val running by session.magneticLive.collectAsState()
 val reference by session.magneticReference.collectAsState()
 val owner=androidx.lifecycle.compose.LocalLifecycleOwner.current
 var error by remember{mutableStateOf("")}
 val latest=samples.lastOrNull()
 fun number(value:Double)=String.format(Locale.ROOT,"%.1f",value)
 DisposableEffect(owner){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP)session.magneticLive.value=false};owner.lifecycle.addObserver(observer);onDispose{session.magneticLive.value=false;owner.lifecycle.removeObserver(observer)}}
 DisposableEffect(running,sensor){
  var previous=0L
  val listener=object:SensorEventListener {
   override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){}
   override fun onSensorChanged(event:SensorEvent){
    if(!session.magneticLive.value || event.values.size<3 || event.timestamp-previous<500000000L)return
    val x=event.values[0].toDouble();val y=event.values[1].toDouble();val z=event.values[2].toDouble()
    if(!listOf(x,y,z).all{it.isFinite()})return
    previous=event.timestamp
    val sample=MagneticSample(Instant.now().toString(),event.timestamp,x,y,z,event.accuracy)
    session.magnetic.update{(it+sample).takeLast(60)}
   }
  }
  val stop=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_STOP)manager.unregisterListener(listener)}
  owner.lifecycle.addObserver(stop)
  if(running && sensor!=null && !manager.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_UI)){error="Android no ha permitido iniciar el sensor";session.magneticLive.value=false}
  onDispose{manager.unregisterListener(listener);owner.lifecycle.removeObserver(stop)}
 }
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("CAMPO MAGNÉTICO · SENSOR REAL",style=MaterialTheme.typography.labelLarge,color=DeckCyan)
  Text("El campo a tu alrededor",style=MaterialTheme.typography.headlineSmall)
  Text("Mide la componente magnética en microteslas. No mide Wi-Fi, Bluetooth ni radiación de radiofrecuencia.")
  if(sensor==null)Text("Este móvil no ofrece magnetómetro. El ESP32 tampoco lo incorpora; necesita un sensor externo.",color=MaterialTheme.colorScheme.error)
  else Text("${sensor.name} · ${sensor.vendor} · resolución ${sensor.resolution} µT · rango declarado ${sensor.maximumRange} µT",style=MaterialTheme.typography.bodySmall)
  Button(enabled=sensor!=null,onClick={error="";session.magneticLive.value=!running}){Text(if(running)"Pausar medición" else "Medir campo magnético")}
  if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
  Text(if(running)"LECTURA ACTIVA · máximo 2 muestras/s" else "PAUSADO · las lecturas conservan su fecha",color=if(running)DeckMint else MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
  latest?.let{sample->
   Card(Modifier.fillMaxWidth()){Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(18.dp)){
    Canvas(Modifier.size(84.dp)){val stroke=3.dp.toPx();for(i in 1..3)drawCircle(DeckCyan.copy(alpha=.2f),size.minDimension*i/6,style=Stroke(stroke));drawLine(DeckCyan,Offset(size.width/2,0f),Offset(size.width/2,size.height));drawLine(DeckCyan,Offset(0f,size.height/2),Offset(size.width,size.height/2))}
    Column(Modifier.weight(1f)){Text("${number(sample.magnitude)} µT",style=MaterialTheme.typography.headlineSmall,color=DeckMint);Text("Calidad: ${MagneticPolicy.accuracy(sample.accuracy)}",style=MaterialTheme.typography.bodySmall);Text(displayTime(sample.at),style=MaterialTheme.typography.bodySmall)}
   }}
   Text("X ${number(sample.x)} · Y ${number(sample.y)} · Z ${number(sample.z)} µT",style=MaterialTheme.typography.bodyMedium)
   Text("Ejes del teléfono, no dirección geográfica. Cambiar su orientación también cambia X/Y/Z.",style=MaterialTheme.typography.bodySmall)
   reference?.let{Text("Cambio respecto a referencia: ${number(sample.magnitude-it)} µT. No indica peligro.",color=DeckCyan)}
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    OutlinedButton(onClick={session.magneticReference.value=sample.magnitude}){Text("Fijar referencia")}
    OutlinedButton(onClick={save(MagneticPolicy.report(sensor?.name?:"Magnetómetro",samples,reference))}){Text("Guardar serie")}
   }
   ResourceChart("Campo magnético",samples.map{Triple(it.sensorNanos/1000000,it.magnitude.toFloat(),it.at)},"µT",false)
   TextButton(enabled=!running,onClick={session.magnetic.value=emptyList();session.magneticReference.value=null}){Text("Limpiar lectura temporal")}
  }
  DeckDisclosure("Qué puedes comprobar","Comparar cambios cerca de tus propios altavoces, motores o imanes, observar interferencias con una brújula y documentar lecturas repetibles. Mueve el teléfono suavemente en forma de ocho si Android informa de poca precisión. Retira fundas magnéticas. Este sensor no permite identificar el origen de un campo ni evaluar exposición sanitaria. Un valor alto o variable no acredita un ataque.")
 }
}
