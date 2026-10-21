package com.msarobotika.cyberdeck

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Ornament follows runtime state. Rings are not a radar or sensor reading. */
@Composable fun AgentHud(agent:AgentSession,speaking:Boolean,detail:String=""){
 val compact=agent.plan!=null || agent.run!=null || agent.planning
 val active=agent.planning || agent.running || speaking
 val failed=agent.error.isNotBlank() || agent.results.any{it.error.isNotBlank()}
 val accent=if(failed)DeckAmber else if(speaking)DeckMint else DeckCyan
 val status=when{speaking->"LEYENDO RESUMEN";agent.running->"EJECUTANDO";agent.planning->"PREPARANDO PLAN";failed->"REVISAR RESULTADO";agent.results.any{it.awaiting}->"ESPERA TU INTERVENCIÓN";agent.run!=null->AgentSummary.from(agent)?.let{if(it.title=="Revisión finalizada")"TAREA FINALIZADA" else it.title.uppercase(java.util.Locale.ROOT)}?:"TAREA DETENIDA";agent.plan!=null->"PLAN PREPARADO";else->"EN ESPERA"}
 val motion=android.animation.ValueAnimator.areAnimatorsEnabled()
 val angle=if(active && motion){val transition=rememberInfiniteTransition(label="Estado del agente");val turn by transition.animateFloat(0f,360f,infiniteRepeatable(tween(7000,easing=LinearEasing)),label="Aro de actividad");turn}else 0f
 Surface(modifier=Modifier.fillMaxWidth().testTag("agent-command-center"),shape=RoundedCornerShape(20.dp),color=Color(0xFF091C28),border=BorderStroke(1.dp,accent.copy(alpha=.45f))){
  Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF103847),Color(0xFF09141F))))){
   Canvas(Modifier.matchParentSize()){
    val step=28.dp.toPx()
    var x=0f;while(x<size.width){drawLine(accent.copy(alpha=.035f),Offset(x,0f),Offset(x,size.height));x+=step}
    var y=0f;while(y<size.height){drawLine(accent.copy(alpha=.035f),Offset(0f,y),Offset(size.width,y));y+=step}
    drawLine(accent.copy(alpha=.7f),Offset(16.dp.toPx(),0f),Offset(size.width*.38f,0f),2.dp.toPx())
   }
   Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
     Box(Modifier.size(if(compact)48.dp else 64.dp),contentAlignment=Alignment.Center){
      Canvas(Modifier.fillMaxSize()){
       val w=size.width
       drawCircle(accent.copy(alpha=.15f),w*.48f,style=Stroke(1.dp.toPx()))
       drawArc(accent,angle-65f,104f,false,Offset(w*.04f,w*.04f),Size(w*.92f,w*.92f),style=Stroke(2.dp.toPx()))
       drawArc(accent.copy(alpha=.4f),angle+115f,75f,false,Offset(w*.04f,w*.04f),Size(w*.92f,w*.92f),style=Stroke(2.dp.toPx()))
       for(i in 0..3){val a=Math.toRadians((45+i*90).toDouble());val dx=kotlin.math.cos(a).toFloat();val dy=kotlin.math.sin(a).toFloat();drawLine(accent.copy(alpha=.5f),center+Offset(dx*w*.40f,dy*w*.40f),center+Offset(dx*w*.46f,dy*w*.46f),1.dp.toPx())}
      }
      Image(painterResource(R.drawable.cyberdeck_robot),"Robot original MSA Robotika",Modifier.size(if(compact)36.dp else 48.dp))
     }
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){
      Text("MSA ROBOTIKA / AGENTE",fontFamily=FontFamily.Monospace,fontSize=10.sp,letterSpacing=1.sp,color=accent)
      Text(if(agent.plan==null && agent.run==null && !agent.planning)"¿Qué hacemos?" else "CiberDeck",style=MaterialTheme.typography.titleLarge)
      if(!compact || active)Text(if(active)detail.ifBlank{agent.activeTool?.label?:if(speaking)"Voz del sistema" else "Preparando herramientas"} else "Pide una tarea por voz o texto.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     }
    }
    HorizontalDivider(color=accent.copy(alpha=.2f))
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
     Canvas(Modifier.size(6.dp)){drawCircle(accent)}
     Text(status,modifier=Modifier.weight(1f).testTag("agent-hud-status"),fontFamily=FontFamily.Monospace,fontSize=10.sp,color=accent)
     val count=AgentAnswerPolicy.observations(agent).size
     Text("$count ${if(count==1)"resultado" else "resultados"}",fontFamily=FontFamily.Monospace,fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
  }
 }
}
