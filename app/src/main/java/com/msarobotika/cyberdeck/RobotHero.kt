package com.msarobotika.cyberdeck

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable fun RobotHero(connected: Boolean, warnings: Long) {
    var motion by rememberSaveable { mutableStateOf(true) }
    val pulse = if (motion) {
        val transition = rememberInfiniteTransition(label = "mascota")
        val value by transition.animateFloat(.65f, 1f,
            infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "respiracion")
        value
    } else 1f
    val cyan = Color(0xFF5DE7FF)
    val accent = if (warnings > 0) Color(0xFFFFD166) else cyan
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MSA_ROBOTIKA", color = cyan, style = MaterialTheme.typography.labelLarge)
            Box(Modifier.width(180.dp).aspectRatio(1320f / 1191f).graphicsLayer {
                translationY = if (motion) (pulse - .825f) * 14f else 0f
            }) {
                Image(painterResource(R.drawable.cyberdeck_robot), "Robot de MSA Robotika",
                    Modifier.fillMaxSize())
                Canvas(Modifier.fillMaxSize()) {
                    val points = listOf(Offset(size.width * .423f, size.height * .36f),
                        Offset(size.width * .576f, size.height * .36f),
                        Offset(size.width * .5f, size.height * .775f))
                    points.forEach { point ->
                        val radius = size.width * .065f
                        drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .5f * pulse),
                            Color.Transparent), point, radius), radius, point)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(if (warnings > 0) "El sensor tiene avisos: revísalos"
                else if (connected) "Lectura del sensor disponible" else "Listo para conectar tu sensor",
                color = accent, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { motion = !motion }) {
                Text(if (motion) "Pausar animación" else "Animar robot")
            }
        }
    }
}
