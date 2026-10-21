package com.msarobotika.cyberdeck

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

data class FileFingerprint(val bytes:Long,val sha256:String)
object FileAudit {
    const val LIMIT=128L*1024*1024
    fun stream(input:InputStream,output:OutputStream?=null,limit:Long=LIMIT):FileFingerprint {
        require(limit>=0)
        val digest=MessageDigest.getInstance("SHA-256");val buf=ByteArray(65536);var size=0L
        while(true){val n=input.read(buf);if(n<0)break;if(n==0)continue
            require(n<=limit-size){"El APK supera el límite de 128 MiB"}
            size+=n;digest.update(buf,0,n);output?.write(buf,0,n)
        }
        return FileFingerprint(size,digest.digest().joinToString(""){"%02x".format(it)})
    }
}
object ApkPermissionHints {
    private val special=mapOf("SYSTEM_ALERT_WINDOW" to "Superposición sobre otras apps","REQUEST_INSTALL_PACKAGES" to "Solicitud de instalación de APK","MANAGE_EXTERNAL_STORAGE" to "Acceso amplio a archivos","QUERY_ALL_PACKAGES" to "Visibilidad amplia de aplicaciones","BIND_ACCESSIBILITY_SERVICE" to "Servicio de accesibilidad declarado")
    fun special(permission:String)=special[permission.removePrefix("android.permission.")]
}
