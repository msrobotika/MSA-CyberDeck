package com.msarobotika.cyberdeck

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

private val toolGuides=mapOf(
    "USB" to listOf("Necesitas un cable de datos OTG y el ESP32 conectado directamente a este móvil.","Pulsa Buscar USB, elige el adaptador y permite el acceso en Android. Después consulta el firmware.","Las respuestas aparecerán en el monitor serie. Guarda el registro si quieres conservarlo; una identidad anunciada no autentica la placa."),
    "Bluetooth" to listOf("Activa Bluetooth y concede los permisos que solicite Android.","Inicia la exploración y espera el resultado. La consulta GATT requiere elegir un dispositivo autorizado.","La señal y los anuncios describen lo observado; no prueban identidad, distancia ni que un dispositivo sea malicioso."),
    "NFC" to listOf("Necesitas NFC activo y una etiqueta compatible propia.","Inicia la lectura y acerca la etiqueta a la parte trasera del móvil. Mantén la etiqueta quieta unos segundos.","Revisa las tecnologías y datos que hayan podido leerse. Detectar una tarjeta no significa poder leer datos protegidos ni copiar su credencial."),
    "Recibir" to listOf("Pon los dos equipos en la misma Wi-Fi de confianza. El otro solo necesita navegador.","Prepara la recepción, abre su QR o dirección en el otro equipo y elige allí el archivo. Confirma el guardado en este móvil.","El archivo aceptado queda en Evidencias. Cierra la recepción al acabar; esta modalidad usa HTTP local sin cifrar."),
    "Red" to listOf("Elige un dominio o IP de tu ámbito autorizado. Para TCP necesitas además el puerto.","Resuelve el destino o comprueba el puerto con los botones de esta pantalla.","Verás la respuesta o el error y podrás guardar el resultado. Una conexión aceptada no identifica el servicio ni demuestra una vulnerabilidad."),
    "Web" to listOf("Necesitas conexión y una URL de tu ámbito autorizado.","Introduce la dirección y pulsa Revisar URL. Se envía una consulta HEAD sin seguir redirecciones.","Revisa TLS y cabeceras o el error obtenido. Es una comprobación puntual, no una certificación de seguridad del sitio.")
)

fun hasToolGuide(route:String)=route in toolGuides

@Composable fun DeckToolGuide(route:String) {
    val steps=toolGuides[route]?:return
    var expanded by rememberSaveable(route){mutableStateOf(false)}
    Card(Modifier.fillMaxWidth()){
        Column(Modifier.padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            TextButton(onClick={expanded=!expanded},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("tool-guide").semantics{stateDescription=if(expanded)"Desplegado" else "Plegado"}){
                Text("Cómo empezar",Modifier.weight(1f))
                Text(if(expanded)"−" else "+")
            }
            if(expanded){
                steps.forEachIndexed { index,text -> Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    Text("${index+1}",color=DeckCyan,style=MaterialTheme.typography.titleSmall)
                    Text(text,Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
                }}
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
