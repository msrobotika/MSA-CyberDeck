package com.msarobotika.cyberdeck

data class Ipv4Plan(val address:Long,val prefix:Int){
 val mask=if(prefix==0)0L else (0xffffffffL shl (32-prefix)) and 0xffffffffL
 val network=address and mask
 val broadcast=network or (0xffffffffL xor mask)
 val count=1L shl (32-prefix)
 val first=if(prefix>=31)network else network+1
 val last=if(prefix>=31)broadcast else broadcast-1
 val usable=if(prefix>=31)count else count-2
}
object NetworkPlan {
 fun parse(raw:String):Ipv4Plan {
  val parts=raw.trim().split('/');require(parts.size==2){"Usa una IPv4 y prefijo, por ejemplo 192.168.1.20/24"}
  val octets=parts[0].split('.');require(octets.size==4){"Se requieren cuatro octetos IPv4"}
  val values=octets.map{require(it.matches(Regex("0|[1-9][0-9]{0,2}"))){"Octeto inválido o ambiguo"};it.toInt().also{v->require(v in 0..255){"Octeto fuera de 0–255"}}}
  require(parts[1].matches(Regex("0|[1-9][0-9]?"))){"Prefijo inválido"}
  val prefix=parts[1].toInt();require(prefix in 0..32){"El prefijo debe estar entre 0 y 32"}
  return Ipv4Plan(values.fold(0L){a,n->(a shl 8) or n.toLong()},prefix)
 }
 fun ip(value:Long)=(3 downTo 0).joinToString("."){((value ushr (it*8)) and 255).toString()}
 fun details(plan:Ipv4Plan)=listOf(
  "Dirección introducida" to "${ip(plan.address)}/${plan.prefix}","Máscara" to ip(plan.mask),"Red" to ip(plan.network),
  (if(plan.prefix>=31)"Última dirección del bloque" else "Broadcast calculado") to ip(plan.broadcast),
  "Rango utilizable según prefijo" to "${ip(plan.first)} – ${ip(plan.last)}","Direcciones del bloque" to plan.count.toString(),"Utilizables según prefijo" to plan.usable.toString(),
  "Límite" to "Cálculo matemático, no detección de equipos ni concesiones DHCP. /31 trata las dos direcciones como utilizables para enlaces punto a punto; /32 es una ruta de host. No acredita que todas las direcciones sean asignables en tu entorno.")
}
