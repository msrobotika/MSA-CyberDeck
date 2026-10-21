package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable fun UsbBridgePanel(bridge:UsbBridge,hostBusy:Boolean=false,save:()->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("CIBERDECK · ENLACE USB",style=MaterialTheme.typography.titleMedium,color=DeckCyan)
        Text(bridge.status,style=MaterialTheme.typography.bodyMedium)
        if(bridge.connecting || bridge.busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedButton(onClick={bridge.refresh()},enabled=!bridge.connecting){Text("Buscar USB")}
            OutlinedButton(onClick={bridge.disconnect()}){Text("Detener enlace")}
        }
        if(!bridge.connected) {
            if(bridge.drivers.isEmpty())Text("Conecta el ESP32 directamente al OPPO mediante OTG. Dos cables separados al PC no establecen este enlace.",style=MaterialTheme.typography.bodySmall)
            bridge.drivers.forEach { driver ->
                Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(14.dp)){
                    Text("Adaptador %04X:%04X".format(driver.device.vendorId,driver.device.productId),color=DeckCyan)
                    Text(driver.javaClass.simpleName,style=MaterialTheme.typography.bodySmall)
                    Button(enabled=!bridge.connecting && !hostBusy,onClick={bridge.connect(driver)}){Text("Conectar y consultar firmware")}
                }}
            }
        }
        if(bridge.connected) {
            Text(bridge.adapter,style=MaterialTheme.typography.bodySmall)
            bridge.observation?.let { obs ->
                Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    Text(obs.firmware,style=MaterialTheme.typography.titleMedium)
                    Text("SoC anunciado: ${obs.chip?:"este firmware no informa el modelo"}")
                    Text("Protocolo: ${obs.dialect} · Capacidades: ${obs.capabilities.joinToString().ifBlank{"pendientes de consulta"}}",style=MaterialTheme.typography.bodySmall)
                    Text("Identidad declarada por el firmware; no autenticación criptográfica. El VID/PID identifica al adaptador.",style=MaterialTheme.typography.bodySmall)
                }}
            }
            Button(onClick={bridge.send("LEGACY_STATUS")},enabled=!bridge.busy && !hostBusy){Text("Consultar estado MSA")}
            if(bridge.observation?.dialect!=UsbDialect.LEGACY) {
                for((command,title) in listOf("HELLO" to "Identificar SF1", "STATUS" to "Consultar diagnóstico SF1", "CAPABILITIES" to "Consultar capacidades"))
                    OutlinedButton(onClick={bridge.send(command)},enabled=!bridge.busy && !hostBusy && (command=="HELLO" || bridge.observation!=null)){Text(title)}
            }
        }
        DeckDisclosure("Cómo funciona el enlace","115200 8N1. La primera conexión requiere selección y permiso Android. Solo envía consultas de la lista permitida. Durante esta pantalla de la app puede recuperar el mismo adaptador, hasta seis intentos, sin reproducir operaciones de escritura. Cambiar de pantalla a otra herramienta conserva el enlace; salir de la app lo detiene. Varios adaptadores iguales requieren selección. No actualiza firmware, no borra flash y no activa GPIO ni transmisiones RF.")
        DeckSection("Monitor serie","Últimos 300 eventos · ${bridge.dropped} anteriores descartados")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            FilledTonalButton(onClick=save,enabled=bridge.logs.isNotEmpty() && !hostBusy){Text("Guardar registro")}
            TextButton(onClick={bridge.clearLog()}){Text("Vaciar monitor")}
        }
        if(bridge.logs.isEmpty())Text("Sin tramas recibidas. No se muestran datos de demostración.",style=MaterialTheme.typography.bodySmall)
        bridge.logs.takeLast(25).asReversed().forEach { event ->
            Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
                Text("${event.direction} · ${event.at}",style=MaterialTheme.typography.labelSmall,color=if(event.direction in listOf("ERROR","TIMEOUT"))MaterialTheme.colorScheme.error else DeckCyan)
                SelectionContainer{Text(event.text,fontFamily=FontFamily.Monospace,style=MaterialTheme.typography.bodySmall)}
            }}
        }
    }
}

enum class ModuleStage { IMPLEMENTED, EXPERIMENTAL, PENDING }
data class SuperModule(val id:String,val title:String,val stage:ModuleStage,val detail:String)
object SuperModules {
    val all=listOf(
        SuperModule("diagnostics","Diagnóstico, DNS y web",ModuleStage.IMPLEMENTED,"Funciones Android conservadas; un destino elegido por operación."),
        SuperModule("ble","Bluetooth LE",ModuleStage.IMPLEMENTED,"Anuncios y servicios GATT con permiso; sin escrituras de características."),
        SuperModule("nfc","NFC Android",ModuleStage.IMPLEMENTED,"Tecnologías y NDEF disponible; sin clonación de credenciales."),
        SuperModule("reports","Evidencias e informes",ModuleStage.IMPLEMENTED,"Copias privadas, SHA-256, contexto y exportación."),
        SuperModule("usb","USB OTG + monitor",ModuleStage.EXPERIMENTAL,"Código de conexión y reconexión; validación OTG física pendiente."),
        SuperModule("wifi","Wi-Fi Sentinel",ModuleStage.EXPERIMENTAL,"Protocolo existente y radar conservados. Sensor USB externo requerido."),
        SuperModule("rf","Radiofrecuencia",ModuleStage.PENDING,"Requiere transceptor externo y perfil de frecuencias autorizado; el ESP32 no sustituye un módulo sub-GHz."),
        SuperModule("ir","Infrarrojos",ModuleStage.PENDING,"Requiere emisor/receptor y pinout documentado."),
        SuperModule("gpio","GPIO, sensores y electrónica",ModuleStage.PENDING,"Requiere perfil eléctrico real de placa y confirmación para salidas."),
        SuperModule("industrial","MQTT, Modbus, PLC y domótica",ModuleStage.PENDING,"Requiere destino autorizado y control explícito de escrituras."),
        SuperModule("protocols","Analizador de protocolos",ModuleStage.EXPERIMENTAL,"Análisis de tramas JSONL USB; sin captura lógica de buses aún."),
        SuperModule("ai","Asistente IA",ModuleStage.EXPERIMENTAL,"Motor Google LiteRT-LM e importación de Gemma local. Generación pendiente de validar con un modelo real; sin nube ni acciones automáticas."))
}
@Composable fun SuperModulesPanel() {
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("CIBERDECK · MÓDULOS",style=MaterialTheme.typography.titleMedium,color=DeckCyan)
        Text("Base extensible con estado explícito. Un módulo pendiente no ejecuta funciones ni muestra lecturas.")
        SuperModules.all.forEach { module -> Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Text(module.title,style=MaterialTheme.typography.titleMedium)
            Text(when(module.stage){ModuleStage.IMPLEMENTED->"IMPLEMENTADO";ModuleStage.EXPERIMENTAL->"EXPERIMENTAL";ModuleStage.PENDING->"PENDIENTE"},color=DeckCyan,style=MaterialTheme.typography.labelSmall)
            Text(module.detail,style=MaterialTheme.typography.bodySmall)
        }}}
    }
}
