package com.msarobotika.cyberdeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF071018); private val Panel = Color(0xFF101E2D); private val Cyan = Color(0xFF5DE7FF); private val Green = Color(0xFF62F5B0); private val Amber = Color(0xFFFFD166)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { CyberDeck() } }
}

@Composable fun CyberDeck() {
    var tab by remember { mutableStateOf("Resumen") }
    MaterialTheme(colorScheme = darkColorScheme(background = Navy, surface = Panel, primary = Cyan)) {
        Surface(color = Navy, modifier = Modifier.fillMaxSize()) {
            LazyColumn(contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { Text("MSA_CyberDeck", color = Cyan, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("OFFLINE-FIRST SECURITY WORKBENCH", color = Color(0xFF8DA2BD), fontSize = 12.sp); Spacer(Modifier.height(8.dp)); StatusChip("● LABORATORIO LOCAL", Green) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Resumen","Sensores","Evidencias","Informes").forEach { label -> FilterChip(selected = tab == label, onClick = { tab = label }, label = { Text(label) }) } } }
                item { when (tab) { "Sensores" -> Sensors(); "Evidencias" -> Evidence(); "Informes" -> Reports(); else -> Overview() } }
            }
        }
    }
}

@Composable private fun Overview() { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Section("POSTURA DE SEGURIDAD", "Preparada para auditoría autorizada", Green); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Metric("3", "sensores"); Metric("0", "alertas críticas"); Metric("12", "evidencias") }; Section("ÚLTIMO EVENTO", "WiFi Sentinel · red observada · hace 2 min", Amber); Section("SIGUIENTE PASO", "Conecta un ESP32 por USB o importa un informe JSON", Cyan) } }
@Composable private fun Sensors() { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Section("SENSORES CONECTADOS", "WiFi Sentinel · online", Green); Section("ROGUEWATCH", "USB pendiente · conectar para telemetría", Amber); Section("CHIPSAFE", "auditoría disponible", Cyan); Button(onClick = {}) { Text("BUSCAR USB / BLE") } } }
@Composable private fun Evidence() { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Section("CADENA DE CUSTODIA", "12 elementos · hashes locales verificados", Green); Section("EXPORTACIÓN", "JSON y Markdown listos para informe", Cyan); Button(onClick = {}) { Text("AÑADIR EVIDENCIA") } } }
@Composable private fun Reports() { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Section("INFORMES", "Sincronización local disponible", Cyan); Section("PLANTILLAS", "Estudiante · auditoría · laboratorio", Green); Button(onClick = {}) { Text("GENERAR INFORME") } } }
@Composable private fun Section(title: String, body: String, color: Color) { Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(16.dp)) { Text(title, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(body, color = Color.White, fontSize = 16.sp) } } }
@Composable private fun Metric(value: String, label: String) { Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.weight(1f)) { Column(Modifier.padding(14.dp)) { Text(value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text(label, color = Color(0xFF8DA2BD), fontSize = 12.sp) } } }
@Composable private fun StatusChip(text: String, color: Color) { Text(text, color = color, modifier = Modifier.background(color.copy(alpha=.12f), RoundedCornerShape(18.dp)).padding(horizontal=12.dp, vertical=8.dp), fontSize = 12.sp) }
