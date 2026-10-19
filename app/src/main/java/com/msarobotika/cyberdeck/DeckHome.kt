package com.msarobotika.cyberdeck

import android.animation.ValueAnimator
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

data class DeckTool(val route:String,val title:String,val subtitle:String,val group:String,val glyph:String,val badge:String)
val deckTools=listOf(
    DeckTool("Trabajos","Trabajos de auditoría","Objetivos, evidencias fijadas e informe por trabajo","Auditoría","report","Guardado local"),
    DeckTool("CIDR","Plan de red","IPv4, máscara, rango y direcciones utilizables","Conectividad","tools","Cálculo local"),
    DeckTool("Magnético","Campo magnético","µT, ejes, gráfica y referencia del sensor real","Electrónica","radio","Requiere magnetómetro"),
    DeckTool("Equipo","Auditar equipo","Alcance, conexiones TCP, comparación e informe","Auditoría","tools","Un equipo · 8 puertos"),
    DeckTool("IA","Asistente IA","IA local para explicar resultados elegidos","Asistente","report","Modelo local"),
    DeckTool("APK","Inspector APK","Firma, permisos y manifiesto sin instalar","Protección personal","files","Análisis local"),
    DeckTool("Llamadas","Llamadas","Verificación del operador y avisos locales","Protección personal","report","Activación opcional"),
    DeckTool("Enlaces","Analizar enlace","Señales de riesgo sin abrir la dirección","Protección personal","report","Sin conexión"),
    DeckTool("Móvil","Mi móvil","Seguridad, conexión, recursos y hardware","En este móvil","report","Sin periféricos"),
    DeckTool("Mi red","Mi red","Servicios anunciados en tu red local","Conectividad","radio","15 s"),
    DeckTool("Red","DNS y puertos","Resolver un destino y comprobar un puerto TCP","Conectividad","tools","Un destino"),
    DeckTool("Web","Inspección web","Certificado TLS, cabeceras y redirección","Conectividad","report","HEAD"),
    DeckTool("Bluetooth","Bluetooth LE","Anuncios, señal y servicios GATT autorizados","Radio y proximidad","radio","10 s"),
    DeckTool("NFC","NFC","Inspeccionar etiquetas y comparar informes","Radio y proximidad","files","30 s"),
    DeckTool("USB","ESP32 por USB","Identidad, consola y redes del sensor","Hardware y módulos","tools","OTG experimental"),
    DeckTool("Módulos","En desarrollo","Funciones futuras y hardware necesario","Hardware y módulos","tools","Hoja de ruta"),
    DeckTool("Radio","Radar Wi-Fi","Visualizar la última lectura del sensor","Hardware y módulos","radio","Requiere lectura"),
    DeckTool("Recibir","Recibir archivos","Desde el navegador del otro móvil","Archivos y colaboración","transfer","HTTP local"),
    DeckTool("Sesiones","Sesión cifrada","Transferir entre dos CiberDeck con aprobación","Archivos y colaboración","transfer","TLS local"))

@Composable fun RobotCommandHero() {
    var motion by rememberSaveable { mutableStateOf(true) }
    val enabled=motion && ValueAnimator.areAnimatorsEnabled()
    val pulse=if(enabled) {
        val value by rememberInfiniteTransition(label="robot-home").animateFloat(.55f,1f,
            infiniteRepeatable(tween(2200),RepeatMode.Reverse),label="glow")
        value
    } else .8f
    Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF101E30)),border=BorderStroke(1.dp,DeckCyan.copy(alpha=.24f))) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF11293A),Color(0xFF121B30)))).padding(18.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TU LABORATORIO",style=MaterialTheme.typography.labelSmall,fontFamily=FontFamily.Monospace,color=DeckCyan)
                    Spacer(Modifier.height(8.dp))
                    Text("Contigo.\nBajo tu control.",style=MaterialTheme.typography.headlineSmall)
                }
                Box(Modifier.size(116.dp),contentAlignment=Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(Brush.radialGradient(listOf(DeckCyan.copy(alpha=.22f*pulse),Color.Transparent)),size.minDimension*.48f)
                        for(i in 1..3)drawCircle(DeckCyan.copy(alpha=.08f),size.minDimension*.15f*i,style=androidx.compose.ui.graphics.drawscope.Stroke(1f))
                    }
                    Image(painterResource(R.drawable.cyberdeck_robot),"Robot MSA Robotika",Modifier.fillMaxSize())
                    Canvas(Modifier.fillMaxSize()) {
                        for(x in listOf(.423f,.576f))drawCircle(Brush.radialGradient(listOf(DeckCyan.copy(alpha=.4f*pulse),Color.Transparent),Offset(size.width*x,size.height*.36f),size.width*.075f),size.width*.075f,Offset(size.width*x,size.height*.36f))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Observa, comprueba y conserva lo que importa.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick={motion=!motion},contentPadding=PaddingValues(0.dp)) { Text(if(motion) "Pausar efectos" else "Activar efectos",style=MaterialTheme.typography.labelSmall) }
        }
    }
}

@Composable fun DeckStats(entries:List<Evidence>, sensorReady:Boolean) {
    val big=LocalConfiguration.current.fontScale>1.4f
    val values=listOf("${entries.size}" to "Evidencias",displayBytes(entries.sumOf{it.bytes}) to "Guardado",(if(sensorReady) "Sí" else "No") to "Lectura ESP")
    if(big) Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {values.forEach {Metric(it.first,it.second,Modifier.fillMaxWidth())}}
    else Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {values.forEach {Metric(it.first,it.second,Modifier.weight(1f))}}
}
@Composable private fun Metric(value:String,label:String,modifier:Modifier) {
    Card(modifier,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)){Text(value,style=MaterialTheme.typography.titleMedium,color=DeckCyan);Text(label,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
}

@Composable fun DeckQuickActions(navigate:(String)->Unit) {
    val big=LocalConfiguration.current.fontScale>1.4f
    if(big) Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        QuickAction("Mi red","Consultar servicios locales","radio",Modifier.fillMaxWidth()){navigate("Mi red")}
        QuickAction("Recibir","Traer un archivo de otro móvil","transfer",Modifier.fillMaxWidth()){navigate("Recibir")}
    } else Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        QuickAction("Mi red","Servicios locales","radio",Modifier.weight(1f)){navigate("Mi red")}
        QuickAction("Recibir","Archivos de otro móvil","transfer",Modifier.weight(1f)){navigate("Recibir")}
    }
}
@Composable private fun QuickAction(title:String,subtitle:String,glyph:String,modifier:Modifier,click:()->Unit) {
    Card(onClick=click,modifier=modifier.heightIn(min=110.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer.copy(alpha=.45f)),border=BorderStroke(1.dp,DeckCyan.copy(alpha=.15f))) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            DeckGlyph(glyph);Text(title,style=MaterialTheme.typography.titleMedium);Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun DeckToolSearch(query:String, changed:(String)->Unit) {
    val focus=LocalFocusManager.current
    OutlinedTextField(query,changed,label={Text("Buscar una herramienta")},placeholder={Text("NFC, DNS, archivos…")},
        singleLine=true,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),keyboardActions=KeyboardActions(onDone={focus.clearFocus()}),modifier=Modifier.fillMaxWidth())
}
@Composable fun DeckToolCard(tool:DeckTool,open:()->Unit) {
    Card(onClick=open,modifier=Modifier.fillMaxWidth().testTag("tool-${tool.route}"),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center){DeckGlyph(tool.glyph)}
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(tool.title,style=MaterialTheme.typography.titleMedium)
                Text(tool.subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(tool.badge,style=MaterialTheme.typography.labelSmall,color=DeckCyan)
            }
        }
    }
}

@Composable fun DeckRecent(entry:Evidence,click:()->Unit) {
    Card(onClick=click,modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            DeckGlyph("files",tint=DeckViolet)
            Column(Modifier.weight(1f)){Text(entry.name,style=MaterialTheme.typography.bodyMedium);Text("${displayBytes(entry.bytes)} · ${displayTime(entry.importedAt)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }
    }
}
