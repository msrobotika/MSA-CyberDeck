package com.msarobotika.cyberdeck

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class EvidenceHashTest {
    @Test fun knownVectorAndExactCopy() {
        val output = ByteArrayOutputStream()
        val result = EvidenceHash.copyAndHash(ByteArrayInputStream("abc".toByteArray()), output)
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", result.first)
        assertEquals(3L, result.second)
        assertArrayEquals("abc".toByteArray(), output.toByteArray())
    }
    @Test fun largeFileCrossesBufferBoundary() {
        val bytes = ByteArray(200000) { (it % 251).toByte() }
        val output = ByteArrayOutputStream()
        val result = EvidenceHash.copyAndHash(ByteArrayInputStream(bytes), output)
        assertEquals(bytes.size.toLong(), result.second)
        assertArrayEquals(bytes, output.toByteArray())
        bytes[100000] = (bytes[100000] + 1).toByte()
        assertNotEquals(result.first, EvidenceHash.copyAndHash(ByteArrayInputStream(bytes), ByteArrayOutputStream()).first)
    }
}

