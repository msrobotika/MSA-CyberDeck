package com.msarobotika.cyberdeck

import android.animation.ValueAnimator
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable fun CiberDeckCommand(snapshot:PhoneSnapshot?,busy:Boolean,audit:()->Unit,analyze:()->Unit) {
    var motion by rememberSaveable{mutableStateOf(true)}
    val enabled=motion && ValueAnimator.areAnimatorsEnabled()
    val glow=if(enabled){val value by rememberInfiniteTransition(label="ciberdeck").animateFloat(.08f,.25f,infiniteRepeatable(tween(2200),RepeatMode.Reverse),label="breath");value}else .15f
    Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),border=BorderStroke(1.dp,DeckCyan.copy(alpha=.25f))) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF102B38),Color(0xFF111D2E)))).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){
                Text("TU LABORATORIO",style=MaterialTheme.typography.labelSmall,color=DeckCyan)
                Text("Comprueba tus\nequipos.",style=MaterialTheme.typography.headlineSmall)
            }
                Box(Modifier.size(88.dp).background(Brush.radialGradient(listOf(DeckCyan.copy(alpha=glow),Color.Transparent))),contentAlignment=Alignment.Center){Image(painterResource(R.drawable.cyberdeck_robot),"Robot MSA Robotika",Modifier.fillMaxSize())}
            }
            Text(snapshot?.let{"Último chequeo: ${displayTime(it.at)} · ${it.checks.count{c->c.tone==PhoneTone.REVIEW}} ajustes para revisar"}?:"Elige un equipo, comprueba servicios y guarda las pruebas. También puedes revisar este móvil.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Button(enabled=!busy,onClick=audit,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){Text("Auditar un equipo")}
            OutlinedButton(enabled=!busy,onClick=analyze,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){Text("Analizar mi móvil")}
            TextButton(onClick={motion=!motion},contentPadding=PaddingValues(0.dp),modifier=Modifier.heightIn(min=48.dp)){Text(if(motion)"Pausar efectos" else "Activar efectos",style=MaterialTheme.typography.labelSmall)}
        }
    }
}
@Composable fun CiberDeckActions(navigate:(String)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
            Mission("Mi red","Servicios y conectividad","radio",Modifier.weight(1f)){navigate("Mi red")}
            Mission("Bluetooth","Explorar dispositivos BLE","radio",Modifier.weight(1f)){navigate("Bluetooth")}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
            Mission("NFC","Leer una etiqueta propia","files",Modifier.weight(1f)){navigate("NFC")}
            Mission("Recibir","Traer un archivo","transfer",Modifier.weight(1f)){navigate("Recibir")}
        }
    }
}
@Composable private fun Mission(title:String,subtitle:String,icon:String,modifier:Modifier,click:()->Unit) {
    Card(onClick=click,modifier=modifier.heightIn(min=110.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            DeckGlyph(icon,Modifier.size(22.dp));Text(title,style=MaterialTheme.typography.titleMedium);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
