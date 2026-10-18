package com.msarobotika.cyberdeck

import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import android.hardware.SensorManager
import android.hardware.usb.UsbManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.nfc.NfcAdapter
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.provider.Settings
import java.time.Instant
import java.time.LocalDate

data class PhoneSnapshot(val device:String,val at:String,val checks:List<PhoneCheck>,val ramAvailable:Long?,val ramTotal:Long?,
    val diskAvailable:Long?,val diskTotal:Long?,val battery:Int?,val batteryTemperature:Double?,val hardware:List<Pair<String,String>>) {
    fun diagnostic()=DiagnosticResult("phone",device,at,"${checks.count{it.tone==PhoneTone.REVIEW}} ajustes para revisar · observación local",
        checks.map{it.title to "${it.value}. ${it.explanation}"}+listOf(
            "RAM disponible / total" to "${ramAvailable?:"N/D"} / ${ramTotal?:"N/D"} bytes",
            "Volumen de la app disponible / total" to "${diskAvailable?:"N/D"} / ${diskTotal?:"N/D"} bytes",
            "Batería" to "${battery?:"N/D"}% · temperatura de batería ${batteryTemperature?:"N/D"} °C")+hardware+
            ("Alcance" to "Instantánea de APIs locales de Android. No escanea archivos, apps o redes; no detecta malware ni certifica que el móvil sea seguro."))
}
/** Public Android APIs only. No shell, root, admin enrollment or extra runtime permissions. */
object PhoneAudit {
    fun read(context:Context):PhoneSnapshot {
        val checks=mutableListOf<PhoneCheck>()
        checks+=PhoneAssessment.lock(runCatching{context.getSystemService(KeyguardManager::class.java).isDeviceSecure}.getOrNull())
        checks+=PhoneAssessment.patch(Build.VERSION.SECURITY_PATCH,LocalDate.now())
        checks+=PhoneAssessment.debugging(runCatching{Settings.Global.getInt(context.contentResolver,Settings.Global.ADB_ENABLED,-1)}.getOrDefault(-1))
        val encryption=runCatching{context.getSystemService(DevicePolicyManager::class.java).storageEncryptionStatus}.getOrNull()
        checks+=PhoneCheck("Cifrado del almacenamiento",when(encryption){
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER->"Activo por usuario"
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE->"Activo"
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY->"Activo con clave por defecto"
            DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE->"Inactivo"
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING->"En activación"
            else->"No verificable en este perfil"},"Estado informado por Android; no se extraen claves. Un dato no disponible no se interpreta como ausencia de cifrado.",
            when(encryption){DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER,DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE->PhoneTone.OK
                DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE,DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY->PhoneTone.REVIEW
                else->PhoneTone.UNKNOWN},"Seguridad","android.settings.SECURITY_SETTINGS")
        val cm=context.getSystemService(ConnectivityManager::class.java)
        val net=cm.activeNetwork
        val caps=net?.let{runCatching{cm.getNetworkCapabilities(it)}.getOrNull()}
        val link=net?.let{runCatching{cm.getLinkProperties(it)}.getOrNull()}
        val connection=ConnectionDiagnostic.read(context)
        checks+=PhoneCheck("Acceso a Internet",connection.summary,"Validación de Android, sin peticiones a terceros. No prueba que la red sea segura.",PhoneTone.INFO,"Conexión","android.settings.WIFI_SETTINGS")
        checks+=PhoneCheck("VPN en la red activa",when{caps==null->"No disponible";caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)->"Presente";else->"No anunciada por Android"},
            "Describe la red usada por la app; no certifica el túnel, proveedor ni cobertura de otras aplicaciones.",if(caps==null)PhoneTone.UNKNOWN else PhoneTone.INFO,"Conexión","android.settings.VPN_SETTINGS")
        val privateDns=if(Build.VERSION.SDK_INT>=28)link?.isPrivateDnsActive else null
        checks+=PhoneCheck("DNS privado del enlace",when(privateDns){true->"Activo";false->"No activo";null->"No disponible"},
            "Estado del resolutor del enlace. Las aplicaciones pueden usar otros mecanismos DNS.",if(privateDns==null)PhoneTone.UNKNOWN else PhoneTone.INFO,"Conexión","android.settings.WIRELESS_SETTINGS")
        checks+=PhoneCheck("Proxy del enlace",if(link==null)"No disponible" else if(link.httpProxy!=null)"Configurado" else "Sin proxy anunciado",
            "No se leen credenciales ni se cambia la configuración.",if(link==null)PhoneTone.UNKNOWN else PhoneTone.INFO,"Conexión")
        connection.details.filter{it.first in listOf("Transporte","DNS configurados","Interfaz")}.forEach{checks+=PhoneCheck(it.first,it.second,"Lectura del sistema para la red activa.",PhoneTone.INFO,"Conexión")}
        val ram=runCatching{ActivityManager.MemoryInfo().also{context.getSystemService(ActivityManager::class.java).getMemoryInfo(it)}}.getOrNull()
        val disk=runCatching{StatFs(context.filesDir.absolutePath)}.getOrNull()
        val battery=runCatching{context.registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))}.getOrNull()
        val level=battery?.getIntExtra(BatteryManager.EXTRA_LEVEL,-1)?:-1
        val scale=battery?.getIntExtra(BatteryManager.EXTRA_SCALE,-1)?:-1
        val temperature=battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,Int.MIN_VALUE)
        val pm=context.packageManager
        val sensors=runCatching{context.getSystemService(SensorManager::class.java).getSensorList(android.hardware.Sensor.TYPE_ALL).map{it.name}.distinct()}.getOrNull()
        val nfc=runCatching{NfcAdapter.getDefaultAdapter(context)}.getOrNull()
        val usbCount=runCatching{context.getSystemService(UsbManager::class.java).deviceList.size}.getOrNull()
        val hardware=listOf("Android" to "${Build.VERSION.RELEASE} · API ${Build.VERSION.SDK_INT}",
            "Bluetooth LE" to if(pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))"Disponible; inicia una búsqueda desde Bluetooth" else "No anunciado",
            "NFC" to if(nfc==null)"No detectado" else if(nfc.isEnabled)"Disponible y activo" else "Disponible, apagado",
            "USB Host" to if(pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST))"Anunciado por Android; ${usbCount?:"N/D"} periféricos actuales" else "No anunciado",
            "Emisor infrarrojo integrado" to if(runCatching{context.getSystemService(ConsumerIrManager::class.java)?.hasIrEmitter()==true}.getOrDefault(false))"Disponible" else "No detectado; requiere hardware externo",
            "Sensores anunciados" to (sensors?.let{"${it.size} tipos/nombres distintos. Primeros 16: ${it.take(16).joinToString()}. Inventario; no se están leyendo muestras."}?:"No disponible"))
        return PhoneSnapshot("${Build.MANUFACTURER} ${Build.MODEL}",Instant.now().toString(),checks,ram?.availMem,ram?.totalMem,disk?.availableBytes,disk?.totalBytes,
            if(level>=0 && scale>0)((level*100.0)/scale).toInt().coerceIn(0,100) else null,
            temperature?.takeIf{it!=Int.MIN_VALUE}?.div(10.0),hardware)
    }
}
