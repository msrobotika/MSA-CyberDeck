package com.msarobotika.cyberdeck
import org.junit.Test
import org.junit.Assert.*
class CallPolicyTest {
 @Test fun normalizeOnlyPresentation(){assertEquals("+34900123456",CallPolicy.number("+34 900-123-456"));assertNotEquals(CallPolicy.number("900123456"),CallPolicy.number("+34900123456"))}
 @Test fun rejectControlCodes(){listOf("*123#","", "abc","12","+123;456").forEach{assertNull(CallPolicy.number(it))}}
 @Test fun onlyExplicitSignalsWarn(){assertFalse(CallPolicy.warn(false,0));assertFalse(CallPolicy.warn(false,1));assertTrue(CallPolicy.warn(false,2));assertTrue(CallPolicy.warn(true,0))}
 @Test fun maskedDoesNotLeakNumber(){assertEquals("••••456",CallPolicy.masked("+34900123456"))}
}
