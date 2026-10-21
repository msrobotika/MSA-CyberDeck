package com.msarobotika.cyberdeck

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.time.Instant

data class ResourceSample(val at:String,val elapsed:Long,val ramAvailable:Long?,val ramTotal:Long?,val battery:Int?)
object ResourceSampler {
    fun read(context:Context):ResourceSample {
        val memory=runCatching{ActivityManager.MemoryInfo().also{context.getSystemService(ActivityManager::class.java).getMemoryInfo(it)}}.getOrNull()
        val status=runCatching{context.registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))}.getOrNull()
        val level=status?.getIntExtra(BatteryManager.EXTRA_LEVEL,-1)?:-1
        val scale=status?.getIntExtra(BatteryManager.EXTRA_SCALE,-1)?:-1
        return ResourceSample(Instant.now().toString(),SystemClock.elapsedRealtime(),memory?.availMem,memory?.totalMem,
            if(level>=0 && scale>0)(level*100.0/scale).toInt().coerceIn(0,100) else null)
    }
    fun diagnostic(samples:List<ResourceSample>)=DiagnosticResult("resources","Muestras del móvil",Instant.now().toString(),"${samples.size} muestras locales reales",
        listOf("Método" to "Lecturas de RAM disponible y batería del sistema, solicitadas por el usuario; hasta 60 muestras. No se mide temperatura de CPU ni velocidad de Internet.")+
            samples.map{it.at to "RAM disponible ${it.ramAvailable?:"N/D"} / ${it.ramTotal?:"N/D"} bytes; batería ${it.battery?:"N/D"}%; reloj monotónico ${it.elapsed} ms"})
}

@Composable fun LiveResourceGraphs(samples:List<ResourceSample>,live:Boolean,setLive:(Boolean)->Unit,add:(ResourceSample)->Unit,save:()->Unit,busy:Boolean) {
    val context=LocalContext.current.applicationContext
    val callback by rememberUpdatedState(add)
    LaunchedEffect(live){if(live)while(isActive){callback(withContext(Dispatchers.IO){ResourceSampler.read(context)});delay(2000)}}
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(if(live)"MEDICIÓN EN VIVO" else "MEDICIÓN PAUSADA",color=if(live)DeckMint else MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelLarge)
        Text("${samples.size}/60 muestras · intervalo de 2 s · se detiene al salir",style=MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Button(onClick={setLive(!live)}){Text(if(live)"Pausar gráfica" else "Iniciar gráfica en vivo")}
            TextButton(enabled=samples.isNotEmpty() && !busy,onClick=save){Text("Guardar serie")}
        }
        ResourceChart("RAM disponible",samples.mapNotNull{sample->sample.ramAvailable?.let{Triple(sample.elapsed,it/1048576f,sample.at)}},"MiB",false)
        ResourceChart("Batería",samples.mapNotNull{sample->sample.battery?.let{Triple(sample.elapsed,it.toFloat(),sample.at)}},"%",true)
    }
}
@Composable internal fun ResourceChart(title:String,data:List<Triple<Long,Float,String>>,unit:String,fixedPercent:Boolean) {
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text(title,style=MaterialTheme.typography.titleMedium)
        if(data.isEmpty())Text("Inicia la medición para ver muestras reales.",style=MaterialTheme.typography.bodySmall)
        else {
            val values=data.map{it.second};val min=if(fixedPercent)0f else values.min();val max=if(fixedPercent)100f else values.max()
            Text("Última: ${"%.1f".format(java.util.Locale.ROOT,data.last().second)} $unit · ${displayTime(data.last().third)}",style=MaterialTheme.typography.bodySmall,color=DeckCyan)
            Text("Escala ${"%.1f".format(java.util.Locale.ROOT,min)}–${"%.1f".format(java.util.Locale.ROOT,max)} $unit",style=MaterialTheme.typography.bodySmall)
            val range=(max-min).coerceAtLeast(1f);val start=data.first().first;val span=(data.last().first-start).coerceAtLeast(1)
            Canvas(Modifier.fillMaxWidth().height(120.dp).semantics{contentDescription="$title: ${data.size} muestras observadas. Última ${data.last().second} $unit"}) {
                val margin=8f;val w=size.width-margin*2;val h=size.height-margin*2
                for(i in 0..3){val y=margin+h*i/3;drawLine(androidx.compose.ui.graphics.Color(0xFF20394D),Offset(margin,y),Offset(margin+w,y),1f)}
                fun point(row:Triple<Long,Float,String>)=Offset(margin+(row.first-start).toFloat()/span*w,margin+h*(1f-(if(max==min).5f else (row.second-min)/range)))
                data.forEachIndexed{index,row->val p=point(row);if(index>0 && row.first-data[index-1].first<=5000)drawLine(DeckCyan,point(data[index-1]),p,3f);drawCircle(DeckMint,3.5f,p)}
            }
            Text("${data.size} puntos · ${(data.last().first-data.first().first)/1000} s observados. El trazo une muestras; los huecos de más de 5 s quedan abiertos.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }}
}

@Composable fun BleSignalChart(snapshot:BleSnapshot) {
    val devices=snapshot.devices.filter{it.rssi in -127..0}.sortedByDescending{it.rssi}.take(12)
    if(devices.isEmpty())return
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
        Text("Señal BLE observada",style=MaterialTheme.typography.titleMedium)
        Text("${if(snapshot.running)"Búsqueda en curso" else "Última lectura"} · ${displayTime(snapshot.observedAt)}",style=MaterialTheme.typography.bodySmall)
        devices.forEach{row->
            Row(Modifier.fillMaxWidth()){Text(row.name.ifBlank{"Sin nombre"}.take(24),Modifier.weight(1f),style=MaterialTheme.typography.bodySmall);Text("${row.rssi} dBm",style=MaterialTheme.typography.bodySmall,color=DeckCyan)}
            LinearProgressIndicator(progress={((row.rssi+127)/127f).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
        }
        Text("Hasta 12 señales ordenadas. Escala −127 a 0 dBm. RSSI no mide distancia ni dirección.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }}
}
