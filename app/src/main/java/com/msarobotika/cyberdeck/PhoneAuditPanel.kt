package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun PhoneAuditPanel(snapshot:PhoneSnapshot?,busy:Boolean,refresh:()->Unit,save:(DiagnosticResult)->Unit,settings:(String)->Unit,
    samples:List<ResourceSample>,live:Boolean,setLive:(Boolean)->Unit,add:(ResourceSample)->Unit) {
    var section by rememberSaveable{mutableStateOf("Seguridad")}
    LaunchedEffect(section){if(section!="Recursos")setLive(false)}
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("MI MÓVIL",style=MaterialTheme.typography.labelLarge,color=DeckCyan)
        if(snapshot==null){Text("Comprueba los ajustes y recursos que Android permite observar. Todo se analiza en este móvil.");Button(enabled=!busy,onClick=refresh){Text("Analizar ahora")}}
        else {
            Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text(snapshot.device,style=MaterialTheme.typography.titleLarge)
                val review=snapshot.checks.count{it.tone==PhoneTone.REVIEW}
                Text(if(review==0)"Sin ajustes destacados para revisar" else "$review ajustes para revisar",color=if(review>0)MaterialTheme.colorScheme.tertiary else DeckCyan)
                Text("Lectura: ${displayTime(snapshot.at)}",style=MaterialTheme.typography.bodySmall)
                Text("Una observación local; no es un antivirus ni garantiza ausencia de ataques.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button(enabled=!busy,onClick=refresh){Text("Actualizar")}
                    OutlinedButton(enabled=!busy,onClick={save(snapshot.diagnostic())}){Text("Guardar chequeo")}
                }
            }}
            FilterChip(selected=section=="Plan",onClick={section="Plan"},label={Text("Plan de revisión")})
            for(pair in listOf(listOf("Seguridad","Conexión"),listOf("Recursos","Hardware")))Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                pair.forEach{label->FilterChip(selected=section==label,onClick={section=label},label={Text(label)},modifier=Modifier.weight(1f))}
            }
            if(section=="Plan")PhoneReviewPanel(snapshot,busy,save,settings)
            if(section in listOf("Seguridad","Conexión")) snapshot.checks.filter{it.section==section}.forEach{check->
                Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    Text(check.title,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(check.value,style=MaterialTheme.typography.titleMedium,color=when(check.tone){PhoneTone.OK->DeckMint;PhoneTone.REVIEW->MaterialTheme.colorScheme.tertiary;PhoneTone.UNKNOWN->MaterialTheme.colorScheme.onSurfaceVariant;else->DeckCyan})
                    Text(check.explanation,style=MaterialTheme.typography.bodySmall)
                    check.settings?.let{action->TextButton(onClick={settings(action)}){Text("Abrir ajuste de Android")}}
                }}
            }
            if(section=="Recursos") {
                LiveResourceGraphs(samples,live,setLive,add,save={save(ResourceSampler.diagnostic(samples))},busy=busy)
                ResourceCard("Memoria RAM disponible",snapshot.ramAvailable,snapshot.ramTotal)
                ResourceCard("Espacio disponible",snapshot.diskAvailable,snapshot.diskTotal)
                Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text("Batería",style=MaterialTheme.typography.titleMedium)
                    Text(snapshot.battery?.let{"$it %"}?:"No disponible",style=MaterialTheme.typography.headlineSmall,color=DeckCyan)
                    snapshot.battery?.let{LinearProgressIndicator(progress={it/100f},modifier=Modifier.fillMaxWidth())}
                    Text("Temperatura de batería: ${snapshot.batteryTemperature?.let{"$it °C"}?:"no disponible"}",style=MaterialTheme.typography.bodySmall)
                    Text("Es una instantánea; no mide temperatura de CPU ni ejecuta una prueba de estrés.",style=MaterialTheme.typography.bodySmall)
                }}
            }
            if(section=="Hardware")snapshot.hardware.forEach{(label,value)->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                Text(label,color=DeckCyan,style=MaterialTheme.typography.titleMedium);Text(value,style=MaterialTheme.typography.bodyMedium)
            }}}
        }
    }
}
@Composable private fun ResourceCard(title:String,available:Long?,total:Long?) {
    Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text(title,style=MaterialTheme.typography.titleMedium)
        Text(if(available!=null && total!=null)"${displayBytes(available)} de ${displayBytes(total)}" else "No disponible",color=DeckCyan)
        if(available!=null && total!=null && total>0)LinearProgressIndicator(progress={(available.toFloat()/total).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
        Text(if(title=="Espacio disponible")"Volumen donde se guarda esta app; lectura puntual de Android." else "Memoria disponible del sistema; no es el consumo de CiberDeck.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }}
}
