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

/** RSSI controls radius; angles only separate markers and never imply bearing. */
@Composable fun RadioDashboard(sensor: SensorSnapshot) {
    val cyan = Color(0xFF5DE7FF)
    val sweep by rememberInfiniteTransition(label = "radar").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "sweep")
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("RADAR DE SEÑAL", color = cyan)
            Text("RSSI relativo · ángulos decorativos, sin dirección ni distancia", style = MaterialTheme.typography.bodySmall)
            Canvas(Modifier.fillMaxWidth().height(240.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = min(size.width, size.height) * .43f
                for (i in 1..4) drawCircle(cyan.copy(alpha = .2f), radius * i / 4f, center, style = Stroke(1f))
                drawLine(cyan.copy(alpha = .15f), center - Offset(radius, 0f), center + Offset(radius, 0f))
                drawLine(cyan.copy(alpha = .15f), center - Offset(0f, radius), center + Offset(0f, radius))
                drawArc(cyan.copy(alpha = .12f), sweep - 30f, 30f, true,
                    topLeft = center - Offset(radius, radius), size = Size(radius * 2f, radius * 2f))
                val angle = Math.toRadians(sweep.toDouble())
                drawLine(cyan.copy(alpha = .6f), center,
                    center + Offset((cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat()), 2f)
                sensor.networks.sortedBy { it.bssid }.forEachIndexed { index, ap ->
                    val a = index * 2.399963
                    val r = ((-ap.rssi - 30f) / 70f).coerceIn(.12f, 1f) * radius
                    val point = center + Offset((cos(a) * r).toFloat(), (sin(a) * r).toFloat())
                    drawCircle(cyan.copy(alpha = .13f), 11f, point)
                    drawCircle(cyan, 4f, point)
                }
                drawCircle(Color.White, 3f, center)
            }
            Text("${sensor.networks.size} redes · última lectura USB", style = MaterialTheme.typography.bodySmall)
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
