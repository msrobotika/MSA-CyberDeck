package com.msarobotika.cyberdeck

import androidx.test.platform.app.InstrumentationRegistry
import android.content.ContextWrapper
import java.io.File
import java.util.UUID
import org.junit.Test
import org.junit.Assert.*
class SavedAiContextDeviceTest {
 @Test fun recheckingRejectsCorruptionDeletionAndAiInterpretations(){
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val folder=File(context.cacheDir,"ai-context-fixture-${UUID.randomUUID()}").apply{mkdirs()}
  val isolated=object:ContextWrapper(context){override fun getFilesDir()=folder}
  try{
   val store=EvidenceStore(isolated)
   val result=DiagnosticResult("fixture","owned fixture","2026-10-19T00:00:00Z","Fixture, not physical observations",listOf("field" to "value"))
   val source=store.saveBytes("diagnostico-fixture.json",result.toEvidenceJson().toByteArray()).last()
   val selected=loadSavedAiContext(store,source);assertEquals(result,selected.result)
   assertEquals(source.sha256,selected.input().details.first().second)
   val prompt=AiPolicy.modelPrompt("Explica los límites",selected.input(),"qwen3")
   assertTrue(prompt.contains(source.sha256));assertTrue(prompt.contains("field: value"));assertTrue(prompt.endsWith("/no_think"))
   val file=File(folder,"evidence/${source.id}");val original=file.readBytes();val changed=original.clone();changed[changed.size/2]=(changed[changed.size/2].toInt() xor 1).toByte();file.writeBytes(changed)
   assertTrue(runCatching{loadSavedAiContext(store,source)}.isFailure)
   file.writeBytes(original);store.delete(source.id);assertTrue(runCatching{loadSavedAiContext(store,source)}.isFailure)
   val generated=store.saveBytes("diagnostico-ai-fixture.json",result.copy(kind="ai").toEvidenceJson().toByteArray()).last()
   assertTrue(runCatching{loadSavedAiContext(store,generated)}.isFailure)
  }finally{folder.deleteRecursively()}
 }
}
