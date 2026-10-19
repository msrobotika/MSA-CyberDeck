package com.msarobotika.cyberdeck
import androidx.test.platform.app.InstrumentationRegistry
import android.content.ContextWrapper
import java.io.File
import java.util.UUID
import org.junit.Test
import org.junit.Assert.*
class ComparisonStoreDeviceTest {
 @Test fun actualStoredPhoneReadingsComparedWithoutScreenAccess(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val store=EvidenceStore(context)
  val phone=store.list().filter{it.name.startsWith("diagnostico-phone-")}.filter{runCatching{store.diagnostic(it).kind=="phone"}.getOrDefault(false)}
  org.junit.Assume.assumeTrue(phone.size>=2)
  val a=phone.first();val b=phone.last();val result=loadSavedComparison(store,a,b)
  assertEquals(result.rows.size,FieldChange.entries.sumOf{result.count(it)})
  assertEquals(store.diagnostic(a).details.size,result.baseline.details.size)
  assertTrue(store.verifiedFile(a).isFile);assertTrue(store.verifiedFile(b).isFile)
  val report=result.diagnostic();assertEquals(result.verifiedAt,report.checkedAt)
  val folder=File(context.filesDir,"validation").apply{mkdirs()}
  File(folder,"a15-comparison-private.json").writeText(report.toEvidenceJson())
  println("Actual stored phone comparison: ${result.rows.size} labels; ${result.count(FieldChange.CHANGED)} modified. No network traffic.")
 }

 @Test fun comparisonsRefuseChangedOrDeletedSources(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val folder=File(context.cacheDir,"comparison-fixture-${UUID.randomUUID()}").apply{mkdirs()}
  val isolated=object:ContextWrapper(context){override fun getFilesDir()=folder}
  try{
   val store=EvidenceStore(isolated)
   val a=DiagnosticResult("fixture","owned fixture","2026-10-19T00:00:00Z","Fixture, not actual data",listOf("field" to "before"))
   val first=store.saveBytes("diagnostico-fixture-a.json",a.toEvidenceJson().toByteArray()).last()
   val second=store.saveBytes("diagnostico-fixture-b.json",a.copy(details=listOf("field" to "after")).toEvidenceJson().toByteArray()).last()
   assertEquals(1,loadSavedComparison(store,first,second).count(FieldChange.CHANGED))
   assertTrue(runCatching{loadSavedComparison(store,first,first)}.isFailure)
   val file=File(folder,"evidence/${second.id}");val bytes=file.readBytes();val changed=bytes.clone();changed[changed.size/2]=(changed[changed.size/2].toInt() xor 1).toByte();file.writeBytes(changed)
   assertTrue(runCatching{loadSavedComparison(store,first,second)}.isFailure)
   file.writeBytes(bytes);store.delete(first.id);assertTrue(runCatching{loadSavedComparison(store,first,second)}.isFailure)
  }finally{folder.deleteRecursively()}
 }
}
