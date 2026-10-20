package com.msarobotika.cyberdeck

/** Frozen sources, captured before generation. Never extracted from a model's answer. */
data class AiSourcePacket(
 val capturedAt:String,val environment:String,val phoneAt:String,val phoneFields:List<Pair<String,String>>,
 val sourceKind:String,val sourceTarget:String,val sourceAt:String,val excerpt:String,
 val sourceId:String="",val sourceHash:String="",val verifiedAt:String=""
)

object AiSources {
 fun capture(input:DiagnosticResult?,phone:DiagnosticResult?,environment:AiContextSnapshot,at:String,id:String="",hash:String="",verifiedAt:String=""):AiSourcePacket {
  // Only typed Android data can populate these rows. User declarations and model text cannot.
  val fields=if(phone?.kind=="phone" && environment.hasPhone)AiContext.observedFields(phone)
   .filter{(key,value)->environment.text.lines().contains("$key: $value")} else emptyList()
  return AiSourcePacket(at,environment.text,if(phone?.kind=="phone" && environment.hasPhone)phone.checkedAt.take(100) else "",fields,
   input?.kind.orEmpty().take(64),input?.target.orEmpty().take(200),input?.checkedAt.orEmpty().take(100),input?.let{AiPolicy.observationText(it,1200)}.orEmpty(),id.take(200),hash.take(64),verifiedAt.take(100))
 }
 fun nature(packet:AiSourcePacket)=when(packet.sourceKind){
  ""->"Sin fuente adicional"
  "ai"->"Interpretación de IA · sin verificar"
  "manual-step"->"Confirmación manual · sin lectura verificada"
  "agent","agent-context","team-context","source-brief"->"Resumen de fuentes · no es una nueva medición"
  else->"Resultado de herramienta · conservar sus límites"
 }
 fun report(packet:AiSourcePacket):DiagnosticResult=DiagnosticResult("source-brief","Fuentes de la consulta",packet.capturedAt,
  "${packet.phoneFields.size} campos Android representados · ${nature(packet)}",
  listOf("Método" to "Resumen preparado por código, sin inferencia de IA. Los valores proceden de las fuentes consultadas, no de una respuesta del modelo.","Fecha de instantánea Android" to packet.phoneAt.ifBlank{"No adjunta"})+
   packet.phoneFields.map{(key,value)->"Android / $key" to value}+
   listOf("Origen de la fuente adicional" to nature(packet),"Tipo de fuente" to packet.sourceKind,"Destino de fuente" to packet.sourceTarget,"Fecha de fuente" to packet.sourceAt,
    "Fragmento de fuente utilizado" to packet.excerpt,"Entorno y objetivo declarados" to packet.environment,"ID fuente guardada" to packet.sourceId,"SHA-256 fuente guardada" to packet.sourceHash,"Verificada" to packet.verifiedAt,
    "Límite" to "Fragmento adicional de hasta 1200 caracteres. Una declaración del usuario no es una observación. No certifica seguridad ni verifica interpretaciones o resultados ajenos."))
}
