package com.msarobotika.cyberdeck

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import com.hoho.android.usbserial.driver.*
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.time.Instant
import java.util.concurrent.Executors
import org.json.JSONObject

data class UsbLog(val at:String,val direction:String,val text:String)

/** Explicit first consent, bounded reconnect and read-only command allowlist. Main-thread API. */
class UsbBridge(private val activity:Activity) {
    private val usb=activity.getSystemService(Context.USB_SERVICE) as UsbManager
    private val main=Handler(Looper.getMainLooper())
    private val worker=Executors.newSingleThreadExecutor()
    private val action=activity.packageName+".SUPERFLYPER_USB_PERMISSION"
    private val decoder=UsbFrameDecoder()
    private var port:UsbSerialPort?=null
    private var activeDeviceId:Int?=null
    private var io:SerialInputOutputManager?=null
    private var epoch=0L
    private var seq=0L
    private var wanted:Pair<Int,Int>?=null
    private var wantedSerial:String?=null
    private var pendingPermission:Int?=null
    private var closed=false
    private var retry=0
    private var deadline:Runnable?=null
    private var reconnect:Runnable?=null
    private var pending:Triple<String,Long,UsbDialect>?=null
    var drivers by mutableStateOf<List<UsbSerialDriver>>(emptyList());private set
    var status by mutableStateOf("Sin sesión USB · conecta el ESP32 al teléfono por OTG");private set
    var connected by mutableStateOf(false);private set
    var connecting by mutableStateOf(false);private set
    var observation by mutableStateOf<UsbObservation?>(null);private set
    var logs by mutableStateOf<List<UsbLog>>(emptyList());private set
    var dropped by mutableIntStateOf(0);private set
    var busy by mutableStateOf(false);private set
    var adapter by mutableStateOf("");private set
    private fun log(direction:String,text:String){if(logs.size>=300)dropped++;logs=(logs+UsbLog(Instant.now().toString(),direction,text.take(4096))).takeLast(300)}
    private val receiver=object:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){
        if(closed)return
        @Suppress("DEPRECATION") val device=intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
        when(intent.action){
            action->{
                if(pendingPermission==null)return
                if(device?.deviceId!=pendingPermission)return
                pendingPermission=null;connecting=false
                if(device!=null && usb.hasPermission(device))drivers.firstOrNull{it.device.deviceId==device.deviceId}?.let{open(it)}
                else{wanted=null;status="Permiso USB denegado o dispositivo retirado";log("EVENT",status)}
            }
            UsbManager.ACTION_USB_DEVICE_DETACHED->{
                if(device?.deviceId==activeDeviceId || device?.deviceId==pendingPermission){closePort();status="Cable retirado · esperando el mismo adaptador";log("EVENT",status)}
                refresh()
            }
            UsbManager.ACTION_USB_DEVICE_ATTACHED->{retry=0;refresh();scheduleReconnect()}
        }
    }}
    init {
        ContextCompat.registerReceiver(activity,receiver,IntentFilter(action).apply{
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        },ContextCompat.RECEIVER_NOT_EXPORTED)
        refresh()
    }
    fun refresh(){if(!closed){drivers=UsbSerialProber.getDefaultProber().findAllDrivers(usb);if(!connected && !connecting && wanted==null)status=if(drivers.isEmpty())"Sin adaptadores USB serie en el teléfono" else "${drivers.size} adaptadores · selecciona el tuyo"}}
    fun connect(driver:UsbSerialDriver){
        if(closed)return
        disconnect();wanted=driver.device.vendorId to driver.device.productId;retry=0
        requestOrOpen(driver)
    }
    private fun requestOrOpen(driver:UsbSerialDriver){
        if(usb.hasPermission(driver.device)){open(driver);return}
        connecting=true;pendingPermission=driver.device.deviceId;status="Autoriza el adaptador en Android"
        val pi=PendingIntent.getBroadcast(activity,driver.device.deviceId,Intent(action).setPackage(activity.packageName),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        runCatching{usb.requestPermission(driver.device,pi)}.onFailure{pendingPermission=null;connecting=false;wanted=null;status="No se pudo solicitar permiso: ${it.message}";log("ERROR",status)}
    }
    private fun open(driver:UsbSerialDriver){
        if(closed || connected || connecting && pendingPermission!=null)return
        connecting=true;val generation=++epoch;status="Abriendo USB · 115200 8N1"
        worker.execute{
            try{
                val connection=usb.openDevice(driver.device)?:error("No se pudo abrir el adaptador")
                val serial=driver.ports.firstOrNull()?:run{connection.close();error("Sin puerto serie")}
                try{serial.open(connection);serial.setParameters(115200,8,UsbSerialPort.STOPBITS_1,UsbSerialPort.PARITY_NONE);serial.dtr=false;serial.rts=false}
                catch(e:Exception){runCatching{serial.close()};connection.close();throw e}
                main.post{
                    if(closed || generation!=epoch){runCatching{serial.close()};return@post}
                    port=serial;activeDeviceId=driver.device.deviceId;connected=true;connecting=false;decoder.clear()
                    wantedSerial=runCatching{driver.device.serialNumber}.getOrNull()
                    adapter="VID %04X · PID %04X · %s".format(driver.device.vendorId,driver.device.productId,driver.javaClass.simpleName)
                    log("EVENT","Conectado · $adapter · 115200 8N1");status="Puerto abierto; consultando firmware"
                    io=SerialInputOutputManager(serial,object:SerialInputOutputManager.Listener{
                        override fun onNewData(data:ByteArray){main.post{if(generation==epoch && !closed)decoder.accept(data).forEach{line->receive(line)}}}
                        override fun onRunError(e:Exception){main.post{if(generation==epoch && !closed){closePort();status="Enlace interrumpido: ${e.message}";log("ERROR",status);scheduleReconnect()}}}
                    }).also{it.start()}
                    send("LEGACY_STATUS")
                }
            }catch(e:Exception){main.post{if(generation==epoch && !closed){connecting=false;status="Error USB: ${e.message}";log("ERROR",status);scheduleReconnect()}}}
        }
    }
    private fun receive(line:String){
        log("RX",line)
        val request=pending?:return
        if(!UsbProtocol.matches(line,request.first,request.second,request.third))return
        deadline?.let{main.removeCallbacks(it)};deadline=null;pending=null;busy=false
        UsbProtocol.identity(line)?.let{observation=it;retry=0}
        if(request.first=="CAPABILITIES"){
            val caps=runCatching{JSONObject(line).getJSONArray("capabilities")}.getOrNull()
            if(caps!=null)observation=observation?.copy(capabilities=(0 until minOf(caps.length(),32)).map{caps.optString(it).take(80)})
        }
        status=observation?.let{"Firmware anunciado: ${it.firmware} · ${it.dialect}"}?:"Respuesta recibida; identidad no disponible"
        if((request.first=="HELLO" || request.first=="LEGACY_STATUS") && observation?.dialect in listOf(UsbDialect.SF1,UsbDialect.JSON))send("CAPABILITIES")
    }
    fun send(command:String){
        if(closed || !connected || busy)return
        if(command !in UsbProtocol.commands){status="Solo consultas permitidas";return}
        if(command in setOf("STATUS","CAPABILITIES") && observation==null){status="Identifica primero un firmware compatible";return}
        if(observation?.dialect==UsbDialect.LEGACY && command!="LEGACY_STATUS"){status="Este firmware solo admite la consulta heredada";return}
        val dialect=observation?.dialect?:if(command=="HELLO")UsbDialect.SF1 else UsbDialect.UNKNOWN
        val id=++seq;val frame=UsbProtocol.request(command,id,dialect)
        val active=port?:return;val generation=epoch
        busy=true;pending=Triple(command,id,dialect);log("TX",frame.trimEnd())
        worker.execute{try{active.write(frame.toByteArray(Charsets.UTF_8),2000)}catch(e:Exception){main.post{if(generation==epoch && !closed){closePort();status="Error de envío: ${e.message}";log("ERROR",status);scheduleReconnect()}}}}
        deadline=Runnable{
            if(generation==epoch && pending?.second==id){pending=null;busy=false;status="Sin respuesta a $command · puerto abierto no equivale a ESP32 identificado";log("TIMEOUT",status)
                if(command=="LEGACY_STATUS" && observation==null)send("HELLO")}
        }.also{main.postDelayed(it,5000)}
    }
    private fun closePort(){
        epoch++;deadline?.let{main.removeCallbacks(it)};deadline=null;pending=null;busy=false
        pendingPermission=null;connecting=false;connected=false;observation=null;decoder.clear()
        runCatching{io?.stop()};io=null;val old=port;port=null;activeDeviceId=null
        if(old!=null)worker.execute{runCatching{old.close()}}
    }
    private fun scheduleReconnect(){
        if(closed || wanted==null || connected || connecting || reconnect!=null || retry>=6)return
        val delay=minOf(30000L,1000L shl retry);retry++
        reconnect=Runnable{
            reconnect=null;refresh()
            val matches=drivers.filter{(it.device.vendorId to it.device.productId)==wanted && (wantedSerial==null || runCatching{it.device.serialNumber}.getOrNull()==wantedSerial)}
            if(matches.size==1){status="Reconectando adaptador autorizado · intento $retry";log("EVENT",status);requestOrOpen(matches.single())}
            else{status=if(matches.size>1)"Varios adaptadores iguales: selecciona uno" else "Esperando adaptador autorizado · intento $retry/6";scheduleReconnect()}
        }.also{main.postDelayed(it,delay)}
    }
    fun disconnect(){wanted=null;wantedSerial=null;reconnect?.let{main.removeCallbacks(it)};reconnect=null;closePort();status="Sesión detenida";log("EVENT",status)}
    fun clearLog(){logs=emptyList();dropped=0}
    fun export():ByteArray=(logs.joinToString("\n"){JSONObject().put("format","msrobotika.usb.log.v1").put("at",it.at).put("direction",it.direction).put("data",it.text).toString()}+"\n").toByteArray(Charsets.UTF_8)
    fun release(){if(!closed){disconnect();closed=true;activity.unregisterReceiver(receiver);worker.shutdown()}}
}
