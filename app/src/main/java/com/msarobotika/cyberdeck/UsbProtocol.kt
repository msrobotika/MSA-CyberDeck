package com.msarobotika.cyberdeck

import org.json.JSONObject
import java.io.ByteArrayOutputStream

/** Bounded UTF-8 JSONL framing. Oversized frames are discarded through the newline. */
class UsbFrameDecoder(private val limit:Int=4096) {
    private val bytes=ByteArrayOutputStream()
    private var discard=false
    fun clear(){bytes.reset();discard=false}
    fun accept(input:ByteArray):List<String> = buildList {
        for(b in input) when {
            b==10.toByte()->{if(!discard && bytes.size()>0)add(bytes.toString("UTF-8").trimEnd('\r'));this@UsbFrameDecoder.clear()}
            discard->Unit
            bytes.size()>=limit->{bytes.reset();discard=true}
            else->bytes.write(b.toInt())
        }
    }
}
enum class UsbDialect { UNKNOWN, JSON, SF1, LEGACY }
data class UsbObservation(val dialect:UsbDialect,val firmware:String,val chip:String?,val capabilities:List<String>)
object UsbProtocol {
    val commands=setOf("HELLO","STATUS","CAPABILITIES","LEGACY_STATUS")
    fun request(command:String,id:Long,dialect:UsbDialect):String {
        require(command in commands){"Comando fuera de la lista de consultas"}
        return when {
            command=="LEGACY_STATUS"->"{\"cmd\":\"status\"}\n"
            dialect==UsbDialect.SF1->"SF1 $command\n"
            else->JSONObject().put("sf",1).put("id",id).put("cmd",command).toString()+"\n"
        }
    }
    fun matches(line:String,command:String,id:Long,dialect:UsbDialect):Boolean {
        val j=runCatching{JSONObject(line)}.getOrNull()?:return false
        if(command=="LEGACY_STATUS")return j.optString("result")=="status" ||
            (j.optInt("sf")==1 && j.optString("cmd")=="STATUS" && j.optBoolean("ok"))
        if(j.optInt("sf")!=1 || j.optString("cmd")!=command || !j.optBoolean("ok"))return false
        return dialect==UsbDialect.SF1 || (j.has("id") && j.optLong("id",-1)==id)
    }
    fun identity(line:String):UsbObservation? {
        val j=runCatching{JSONObject(line)}.getOrNull()?:return null
        val fw=j.optString("firmware").take(160)
        if(j.optInt("sf")==1 && j.optBoolean("ok") && fw.isNotBlank()) {
            val caps=j.optJSONArray("capabilities")
            return UsbObservation(if(j.has("id") || j.optString("protocol")=="json-v1")UsbDialect.JSON else UsbDialect.SF1,fw,
                j.optString("chip").takeIf{it.isNotBlank()}?.take(100),
                if(caps==null)emptyList() else (0 until minOf(caps.length(),32)).map{caps.optString(it).take(80)})
        }
        if(j.optString("result")=="status" && listOf("MSA-WiFi-Sentinel-","MSA-BLE-Scout-").any{fw.startsWith(it)})
            return UsbObservation(UsbDialect.LEGACY,fw,null,listOf("LEGACY_STATUS"))
        return null
    }
}
