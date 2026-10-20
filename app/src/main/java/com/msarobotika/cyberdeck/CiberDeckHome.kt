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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable fun CiberDeckCommand(snapshot:PhoneSnapshot?,busy:Boolean,analyze:()->Unit,agent:()->Unit={},investigate:()->Unit={}) {
    var motion by rememberSaveable{mutableStateOf(true)}
    val enabled=motion && ValueAnimator.areAnimatorsEnabled()
    val glow=if(enabled){val value by rememberInfiniteTransition(label="ciberdeck").animateFloat(.08f,.25f,infiniteRepeatable(tween(2200),RepeatMode.Reverse),label="breath");value}else .15f
    Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),border=BorderStroke(1.dp,DeckCyan.copy(alpha=.3f))) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF123039),Color(0xFF111A29),Color(0xFF171B30))))){
            Canvas(Modifier.matchParentSize()){
                val step=28.dp.toPx()
                for(x in 0..(size.width/step).toInt())drawLine(DeckCyan.copy(alpha=.035f),androidx.compose.ui.geometry.Offset(x*step,0f),androidx.compose.ui.geometry.Offset(x*step,size.height))
                for(y in 0..(size.height/step).toInt())drawLine(DeckCyan.copy(alpha=.035f),androidx.compose.ui.geometry.Offset(0f,y*step),androidx.compose.ui.geometry.Offset(size.width,y*step))
            }
            Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text("¿POR DÓNDE EMPEZAMOS?",Modifier.weight(1f),fontFamily=FontFamily.Monospace,fontSize=10.sp,letterSpacing=1.sp,color=DeckCyan)
                    IconButton(onClick={motion=!motion},modifier=Modifier.size(48.dp).semantics{contentDescription=if(motion)"Pausar animación" else "Activar animación"}){Text(if(motion)"Ⅱ" else "▷",color=DeckCyan)}
                }
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
                        Text("Elige qué\nquieres hacer.",fontSize=23.sp,lineHeight=27.sp,fontWeight=FontWeight.Bold,letterSpacing=(-.5).sp)
                        Text("CiberDeck te lleva al siguiente paso.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(Modifier.size(84.dp).background(Brush.radialGradient(listOf(DeckCyan.copy(alpha=glow),Color.Transparent))),contentAlignment=Alignment.Center){
                        Canvas(Modifier.fillMaxSize()){
                            drawCircle(DeckCyan.copy(alpha=.12f),size.minDimension*.47f,style=androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                            drawArc(DeckCyan.copy(alpha=.5f),-40f,78f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                        }
                        Image(painterResource(R.drawable.cyberdeck_robot),"Robot MSA Robotika",Modifier.fillMaxSize().padding(4.dp))
                    }
                }
                Button(enabled=!busy,onClick=analyze,shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth().heightIn(min=52.dp).testTag("home-phone")){
                    DeckGlyph("shield",Modifier.size(20.dp),MaterialTheme.colorScheme.onPrimary)
                    Text(if(snapshot==null)"Revisar este móvil" else "Revisar este móvil",Modifier.weight(1f).padding(horizontal=10.dp))
                    DeckGlyph("forward",Modifier.size(16.dp),MaterialTheme.colorScheme.onPrimary)
                }
                OutlinedButton(enabled=!busy,onClick=agent,shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("home-agent")){
                    DeckGlyph("spark",Modifier.size(20.dp),DeckViolet)
                    Text("Pedir ayuda al agente",Modifier.weight(1f).padding(horizontal=10.dp))
                    DeckGlyph("forward",Modifier.size(16.dp))
                }
                OutlinedButton(enabled=!busy,onClick=investigate,shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("home-investigation")){
                    DeckGlyph("report",Modifier.size(20.dp),DeckMint)
                    Text("Ver qué ha cambiado",Modifier.weight(1f).padding(horizontal=10.dp))
                    DeckGlyph("forward",Modifier.size(16.dp))
                }
                Text(snapshot?.let{"${displayTime(it.at)} · ${it.checks.count{c->c.tone==PhoneTone.REVIEW}} ajustes para revisar"}?:"Empieza por una revisión del móvil",fontFamily=FontFamily.Monospace,fontSize=10.sp,lineHeight=15.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
@Composable fun CiberDeckActions(openArea:(String)->Unit) {
    DeckAreaGrid("home-area",openArea)
}
@Composable fun DeckRecentTools(routes:List<String>,open:(String)->Unit,clear:()->Unit) {
    Column(Modifier.fillMaxWidth().testTag("home-recent-tools"),verticalArrangement=Arrangement.spacedBy(4.dp)){
        DeckSection("Volver a una herramienta",action="Limpiar",click=clear)
        routes.mapNotNull{route->deckTools.firstOrNull{it.route==route}}.forEach { tool ->
            OutlinedButton(onClick={open(tool.route)},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("recent-${tool.route}")) {
                DeckGlyph(tool.glyph,Modifier.size(20.dp))
                Text(tool.title,Modifier.weight(1f).padding(horizontal=12.dp))
                Text("Abrir",style=MaterialTheme.typography.labelSmall)
            }
        }
    }
}
@Composable fun DeckAreaGrid(tagPrefix:String="tool-area",openArea:(String)->Unit) {
    val large=androidx.compose.ui.platform.LocalConfiguration.current.fontScale>1.3f
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        DeckCatalog.areas.chunked(if(large)1 else 2).forEach { areas ->
            Row(Modifier.height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                areas.forEach { area ->
                    Mission(area,Modifier.weight(1f).fillMaxHeight().testTag("$tagPrefix-${area.id}")){openArea(area.id)}
                }
            }
        }
    }
}
@Composable private fun Mission(area:DeckArea,modifier:Modifier,click:()->Unit) {
    val accent=deckAreaAccent(area.id)
    Card(onClick=click,modifier=modifier.heightIn(min=154.dp),border=BorderStroke(1.dp,accent.copy(alpha=.2f)),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(accent.copy(alpha=.06f),Color.Transparent))).padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Box(Modifier.size(34.dp).background(accent.copy(alpha=.12f),RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){DeckGlyph(area.glyph,Modifier.size(22.dp),accent)}
                Spacer(Modifier.weight(1f));DeckGlyph("forward",Modifier.size(14.dp),accent.copy(alpha=.7f))
            }
            Text(area.title,style=MaterialTheme.typography.titleMedium)
            Text(area.description,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("${area.routes.size} herramientas",fontFamily=FontFamily.Monospace,fontSize=10.sp,color=accent)
        }
    }
}
