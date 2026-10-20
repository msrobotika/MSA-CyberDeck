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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable fun CiberDeckCommand(snapshot:PhoneSnapshot?,busy:Boolean,analyze:()->Unit,agent:()->Unit={}) {
    var motion by rememberSaveable{mutableStateOf(true)}
    val enabled=motion && ValueAnimator.areAnimatorsEnabled()
    val glow=if(enabled){val value by rememberInfiniteTransition(label="ciberdeck").animateFloat(.08f,.25f,infiniteRepeatable(tween(2200),RepeatMode.Reverse),label="breath");value}else .15f
    Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),border=BorderStroke(1.dp,DeckCyan.copy(alpha=.25f))) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF102B38),Color(0xFF111D2E)))).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){
                Text("CIBERDECK · EN TU MÓVIL",style=MaterialTheme.typography.labelSmall,color=DeckCyan)
                Text("Tu próxima\ncomprobación.",style=MaterialTheme.typography.headlineSmall)
            }
                Box(Modifier.size(88.dp).background(Brush.radialGradient(listOf(DeckCyan.copy(alpha=glow),Color.Transparent))),contentAlignment=Alignment.Center){Image(painterResource(R.drawable.cyberdeck_robot),"Robot MSA Robotika",Modifier.fillMaxSize())}
            }
            Text(snapshot?.let{"Último chequeo: ${displayTime(it.at)} · ${it.checks.count{c->c.tone==PhoneTone.REVIEW}} ajustes para revisar"}?:"Empieza por los ajustes de este móvil o elige abajo qué quieres comprobar.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Button(enabled=!busy,onClick=analyze,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp).testTag("home-phone")){Text(if(snapshot==null)"Revisar este móvil" else "Actualizar revisión del móvil")}
            OutlinedButton(enabled=!busy,onClick=agent,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("home-agent")){Text("Pedir una tarea al agente")}
            TextButton(onClick={motion=!motion},contentPadding=PaddingValues(0.dp),modifier=Modifier.heightIn(min=48.dp)){Text(if(motion)"Pausar animación" else "Activar animación",style=MaterialTheme.typography.labelSmall)}
        }
    }
}
@Composable fun CiberDeckActions(openArea:(String)->Unit) {
    DeckAreaGrid("home-area",openArea)
}
@Composable fun DeckAreaGrid(tagPrefix:String="tool-area",openArea:(String)->Unit) {
    val large=androidx.compose.ui.platform.LocalConfiguration.current.fontScale>1.3f
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        DeckCatalog.areas.chunked(if(large)1 else 2).forEach { areas ->
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                areas.forEach { area ->
                    Mission(area.title,area.description,area.glyph,Modifier.weight(1f).testTag("$tagPrefix-${area.id}")){openArea(area.id)}
                }
            }
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
