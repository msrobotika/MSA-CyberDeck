package com.msarobotika.cyberdeck

import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.content.FileProvider
import android.provider.OpenableColumns
import org.junit.Assert.*
import org.junit.Test

class EvidenceFileDeviceTest {
    @Test fun sharedUriPreservesDisplayNameAndExactFileBytes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = EvidenceStore(context)
        val bytes = "Generated file-open fixture".toByteArray()
        val entry = store.saveBytes("file-open-validation.txt",bytes).last()
        try {
            val file = store.verifiedFile(entry)
            val uri = FileProvider.getUriForFile(context,"${context.packageName}.evidence",file,entry.name)
            assertEquals("content",uri.scheme)
            context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)!!.use {
                assertTrue(it.moveToFirst()); assertEquals(entry.name,it.getString(0))
            }
            context.contentResolver.openInputStream(uri)!!.use { assertArrayEquals(bytes,it.readBytes()) }
        } finally { store.delete(entry.id) }
    }
    @Test fun changedFileIsRejectedBeforeItCanBeShared() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = EvidenceStore(context)
        val entry = store.saveBytes("file-open-integrity-validation.txt","original".toByteArray()).last()
        try {
            java.io.File(context.filesDir,"evidence/${entry.id}").writeText("altered!")
            val error = runCatching { store.verifiedFile(entry) }.exceptionOrNull()
            assertTrue(error is IllegalArgumentException)
            assertTrue(error!!.message.orEmpty().contains("SHA-256"))
        } finally { store.delete(entry.id) }
    }
}
