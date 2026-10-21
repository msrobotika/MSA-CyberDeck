package com.msarobotika.cyberdeck

import androidx.test.platform.app.InstrumentationRegistry
import android.content.ComponentName
import android.net.Uri
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.net.*
import kotlin.concurrent.thread
import org.junit.Test
import org.junit.Assert.*

/** Real core checks that do not open an Activity or bypass a locked phone. */
class NativeCoreDeviceTest {
 private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
 @Test fun googleNativeLibraryLoadsInPrivateWorkerWithoutModel()=runBlocking {
  @Suppress("DEPRECATION") val info=context.packageManager.getServiceInfo(ComponentName(context,LocalAiService::class.java),0)
  assertFalse(info.exported);assertTrue(info.processName.endsWith(":local_ai"))
  val flow=MutableStateFlow(LocalAiState());val client=LocalAiClient(context,flow)
  try{
   client.connect()
   val state=withTimeout(20000){flow.first{it.runtime || it.error.isNotBlank()}}
   assertTrue("Native library failed: ${state.error}",state.runtime);assertFalse(state.loaded);assertEquals("",state.output)
   if(AiModelStore(context).current()==null){client.send(AiWire.LOAD);val missing=withTimeout(10000){flow.first{it.error.contains("Importa antes")}};assertFalse(missing.loaded);assertFalse(missing.busy);assertEquals("",missing.output)}
  }finally{client.disconnect()}
 }
 @Test fun realTcpHandshakeToOwnedFixtureSendsNoApplicationBytes(){
  ServerSocket(0,1,InetAddress.getByName("127.0.0.1")).use{server->
   server.soTimeout=10000;var bytes:Int?=null
   val fixture=thread{server.accept().use{it.soTimeout=5000;bytes=it.getInputStream().read()}}
   val plan=ExposureAudit.resolve("127.0.0.1",listOf(server.localPort))
   val observed=mutableListOf<List<PortObservation>>()
   val result=ExposureRunner().run(plan,plan.addresses.first()){observed+=it}
   fixture.join(6000)
   assertFalse(fixture.isAlive);assertEquals(-1,bytes);assertEquals(PortState.OPEN,result.probes.single().state)
   assertEquals("127.0.0.1",result.address);assertFalse(result.cancelled);assertEquals(1,observed.size)
   val cancelled=ExposureRunner().also{it.cancel()}.run(plan,plan.addresses.first()){}
   assertTrue(cancelled.cancelled);assertTrue(cancelled.probes.isEmpty())
  }
 }
 @Test fun invalidModelImportPreservesIndexAndOriginal(){
  val store=AiModelStore(context);val before=store.current()
  val source=File.createTempFile("owned-invalid-model-",".litertlm",context.cacheDir)
  try{source.writeBytes(ByteArray(16384));assertTrue(runCatching{store.import(Uri.fromFile(source)){}}.isFailure);assertTrue(source.exists());assertEquals(16384L,source.length());assertEquals(before,store.current())}finally{source.delete()}
 }
 @Test fun modelStorageTransactionUsesCopyAndPreservesOriginal(){
  val folder=File(context.cacheDir,"ai-storage-fixture-${java.util.UUID.randomUUID()}").apply{mkdirs()}
  val isolated=object:android.content.ContextWrapper(context){override fun getFilesDir()=File(folder,"files").apply{mkdirs()}}
  val source=File(folder,"storage-fixture.litertlm")
  try{
   // Header-only storage fixture, never passed to the native inference engine.
   source.writeBytes(ByteArray(16384).also{"LITERTLM".toByteArray().copyInto(it)})
   val store=AiModelStore(isolated);val imported=store.import(Uri.fromFile(source)){}
   assertEquals(16384L,imported.bytes);assertNotEquals(source.absolutePath,store.path(imported).absolutePath)
   assertEquals(imported,store.current());assertArrayEquals(source.readBytes(),store.path(imported).readBytes())
   source.writeBytes(ByteArray(16384))
   assertTrue(runCatching{store.import(Uri.fromFile(source)){}}.isFailure);assertEquals(imported,store.current())
   store.delete();assertNull(store.current());assertFalse(store.path(imported).exists());assertTrue(source.exists())
  }finally{folder.deleteRecursively()}
 }
 @Test fun installedApkSignatureAndOwnServiceManifestAreReadable(){
  val result=ApkInspector.inspectFile(context,File(context.applicationInfo.sourceDir))
  assertEquals(context.packageName,result.packageName);assertTrue(result.signers.isNotEmpty());assertEquals(64,result.fingerprint.sha256.length)
  assertTrue(result.exported.none{it.first.endsWith("LocalAiService")})
  assertTrue(result.exported.any{it.first.endsWith("CallGuardService") && it.second.contains("BIND_SCREENING_SERVICE")})
 }
}
