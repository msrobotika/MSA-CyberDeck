package com.msarobotika.cyberdeck
import androidx.test.platform.app.InstrumentationRegistry
import android.content.ContextWrapper
import java.io.File
import java.util.UUID
import org.junit.Test
import org.junit.Assert.*
class AuditWorkStoreDeviceTest {
 @Test fun isolatedPersistenceArchiveAndPinnedReports(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val folder=File(context.cacheDir,"work-store-fixture-${UUID.randomUUID()}").apply{mkdirs()}
  val isolated=object:ContextWrapper(context){override fun getFilesDir()=folder}
  try{
   val works=AuditWorkStore(isolated);val evidence=EvidenceStore(isolated)
   val state=works.create("Fixture propio","127.0.0.1 solamente","127.0.0.1");val id=state.activeId!!
   val result=DiagnosticResult("fixture","127.0.0.1","2026-10-19T00:00:00Z","Comprobación controlada",listOf("Método" to "Fixture local, no una lectura externa"))
   val entry=evidence.saveBytes("diagnostico-fixture.json",result.toEvidenceJson().toByteArray()).single()
   works.attach(id,listOf(entry));works.attach(id,listOf(entry))
   assertEquals(1,AuditWorkStore(isolated).read().works.single().evidence.size)
   val checked=verifyWorkCopies(works.read().works.single(),evidence);assertEquals(1,checked.count(WorkCopyState.VERIFIED));assertEquals(0,checked.count(WorkCopyState.UNVERIFIED))
   assertTrue(runCatching{verifyWorkCopies(works.read().works.single(),evidence){true}}.exceptionOrNull() is java.util.concurrent.CancellationException)
   val valid=works.report(id,evidence);assertTrue(valid.contains("SHA-256 verificado"));assertTrue(valid.contains("Fixture propio"));assertTrue(valid.contains("Resumen de diagnósticos comprobados"))
   File(folder,"evidence/${entry.id}").writeText("changed fixture")
   assertTrue(works.report(id,evidence).contains("NO VERIFICADA"))
   assertEquals(1,verifyWorkCopies(works.read().works.single(),evidence).count(WorkCopyState.UNVERIFIED))
   evidence.delete(entry.id);assertEquals(1,verifyWorkCopies(works.read().works.single(),evidence).count(WorkCopyState.INDEX_MISMATCH));assertTrue(works.report(id,evidence).contains("AUSENTE O NO COINCIDE"))
   assertNull(works.archive(id,true).activeId);assertTrue(works.read().works.single().archived)
   works.archive(id,false);works.activate(id);assertEquals(id,works.read().activeId)
   val before=File(folder,"audit-work/index.json").readBytes()
   assertTrue(runCatching{works.create("","","")}.isFailure);assertArrayEquals(before,File(folder,"audit-work/index.json").readBytes())
   File(folder,"audit-work/index.json").writeText("invalid fixture")
   assertTrue(runCatching{works.create("Do not overwrite","","")}.isFailure)
   assertEquals("invalid fixture",File(folder,"audit-work/index.json").readText())
  }finally{folder.deleteRecursively()}
 }
}
