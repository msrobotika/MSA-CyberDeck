package com.msarobotika.cyberdeck
import androidx.test.platform.app.InstrumentationRegistry
import java.net.ServerSocket
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.Assert.*

class AgentExpansionDeviceTest {
 @Test fun offlineCalculationAndLinkDispatchNeedNoNetworkPermission(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val input=AgentInputs(url="https://example.com/",cidr="192.0.2.10/24")
  val link=performAgentTool(context,AgentTool.LINK,null,input);assertEquals("link",link.kind)
  val cidr=performAgentTool(context,AgentTool.CIDR,null,input);assertEquals("cidr",cidr.kind)
  assertTrue(cidr.details.any{it.first=="Red" && it.second=="192.0.2.0"})
 }
 @Test fun assistedResumeReverifiesSourceAndRejectsWrongTypeCorruptionOrDeletion(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val folder=java.io.File(context.cacheDir,"agent-resume-fixture-${java.util.UUID.randomUUID()}").apply{mkdirs()}
  val isolated=object:android.content.ContextWrapper(context){override fun getFilesDir()=folder}
  try{
   val store=EvidenceStore(isolated);val data=DiagnosticResult("nfc","Fixture owned tag","2026-10-19T00:00:00Z","Fixture, not a physical NFC observation",emptyList())
   val entry=store.saveBytes("diagnostico-nfc-fixture.json",data.toEvidenceJson().toByteArray()).last()
   val pending=AgentStepResult(AgentTool.NFC,awaiting=true)
   assertEquals(entry,loadAgentStepResult(store,pending,entry).source)
   assertTrue(runCatching{loadAgentStepResult(store,AgentStepResult(AgentTool.APK,awaiting=true),entry)}.isFailure)
   val file=java.io.File(folder,"evidence/${entry.id}");val bytes=file.readBytes();val bad=bytes.clone();bad[bad.size/2]=(bad[bad.size/2].toInt() xor 1).toByte();file.writeBytes(bad)
   assertTrue(runCatching{loadAgentStepResult(store,pending,entry)}.isFailure)
   file.writeBytes(bytes);store.delete(entry.id);assertTrue(runCatching{loadAgentStepResult(store,pending,entry)}.isFailure)
  }finally{folder.deleteRecursively()}
 }
 @Test fun catalogCoversExistingAppToolsWithoutPretendingHardware(){
  val covered=AgentTool.entries.mapNotNull{it.route}.toSet()+setOf("Móvil","Comparar","CIDR","Red","Web","Enlaces","Agente")
  assertTrue(deckTools.map{it.route}.all{it in covered})
  assertEquals(30,AgentTool.entries.size);assertEquals(14,AgentTool.entries.count{!it.assisted})
  AgentTool.entries.filter{it.assisted}.forEach{tool->var called=false;val run=executeAgentPlan(AgentPlan(listOf(tool)),{called=true;error("Must pause")});assertFalse(called);assertTrue(run.steps.single().awaiting)}
 }
 @Test fun actualLocalReadToolsDoNotModifyEvidence(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext;val before=EvidenceStore(context).list();val work=AuditWorkStore(context).read().activeId
  val tools=listOf(AgentTool.REVIEW,AgentTool.RESOURCES,AgentTool.SELF_APK,AgentTool.INVENTORY,AgentTool.INTEGRITY,AgentTool.CALL_STATUS)
  val results=tools.map{performAgentTool(context,it,work)}
  assertEquals(listOf("phone-review","resources","apk","inventory","work-integrity","call-status"),results.map{it.kind})
  assertTrue(results.all{it.details.isNotEmpty()});assertEquals(before,EvidenceStore(context).list())
 }
 @Test fun activeNetworkToolsWorkOnlyAgainstOwnedLoopbackFixture(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  assertTrue(runCatching{performAgentTool(context,AgentTool.DNS,null,AgentInputs(host="127.0.0.1"))}.isFailure)
  val input=AgentInputs(host="127.0.0.1",networkApproved=true)
  assertEquals("dns",performAgentTool(context,AgentTool.DNS,null,input).kind)
  ServerSocket(0,2,InetAddress.getByName("127.0.0.1")).use{server->
   server.soTimeout=10000;val executor=Executors.newSingleThreadExecutor()
   try{
    val accepted=executor.submit{server.accept().use{}}
    assertEquals("tcp",performAgentTool(context,AgentTool.TCP,null,input.copy(port=server.localPort.toString())).kind);accepted.get(10,TimeUnit.SECONDS)
    val response=executor.submit{server.accept().use{s->s.soTimeout=5000;val reader=s.getInputStream().bufferedReader();assertTrue(reader.readLine().startsWith("HEAD "));while(!reader.readLine().isNullOrEmpty()){};s.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray());s.getOutputStream().flush()}}
    val result=performAgentTool(context,AgentTool.WEB,null,input.copy(url="http://127.0.0.1:${server.localPort}/"));assertEquals("web",result.kind);response.get(10,TimeUnit.SECONDS)
   }finally{executor.shutdownNow()}
  }
 }
}
