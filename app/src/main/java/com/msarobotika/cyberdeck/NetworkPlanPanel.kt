package com.msarobotika.cyberdeck

import android.net.ConnectivityManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.net.Inet4Address
import java.time.Instant

@Composable fun NetworkPlanPanel(save:(DiagnosticResult)->Unit){
 val context=LocalContext.current
 var input by rememberSaveable{mutableStateOf("")}
 var calculated by rememberSaveable{mutableStateOf("")}
 var error by remember{mutableStateOf("")}
 val result=remember(calculated){runCatching{NetworkPlan.parse(calculated)}.getOrNull()}
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("PLAN DE RED · SIN TRÁFICO",style=MaterialTheme.typography.labelLarge,color=DeckCyan)
  Text("Conoce el alcance antes de probar",style=MaterialTheme.typography.headlineSmall)
  Text("Calcula una subred IPv4 sin buscar dispositivos ni enviar paquetes.")
  OutlinedTextField(input,{input=it.take(32);calculated="";error=""},label={Text("IPv4 y prefijo CIDR")},placeholder={Text("192.168.1.20/24")},singleLine=true,modifier=Modifier.fillMaxWidth())
  OutlinedButton(onClick={runCatching{
   val manager=context.getSystemService(ConnectivityManager::class.java)
   val address=manager.getLinkProperties(manager.activeNetwork)?.linkAddresses?.firstOrNull{it.address is Inet4Address}?:throw IllegalStateException("La red activa no publica una IPv4")
   input="${address.address.hostAddress}/${address.prefixLength}";calculated="";error=""
  }.onFailure{error=it.message?:"No se pudo leer la red activa"}}){Text("Leer IPv4 actual")}
  Button(enabled=input.isNotBlank(),onClick={runCatching{NetworkPlan.parse(input)}.onSuccess{calculated=input;error=""}.onFailure{error=it.message?:"Revisa la dirección"}}){Text("Calcular red")}
  if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
  result?.let{plan->
   Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
    Text("${NetworkPlan.ip(plan.network)}/${plan.prefix}",style=MaterialTheme.typography.titleLarge,color=DeckCyan)
    NetworkPlan.details(plan).dropLast(1).forEach{(label,value)->Column{Text(label,style=MaterialTheme.typography.labelSmall);Text(value,style=MaterialTheme.typography.bodyMedium)}}
   }}
   OutlinedButton(onClick={save(DiagnosticResult("cidr",calculated,Instant.now().toString(),"Cálculo IPv4 local; no es una medición de equipos",NetworkPlan.details(plan)))}){Text("Guardar cálculo")}
   Text(NetworkPlan.details(plan).last().second,style=MaterialTheme.typography.bodySmall)
  }
 }
}
