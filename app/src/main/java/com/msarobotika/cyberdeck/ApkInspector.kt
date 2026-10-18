package com.msarobotika.cyberdeck

import android.content.Context
import android.content.pm.*
import android.net.Uri
import android.os.Build
import java.io.File
import java.security.MessageDigest
import java.time.Instant

data class ApkPermission(val name:String,val kind:String)
data class ApkSnapshot(val at:String,val packageName:String,val label:String,val version:String,val minSdk:Int,val targetSdk:Int,
 val fingerprint:FileFingerprint,val signers:List<String>,val debug:Boolean,val testOnly:Boolean,val cleartext:Boolean,
 val permissions:List<ApkPermission>,val exported:List<Pair<String,String>>,val exportedCount:Int) {
 fun diagnostic()=DiagnosticResult("apk",packageName,at,"Inspección estática de APK · $label",listOf(
 "Nombre declarado" to label,"Versión" to version,"API mínima / objetivo" to "$minSdk / $targetSdk",
 "Bytes" to fingerprint.bytes.toString(),"SHA-256 APK" to fingerprint.sha256,
 "Certificados firmantes actuales SHA-256" to signers.joinToString("\n").ifEmpty{"No disponibles"},
 "Depurable" to debug.toString(),"Solo pruebas" to testOnly.toString(),"Bandera HTTP claro" to cleartext.toString(),
 "Permisos declarados" to permissions.joinToString("\n"){"${it.name}: ${it.kind}"},
 "Componentes exportados" to "$exportedCount declarados. Primeros 40: "+exported.joinToString("\n"){"${it.first}: ${it.second}"},
 "Alcance" to "Sin instalar, ejecutar ni subir el APK. Firma identificada por Android, sin reputación del editor ni análisis de malware. Declarar un permiso no significa tenerlo concedido. Componentes exportados pueden tener controles propios. Configuración de seguridad de red XML no analizada."))
}
object ApkInspector {
 fun fromUri(context:Context,uri:Uri):ApkSnapshot {
  val file=File.createTempFile("ciberdeck-apk-",".apk",context.cacheDir)
  try {
   context.contentResolver.openInputStream(uri)?.use{input->file.outputStream().use{out->FileAudit.stream(input,out)}}?:error("No se pudo abrir el archivo elegido")
   return inspectFile(context,file)
  }finally{file.delete()}
 }
 @Suppress("DEPRECATION") fun inspectFile(context:Context,file:File):ApkSnapshot {
  require(file.isFile && file.length() in 1..FileAudit.LIMIT){"Elige un APK único de hasta 128 MiB; no se admiten XAPK/APKS"}
  val fingerprint=file.inputStream().use{FileAudit.stream(it)}
  val pm=context.packageManager
  val flags=PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS or
   (if(Build.VERSION.SDK_INT>=28)PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
  val info=pm.getPackageArchiveInfo(file.absolutePath,flags)?:error("Android no reconoce este archivo como APK compatible")
  val app=info.applicationInfo?:error("No se pudo leer el manifiesto")
  app.sourceDir=file.absolutePath;app.publicSourceDir=file.absolutePath
  val label=runCatching{app.loadLabel(pm).toString().take(120)}.getOrDefault(info.packageName)
  val signatures=if(Build.VERSION.SDK_INT>=28)info.signingInfo?.apkContentsSigners else info.signatures
  val signers=signatures.orEmpty().map{sig->MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()).joinToString(""){"%02x".format(it)}}.distinct()
  val perms=info.requestedPermissions.orEmpty().distinct().take(500).sorted().map{name->
   val special=ApkPermissionHints.special(name)
   val pi=runCatching{pm.getPermissionInfo(name,0)}.getOrNull()
   ApkPermission(name.take(240),special?:when(pi?.protectionLevel?.and(PermissionInfo.PROTECTION_MASK_BASE)){
    PermissionInfo.PROTECTION_DANGEROUS->"Sensible: permiso de ejecución"
    PermissionInfo.PROTECTION_SIGNATURE->"Protegido por firma"
    PermissionInfo.PROTECTION_NORMAL->"Permiso normal"
    else->"Personalizado o no reconocido por este Android"})
  }
  val components=mutableListOf<Pair<String,String>>()
  info.activities.orEmpty().filter{it.exported}.forEach{components+=it.name to "Actividad · permiso ${it.permission?:"no declarado"}"}
  info.services.orEmpty().filter{it.exported}.forEach{components+=it.name to "Servicio · permiso ${it.permission?:"no declarado"}"}
  info.receivers.orEmpty().filter{it.exported}.forEach{components+=it.name to "Receptor · permiso ${it.permission?:"no declarado"}"}
  info.providers.orEmpty().filter{it.exported}.forEach{components+=it.name to "Proveedor · lectura ${it.readPermission?:"no declarado"}; escritura ${it.writePermission?:"no declarado"}"}
  val version=info.versionName.orEmpty().take(80)+" ("+(if(Build.VERSION.SDK_INT>=28)info.longVersionCode else info.versionCode.toLong())+")"
  return ApkSnapshot(Instant.now().toString(),info.packageName,label,version,app.minSdkVersion,app.targetSdkVersion,fingerprint,signers,
   app.flags and ApplicationInfo.FLAG_DEBUGGABLE!=0,app.flags and ApplicationInfo.FLAG_TEST_ONLY!=0,app.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC!=0,
   perms,components.take(40),components.size)
 }
}
