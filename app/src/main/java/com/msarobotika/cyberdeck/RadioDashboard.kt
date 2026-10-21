package com.msarobotika.cyberdeck

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.*

/** Real channel number encodes angle, measured RSSI encodes radius; neither is physical bearing. */
@Composable fun RadioDashboard(sensor: SensorSnapshot) {
    val cyan = Color(0xFF5DE7FF)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("RADAR POR CANAL", color = cyan)
            Text("Ángulo = canal 1–13; radio = RSSI. No representa dirección ni distancia física.", style = MaterialTheme.typography.bodySmall)
            Canvas(Modifier.fillMaxWidth().height(240.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = min(size.width, size.height) * .43f
                for (i in 1..4) drawCircle(cyan.copy(alpha = .2f), radius * i / 4f, center, style = Stroke(1f))
                drawLine(cyan.copy(alpha = .15f), center - Offset(radius, 0f), center + Offset(radius, 0f))
                drawLine(cyan.copy(alpha = .15f), center - Offset(0f, radius), center + Offset(0f, radius))
                for(channel in 1..13){val a=RadioGeometry.channelAngle(channel);drawLine(cyan.copy(alpha=.10f),center,center+Offset((cos(a)*radius).toFloat(),(sin(a)*radius).toFloat()),1f)}
                if(sensor.channel in 1..13){val angle=RadioGeometry.channelAngle(sensor.channel)
                    drawLine(DeckMint,center,center+Offset((cos(angle)*radius).toFloat(),(sin(angle)*radius).toFloat()),2f)}
                sensor.networks.filter{it.channel in 1..13}.forEach { ap ->
                    val a = RadioGeometry.channelAngle(ap.channel)
                    val r = RadioGeometry.signalRadius(ap.rssi) * radius
                    val point = center + Offset((cos(a) * r).toFloat(), (sin(a) * r).toFloat())
                    drawCircle(cyan.copy(alpha = .13f), 11f, point)
                    drawCircle(cyan, 4f, point)
                }
                drawCircle(Color.White, 3f, center)
            }
            Text("${sensor.networks.size} redes · lectura puntual USB · canal sintonizado ${sensor.channel}", style = MaterialTheme.typography.bodySmall)
            Text("Escala radial: −30 dBm en el centro, −100 en el borde; valores fuera se limitan. No hay barrido animado que aparente una lectura en vivo.",style=MaterialTheme.typography.bodySmall)
        }
    }
    Spacer(Modifier.height(12.dp))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("REDES POR CANAL · 2,4 GHz", color = cyan)
            val counts = (1..13).map { ch -> sensor.networks.count { it.channel == ch } }
            val maxCount = maxOf(1, counts.maxOrNull() ?: 1)
            Canvas(Modifier.fillMaxWidth().height(110.dp)) {
                val step = size.width / 13f
                counts.forEachIndexed { i, count ->
                    val h = size.height * count / maxCount
                    drawRect(cyan.copy(alpha = .75f), Offset(i * step + 3f, size.height - h), Size(maxOf(1f, step - 6f), h))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (1..13).forEach { Text("$it", style = MaterialTheme.typography.labelSmall) }
            }
            Text("Cuenta redes observadas; no mide ocupación total del espectro.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
