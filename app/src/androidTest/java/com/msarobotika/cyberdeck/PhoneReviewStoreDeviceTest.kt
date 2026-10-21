package com.msarobotika.cyberdeck
import androidx.test.platform.app.InstrumentationRegistry
import android.content.ContextWrapper
import java.io.File
import java.util.UUID
import org.junit.Test
import org.junit.Assert.*
class PhoneReviewStoreDeviceTest {
 @Test fun isolatedManualNotesPersistAndCorruptionIsNotOverwritten(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val folder=File(context.cacheDir,"phone-review-fixture-${UUID.randomUUID()}").apply{mkdirs()}
  val isolated=object:ContextWrapper(context){override fun getFilesDir()=folder}
  try{
   val store=PhoneReviewStore(isolated);val c=PhoneCheck("Fixture","value","Fixture, no lectura física",PhoneTone.INFO,"Fixture");val item=PhoneReviewPolicy.items("Fixture",listOf(c)).single()
   assertTrue(store.read().isEmpty());store.save(item,true,"Comprobación de prueba")
   val reloaded=PhoneReviewStore(isolated).read();assertEquals(1,reloaded.size);assertEquals(ReviewMark.CURRENT,PhoneReviewPolicy.status(item,reloaded.single()))
   val changed=PhoneReviewPolicy.items("Fixture",listOf(c.copy(value="new value"))).single();assertEquals(ReviewMark.STALE,PhoneReviewPolicy.status(changed,reloaded.single()))
   store.save(changed,false,"Nueva nota sin marcar");assertEquals(ReviewMark.PENDING,PhoneReviewPolicy.status(changed,store.read().single()))
   val file=File(folder,"phone-review/index.json");val before=file.readBytes();assertTrue(runCatching{store.save(item,true,"x".repeat(601))}.isFailure);assertArrayEquals(before,file.readBytes())
   file.writeText("invalid fixture");assertTrue(runCatching{store.save(item,true,"")}.isFailure);assertEquals("invalid fixture",file.readText())
  }finally{folder.deleteRecursively()}
 }
 @Test fun actualAndroidObservationsProducePlanWithoutAutomaticMarks(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val snapshot=PhoneAudit.read(context);val items=PhoneReviewPolicy.items(snapshot.device,snapshot.checks)
  assertTrue(items.isNotEmpty());assertEquals(snapshot.checks.size,items.size)
  assertTrue(items.all{PhoneReviewPolicy.status(it,null)==ReviewMark.PENDING})
  val diagnostic=phoneReviewDiagnostic(snapshot,emptyList());assertEquals("phone-review",diagnostic.kind)
  assertTrue(diagnostic.details.any{it.first=="Fecha de la lectura Android" && it.second==snapshot.at})
  val out=File(context.filesDir,"validation/phone-review-private.json").apply{parentFile?.mkdirs()};out.writeText(diagnostic.toEvidenceJson())
 }
}
