package com.msarobotika.cyberdeck
import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
class FileAuditTest {
 @Test fun realBytesHash(){val r=FileAudit.stream(ByteArrayInputStream("abc".toByteArray()));assertEquals(3L,r.bytes);assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",r.sha256)}
 @Test fun boundBeforeWriting(){val out=ByteArrayOutputStream();assertTrue(runCatching{FileAudit.stream(ByteArrayInputStream(ByteArray(20)),out,10)}.isFailure);assertTrue(out.size()<=10)}
 @Test fun copyMatchesFingerprint(){val out=ByteArrayOutputStream();val bytes=ByteArray(100000){(it%255).toByte()};val r=FileAudit.stream(ByteArrayInputStream(bytes),out);assertArrayEquals(bytes,out.toByteArray());assertEquals(100000L,r.bytes)}
 @Test fun sensitiveSpecialPermission(){assertNotNull(ApkPermissionHints.special("android.permission.SYSTEM_ALERT_WINDOW"));assertNull(ApkPermissionHints.special("com.example.custom"))}
}
