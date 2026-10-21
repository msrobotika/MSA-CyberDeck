package com.msarobotika.cyberdeck

object CallPolicy {
    fun number(raw:String):String? {
        val n=raw.trim().replace(Regex("[ ()-]"),"")
        return n.takeIf{it.matches(Regex("\\+?[0-9]{3,20}"))}
    }
    fun verification(status:Int)=when(status){1->"Verificación de red superada";2->"Verificación de red fallida";else->"Sin verificación disponible"}
    fun warn(marked:Boolean,status:Int)=marked || status==2
    fun masked(number:String)=if(number.length<4)"Número no disponible" else "••••"+number.takeLast(3)
}
