package com.msarobotika.cyberdeck

import android.app.role.RoleManager
import android.hardware.*
import android.hardware.usb.UsbManager
import android.net.ConnectivityManager
import androidx.activity.ComponentActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant

/** Uses the same bounded controllers as the tools. Never interprets model output as code. */
class AgentActionRuntime(private val activity:ComponentActivity,private val access:AgentAndroidAccess,private val session:DeckSessionModel,private val bridge:UsbBridge) {
 val waitingForAndroid get()=access.systemPending
 val status=MutableStateFlow("")
 val liveLink=MutableStateFlow("")
 private val store get()=EvidenceStore(activity)
 private fun result(kind:String,target:String,summary:String,vararg fields:Pair<String,String>)=DiagnosticResult(kind,target,Instant.now().toString(),summary,fields.toList())
 private suspend fun <T> io(block:()->T):T=runInterruptible(Dispatchers.IO){block()}
 private suspend fun until(timeout:Long,predicate:()->Boolean){withTimeout(timeout){while(!predicate()){currentCoroutineContext().ensureActive();delay(100)}}}
 suspend fun execute(tool:AgentTool,inputs:AgentInputs,workId:String?,send:(Int,String)->Unit):DiagnosticResult=withContext(Dispatchers.Main.immediate){
  validateAgentInputs(AgentPlan(listOf(tool)),inputs)
  status.value="${tool.label} · ejecutando";liveLink.value=""
  try{when(tool){
   AgentTool.BLE->ble(inputs.bleGatt)
   AgentTool.NFC->nfc()
   AgentTool.LAN->lan()
   AgentTool.MAGNETIC->magnetic()
   AgentTool.APK->{status.value="Elige la APK en el selector de Android";val uri=access.open(arrayOf("application/vnd.android.package-archive","application/octet-stream"));io{ApkInspector.fromUri(activity,uri)}.also{session.apk.value=it}.diagnostic()}
   AgentTool.USB->usb()
   AgentTool.RADIO->radio()
   AgentTool.EXPOSURE->exposure(inputs)
   AgentTool.CALLS->calls()
   AgentTool.FILES->files()
   AgentTool.WORKS->works()
   AgentTool.EXPORT->export(workId)
   AgentTool.TRANSFER->transfer()
   AgentTool.RECEIVE->receive()
   AgentTool.ANALYST->analyst(workId,send)
   AgentTool.MODULES->result("modules","Catálogo implementado","Consulta de capacidades; no ejecuta hardware pendiente","Disponibles en Android" to "BLE, NFC, magnetómetro si existe, USB Host serie con adaptador autorizado, diagnósticos de red y archivos elegidos","Coprocesador" to "ESP32 con firmware compatible conectado al teléfono por OTG; estar conectado al PC no es suficiente","Pendiente" to "RF externo, IR, GPIO, sensores externos, MQTT/Modbus y extensiones no implementadas: no se anuncian como operaciones ejecutables")
   AgentTool.PHONE,AgentTool.CONNECTION,AgentTool.COMPARE,AgentTool.REVIEW,AgentTool.RESOURCES,AgentTool.SELF_APK,AgentTool.INVENTORY,AgentTool.INTEGRITY,AgentTool.CALL_STATUS,AgentTool.DNS,AgentTool.TCP,AgentTool.WEB,AgentTool.LINK,AgentTool.CIDR->io{performAgentTool(activity,tool,workId,inputs)}
  }}finally{status.value="";liveLink.value=""}
 }
 private suspend fun ble(gatt:Boolean):DiagnosticResult {
  if(!BleInspector.hasPermissions(activity)){status.value="Autoriza los permisos Bluetooth de Android";access.permissions(BleInspector.permissions());check(BleInspector.hasPermissions(activity)){"Permiso Bluetooth no concedido"}}
  var snapshot=BleSnapshot();val reader=BleInspector(activity){snapshot=it;status.value=it.status}
  try{reader.start();check(snapshot.running){snapshot.status};until(13000){!snapshot.running};check(snapshot.status.startsWith("Búsqueda BLE finalizada")){snapshot.status}
   if(gatt){val devices=snapshot.devices;check(devices.isNotEmpty()){"No se observaron dispositivos para consultar"};val chosen=devices[access.choose(activity,"Dispositivo GATT propio o autorizado",devices.map{"${it.name} · ${it.address}"})];check(access.confirm(activity,"Consultar servicios GATT", "${chosen.address}. Confirma que es tu equipo o tienes permiso. No se leen ni escriben características.")){"Consulta cancelada"};reader.inspect(chosen.address);until(11000){!snapshot.gattBusy};check(snapshot.status.startsWith("Servicios GATT observados")){snapshot.status}}
   return snapshot.evidence()}
  finally{reader.stop()}
 }
 private suspend fun nfc():DiagnosticResult {
  var snapshot=NfcSnapshot();val reader=NfcInspector(activity){snapshot=it;status.value=it.status}
  try{reader.start();check(snapshot.armed){snapshot.status};until(33000){!snapshot.armed};return snapshot.result?:error("No se recibió ninguna etiqueta durante los 30 segundos; no hay lectura")}
  finally{reader.close()}
 }
 private suspend fun lan():DiagnosticResult {
  var snapshot=LanSnapshot();val reader=LanInventory(activity){snapshot=it;status.value="Mi red · ${it.services.size} anuncios · búsqueda de 15 s"}
  try{reader.start();until(18000){!snapshot.running};check(snapshot.issues.size<LanInventory.TYPES.size || snapshot.services.isNotEmpty()){snapshot.issues.joinToString()};return snapshot.evidence()}
  finally{reader.stop()}
 }
 private suspend fun magnetic():DiagnosticResult {
  val manager=activity.getSystemService(SensorManager::class.java)
  val sensor=manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?:error("Este móvil no tiene magnetómetro")
  val samples=mutableListOf<MagneticSample>();var previous=0L
  val listener=object:SensorEventListener {
   override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){}
   override fun onSensorChanged(e:SensorEvent){if(e.values.size<3 || e.timestamp-previous<500000000L)return;val values=e.values.take(3).map{it.toDouble()};if(values.any{!it.isFinite()})return;previous=e.timestamp;samples+=MagneticSample(Instant.now().toString(),e.timestamp,values[0],values[1],values[2],e.accuracy);status.value="Magnetómetro · ${samples.size}/6 muestras"}
  }
  try{check(manager.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_UI)){"Android no permite iniciar el sensor"};until(8000){samples.size>=6};session.magnetic.value=samples.toList();return MagneticPolicy.report(sensor.name,samples.toList(),null)}
  finally{manager.unregisterListener(listener)}
 }
 private suspend fun usb():DiagnosticResult {
  check(!bridge.busy && !bridge.connecting){"La consola USB ya tiene una consulta en curso"}
  bridge.refresh()
  if(!bridge.connected){
   val drivers=bridge.drivers;check(drivers.isNotEmpty()){"Conecta el ESP32 directamente al móvil por OTG; no hay adaptadores serie"}
   val i=access.choose(activity,"Adaptador USB del agente",drivers.map{"VID %04X · PID %04X · ID %d".format(it.device.vendorId,it.device.productId,it.device.deviceId)})
   bridge.connect(drivers[i]);status.value="Autoriza el USB en Android; esperando identidad real"
   try{until(22000){bridge.connected && !bridge.busy && bridge.observation!=null};check(bridge.observation!=null){bridge.status}}
   catch(e:Exception){bridge.disconnect();throw e}
  }else{
   bridge.send(if(bridge.observation?.dialect==UsbDialect.LEGACY)"LEGACY_STATUS" else "STATUS")
   try{until(7000){!bridge.busy};check(!bridge.status.startsWith("Sin respuesta")){bridge.status}}
   catch(e:Exception){bridge.disconnect();throw e}
  }
  val o=bridge.observation?:error("Puerto abierto sin identidad válida del firmware")
  return result("usb","Adaptador autorizado", "Respuesta auténtica del firmware", "Adaptador" to bridge.adapter,"Firmware anunciado" to o.firmware,"Chip anunciado" to (o.chip?:"No anunciado"),"Protocolo" to o.dialect.name,"Capacidades anunciadas" to o.capabilities.joinToString(),"Método" to "Consulta serie permitida; identidad declarada por el firmware, no prueba criptográfica. No escribe flash.")
 }
 private suspend fun radio():DiagnosticResult {
  check(!bridge.busy && !bridge.connecting){"La consola USB tiene una consulta en curso"};if(bridge.connected)bridge.disconnect();bridge.refresh()
  val manager=activity.getSystemService(UsbManager::class.java);val devices=SentinelUsb.supported(manager)
  check(devices.isNotEmpty()){"No hay ESP32 serie por OTG"}
  val chosen=devices[access.choose(activity,"ESP32 para radar Wi-Fi",devices.map{"VID %04X · PID %04X · ID %d".format(it.vendorId,it.productId,it.deviceId)})]
  // Reuse the permission-aware bridge without issuing a scan through an unapproved device.
  if(!manager.hasPermission(chosen)){
   val driver=bridge.drivers.firstOrNull{it.device.deviceId==chosen.deviceId}?:run{bridge.refresh();bridge.drivers.firstOrNull{it.device.deviceId==chosen.deviceId}}?:error("Adaptador retirado")
   bridge.connect(driver)
   try{until(22000){manager.hasPermission(chosen) && bridge.connected && !bridge.busy}}
   finally{bridge.disconnect()}
  }
  val snap=io{SentinelUsb.snapshot(manager,chosen)}
  return result("radio","ESP32 conectado por OTG","${snap.networks.size} redes en la respuesta del ESP32","Método" to "Snapshot obtenido por el protocolo WiFi Sentinel; RSSI no indica distancia ni dirección física","Firmware" to snap.firmware,"Canal" to snap.channel.toString(),"Redes" to snap.networks.joinToString("\n"){"${it.ssid} · ${it.bssid} · canal ${it.channel} · ${it.rssi} dBm · ${it.security}"}.take(16000))
 }
 private suspend fun exposure(input:AgentInputs):DiagnosticResult {
  val manager=activity.getSystemService(ConnectivityManager::class.java);val network=manager.activeNetwork?:error("Sin red activa")
  val plan=io{ExposureAudit.resolve(input.host,ExposurePolicy.ports(input.port))}.copy(networkKey=network.toString())
  val selected=plan.addresses[access.choose(activity,"IP resuelta que se va a comprobar",plan.addresses.map{it.hostAddress.orEmpty()})]
  val runner=ExposureRunner()
  try{return withContext(Dispatchers.IO){val job=currentCoroutineContext()[Job]!!;runner.run(plan,selected,configureSocket={network.bindSocket(it)},networkUnchanged={job.isActive && manager.activeNetwork==network},progress={status.value="TCP · ${it.size}/${plan.ports.size} puertos"}).also{check(!it.cancelled){"Prueba interrumpida por cambio de red o cancelación"}}.diagnostic()}}
  finally{runner.cancel()}
 }
 private suspend fun calls():DiagnosticResult {
  if(android.os.Build.VERSION.SDK_INT<29)error("El rol de filtrado requiere Android 10 o posterior")
  check(CallJournal.available(activity)){"El rol de filtrado no está disponible en este Android"}
  if(!CallJournal.active(activity)){status.value="Elige si autorizas CiberDeck como filtro de llamadas";access.intent(activity.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING));check(CallJournal.active(activity)){"No se concedió el rol de filtrado"}}
  return io{CallJournal(activity).diagnostic()}
 }
 private suspend fun chooseEvidence(title:String,workId:String?=null,diagnosticsOnly:Boolean=false):Evidence {
  val list=io{val all=store.list();val work=workId?.let{id->AuditWorkStore(activity).read().works.firstOrNull{it.id==id}?:error("Trabajo no disponible")};all.asReversed().filter{entry->(!diagnosticsOnly || (entry.name.startsWith("diagnostico-") && entry.bytes<=65536)) && (work==null || work.evidence.any{AuditWorkPolicy.matches(it,entry)})}.take(500)}
  check(list.isNotEmpty()){"No hay copias en este alcance"}
  return list[access.choose(activity,title,list.map{"${it.name} · ${it.bytes} B"})]
 }
 private suspend fun files():DiagnosticResult {
  val action=access.choose(activity,"Operación de archivos",listOf("Importar archivo elegido","Verificar una copia","Exportar una copia","Eliminar una copia privada"))
  if(action==0){val uri=access.open();val entry=io{store.import(uri).last()};return result("files",entry.id,"Archivo importado a CiberDeck","Nombre" to entry.name,"Bytes" to entry.bytes.toString(),"SHA-256" to entry.sha256)}
  val entry=chooseEvidence("Elige la copia")
  if(action==3){
   check(access.confirm(activity,"Eliminar copia privada", "${entry.name} · ${entry.bytes} bytes. Se borra solo la copia de CiberDeck; el original se conserva. Las referencias de trabajos pueden quedar ausentes.")){"Borrado cancelado"}
   io{check(store.list().any{it==entry}){"La copia cambió; vuelve a seleccionarla"};store.delete(entry.id)}
   return result("files",entry.id,"Copia privada eliminada","Nombre" to entry.name,"SHA-256 de referencia" to entry.sha256)
  }
  io{store.verifiedFile(entry)}
  if(action==2){val uri=access.create(entry.name);io{val file=store.verifiedFile(entry);activity.contentResolver.openOutputStream(uri)?.use{out->file.inputStream().use{it.copyTo(out)}}?:error("No se pudo escribir el destino")}}
  return result("files",entry.id,if(action==2)"Copia verificada y exportada" else "Copia verificada","Nombre" to entry.name,"Bytes" to entry.bytes.toString(),"SHA-256" to entry.sha256)
 }
 private suspend fun works():DiagnosticResult {
  val works=AuditWorkStore(activity)
  val operation=access.choose(activity,"Trabajos",listOf("Consultar trabajos","Crear trabajo","Activar trabajo","Archivar trabajo"))
  return when(operation){
   1->{val name=access.text(activity,"Nombre del nuevo trabajo",160);val scope=access.text(activity,"Alcance autorizado",2000);val next=io{works.create(name,scope,"")};result("works",next.activeId.orEmpty(),"Trabajo creado y activado","Nombre" to name,"Alcance" to scope,"Continuidad" to "El alcance del plan en curso no se cambia; prepara otro plan para usar este trabajo.")}
   2,3->{val action=operation-2;val all=io{works.read().works.filter{!it.archived}};check(all.isNotEmpty()){"No hay trabajos abiertos"};val w=all[access.choose(activity,"Elige el trabajo",all.map{it.name})];if(action==1)check(access.confirm(activity,"Archivar trabajo", "Se archiva ${w.name}; sus archivos se conservan")){"Operación cancelada"};io{if(action==0)works.activate(w.id) else works.archive(w.id,true)};result("works",w.id,if(action==0)"Trabajo activado" else "Trabajo archivado","Continuidad" to "El alcance fijado del plan actual no cambia")}
   else->{val state=io{works.read()};result("works","Trabajos privados","${state.works.size} trabajos registrados","Activo" to state.activeId.orEmpty(),"Listado" to state.works.joinToString("\n"){"${it.id} · ${it.name} · ${if(it.archived)"Archivado" else "Abierto"} · ${it.evidence.size} referencias"})}
  }
 }
 private suspend fun export(workId:String?):DiagnosticResult {
  val text=io{if(workId!=null)AuditWorkStore(activity).report(workId,store) else store.report(store.list())}
  val bytes=text.toByteArray(Charsets.UTF_8);val uri=access.create("CiberDeck-informe.md")
  io{activity.contentResolver.openOutputStream(uri)?.use{it.write(bytes)}?:error("No se pudo abrir el destino")}
  return result("export",workId?:"Todas las copias privadas","Informe escrito en el destino elegido","Bytes" to bytes.size.toString(),"SHA-256 contenido" to PeerProtocol.sha256(bytes),"Método" to "Generación con verificación de copias por el informe existente; contenido ausente o alterado marcado, no certificado como válido")
 }
 private suspend fun transfer():DiagnosticResult {
  val invitation=access.text(activity,"Invitación temporal del receptor CiberDeck",4096);PeerProtocol.invite(invitation)
  val uri=access.open();val selected=io{val name=activity.contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())it.getString(0)else null}?:"archivo.bin";val bytes=activity.contentResolver.openInputStream(uri)?.use{PeerProtocol.readSelectedFile(it)}?:error("Archivo no disponible");name to bytes}
  var snapshot=PeerState();val peer=PeerSession(activity,{snapshot=it;status.value=it.status},{_,_->})
  try{peer.send(invitation,selected.first,selected.second);until(125000){!snapshot.active};check(snapshot.result.isNotBlank()){snapshot.status};return result("transfer","Receptor de la invitación","Entrega confirmada por el receptor","Recibo" to snapshot.result,"Método" to "TLS con certificado fijado en la invitación y confirmación SHA-256; no explora archivos del otro móvil")}
  finally{peer.close()}
 }
 private suspend fun receive():DiagnosticResult {
  var snapshot=BrowserState();val delivered=java.util.concurrent.atomic.AtomicReference<Evidence?>(null);var offered:BrowserOffer?=null
  val reader=BrowserReceiver(activity,{snapshot=it;status.value=it.status;liveLink.value=it.url},{name,bytes->delivered.set(store.saveBytes(name,bytes).last())})
  try{reader.start();withTimeout(125000){while(delivered.get()==null){
   currentCoroutineContext().ensureActive();val offer=snapshot.offer
   if(offer!=null && offer!==offered){offered=offer;val yes=access.confirm(activity,"Recibir archivo", "${offer.name} · ${offer.size} bytes. HTTP local sin cifrar: solo en una red de confianza.");reader.decide(yes)}
   if(!snapshot.active)check(delivered.get()!=null){snapshot.status};delay(100)
  }};val entry=delivered.get()?:error("No se recibió un archivo");return result("receive",entry.id,"Archivo recibido y guardado","Nombre" to entry.name,"Bytes" to entry.bytes.toString(),"SHA-256" to entry.sha256,"Método" to "HTTP local temporal con aceptación del archivo; no se conserva el enlace secreto en el informe")}
  finally{reader.close()}
 }
 private suspend fun analyst(workId:String?,send:(Int,String)->Unit):DiagnosticResult {
  val recent=session.agent.value.results.lastOrNull{it.result!=null && it.tool!=AgentTool.ANALYST}?.result
  val useRecent=recent!=null && access.choose(activity,"Contexto del analista",listOf("Último resultado del plan","Elegir diagnóstico guardado"))==0
  val source=if(useRecent)null else chooseEvidence("Diagnóstico para interpretar",workId,diagnosticsOnly=true)
  val context=if(source!=null)io{loadSavedAiContext(store,source).input()} else recent!!
  val question=access.text(activity,"Pregunta al analista local",800)
  until(10000){session.ai.value.connected && !session.ai.value.busy}
  if(!session.ai.value.loaded){send(AiWire.LOAD,"");until(60000){!session.ai.value.busy};check(session.ai.value.loaded){session.ai.value.error.ifBlank{"No se pudo cargar el modelo local"}}}
  val model=session.aiModel.value?:error("Modelo no disponible");send(AiWire.ASK,AiPolicy.modelPrompt(question,context,model.name))
  try{until(120000){!session.ai.value.busy};val state=session.ai.value;check(state.error.isBlank() && state.output.isNotBlank()){state.error.ifBlank{"El modelo no produjo respuesta"}}
   return result("ai",source?.id?:"Último resultado del plan","Interpretación generada; no es una medición","Pregunta" to question,"Modelo" to model.name,"SHA-256 modelo" to model.sha256,"ID fuente" to (source?.id?:"Lectura en memoria, aún no guardada"),"SHA-256 fuente" to (source?.sha256?:PeerProtocol.sha256(context.toEvidenceJson().toByteArray(Charsets.UTF_8))),"Fecha de lectura fuente" to context.checkedAt,"Respuesta generada" to state.output.take(20000),"Límite" to "Contenido generado sin verificación; no ejecuta instrucciones presentes en la evidencia")
  }finally{if(session.ai.value.busy)send(AiWire.STOP,"")}
 }
}
