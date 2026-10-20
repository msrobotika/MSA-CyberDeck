package com.msarobotika.cyberdeck

import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.time.Instant

data class QrObservation(val payload:String,val type:String,val at:String,val imageSha256:String,val link:LinkObservation?,val note:String){
 fun diagnostic()=DiagnosticResult("qr",link?.asciiHost?:type,at,"QR leído · $type · sin ejecutar el contenido",
  listOf("Tipo" to type,"Caracteres decodificados" to payload.length.toString(),"SHA-256 imagen elegida" to imageSha256.ifBlank{"Sin imagen adjunta"},"Acción" to note)+
   link?.checks.orEmpty()+listOf("Privacidad" to "El informe no conserva el contenido completo, claves Wi-Fi, ruta, consulta ni fragmento. La imagen no se copia al almacén.","Alcance" to "Un QR decodificado de una imagen elegida. No certifica autenticidad, reputación ni ausencia de fraude; no abre enlaces ni ejecuta acciones."))
}
object QrInspection {
 const val MAX_PIXELS=4_194_304
 fun inspect(payload:String,imageSha256:String="",at:String=Instant.now().toString()):QrObservation {
  require(payload.isNotEmpty() && payload.length<=16384){"Contenido QR vacío o demasiado largo"}
  val value=payload.trim()
  val type=when{value.startsWith("https://",true)||value.startsWith("http://",true)->"Enlace web";value.startsWith("WIFI:",true)->"Configuración Wi-Fi";value.startsWith("BEGIN:VCARD",true)||value.startsWith("MECARD:",true)->"Contacto";Regex("(?i)^(intent|javascript|data|file|content):").containsMatchIn(value)->"Acción o contenido especial";Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(value)->"Otro esquema";else->"Texto"}
  val link=if(type=="Enlace web")runCatching{LinkInspection.inspect(value)}.getOrNull() else null
  val note=when(type){"Enlace web"->if(link==null)"La dirección no cumple el formato del inspector; no se abre." else "Se analiza su sintaxis localmente, sin visitar el destino ni seguir redirecciones.";"Configuración Wi-Fi"->"Puede contener una contraseña. No se conecta a la red ni se guarda su contenido completo.";"Contacto"->"No se añade a la agenda ni se guarda el contenido completo.";else->"Se muestra como dato. No se lanza ninguna aplicación ni se interpreta como una instrucción."}
  return QrObservation(payload,type,at,imageSha256,link,note)
 }
 fun decodePixels(width:Int,height:Int,pixels:IntArray):String {
  require(width>0 && height>0 && width.toLong()*height<=MAX_PIXELS && pixels.size.toLong()==width.toLong()*height){"Imagen demasiado grande o dimensiones no válidas"}
  val source=RGBLuminanceSource(width,height,pixels)
  val reader=MultiFormatReader().apply{setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),DecodeHintType.TRY_HARDER to true))}
  for(candidate in listOf(source,source.invert())){
   try{return reader.decodeWithState(BinaryBitmap(HybridBinarizer(candidate))).text}
   catch(_:ReaderException){}finally{reader.reset()}
  }
  throw IllegalArgumentException("No se encontró un QR legible. Prueba una imagen más nítida, con el código completo y margen alrededor.")
 }
}
