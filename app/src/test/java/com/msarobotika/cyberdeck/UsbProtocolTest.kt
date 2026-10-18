package com.msarobotika.cyberdeck

import org.junit.Test
import org.junit.Assert.*
import org.json.JSONObject

class UsbProtocolTest {
    @Test fun fragmentedUtf8AndCrLfAreDecodedOnce(){
        val d=UsbFrameDecoder();val bytes="{\"ssid\":\"café\"}\r\n".toByteArray()
        val lines=bytes.flatMap{d.accept(byteArrayOf(it))}
        assertEquals(listOf("{\"ssid\":\"café\"}"),lines)
    }
    @Test fun oversizedFrameCannotBecomeValidSuffix(){
        val d=UsbFrameDecoder(8)
        assertEquals(listOf("ok"),d.accept("123456789VALID\nok\n".toByteArray()))
    }
    @Test fun detachClearsPartialFrame(){val d=UsbFrameDecoder();d.accept("old".toByteArray());d.clear();assertEquals(listOf("new"),d.accept("new\n".toByteArray()))}
    @Test fun jsonRepliesRequireMatchingIdAndCommand(){
        val line="{\"sf\":1,\"ok\":true,\"cmd\":\"STATUS\",\"id\":12}"
        assertTrue(UsbProtocol.matches(line,"STATUS",12,UsbDialect.JSON))
        assertFalse(UsbProtocol.matches(line,"STATUS",11,UsbDialect.JSON))
        assertFalse(UsbProtocol.matches(line,"HELLO",12,UsbDialect.JSON))
        assertFalse(UsbProtocol.matches("{\"sf\":1,\"ok\":false,\"cmd\":\"STATUS\",\"id\":12}","STATUS",12,UsbDialect.JSON))
    }
    @Test fun missingIdIsNotAcceptedAsJsonTransaction(){assertFalse(UsbProtocol.matches("{\"sf\":1,\"ok\":true,\"cmd\":\"STATUS\"}","STATUS",1,UsbDialect.JSON))}
    @Test fun legacyAndSf1ResponsesRemainCompatible(){
        assertTrue(UsbProtocol.matches("{\"result\":\"status\"}","LEGACY_STATUS",1,UsbDialect.UNKNOWN))
        assertTrue(UsbProtocol.matches("{\"sf\":1,\"ok\":true,\"cmd\":\"HELLO\"}","HELLO",1,UsbDialect.SF1))
        assertEquals("SF1 HELLO\n",UsbProtocol.request("HELLO",1,UsbDialect.SF1))
    }
    @Test fun onlyKnownMsaLegacyFirmwareIsIdentified(){
        assertNull(UsbProtocol.identity("{\"result\":\"status\",\"firmware\":\"unrelated\"}"))
        val obs=UsbProtocol.identity("{\"result\":\"status\",\"firmware\":\"MSA-WiFi-Sentinel-0.2.1-CyberDeck\"}")!!
        assertEquals(UsbDialect.LEGACY,obs.dialect);assertNull(obs.chip)
    }
    @Test fun modernIdentityDoesNotInferSocFromUsbAdapter(){
        val obs=UsbProtocol.identity("{\"sf\":1,\"ok\":true,\"firmware\":\"SuperFlyper\",\"protocol\":\"json-v1\",\"chip\":\"ESP32-D0WD\",\"capabilities\":[\"STATUS\"]}")!!
        assertEquals(UsbDialect.JSON,obs.dialect);assertEquals("ESP32-D0WD",obs.chip);assertEquals(listOf("STATUS"),obs.capabilities)
    }
    @Test fun malformedAndUnrelatedDataAreNotIdentity(){assertNull(UsbProtocol.identity("boot log"));assertNull(UsbProtocol.identity("{}"));assertFalse(UsbProtocol.matches("noise","STATUS",1,UsbDialect.JSON))}
    @Test fun arbitraryWritesAreRejected(){assertThrows(IllegalArgumentException::class.java){UsbProtocol.request("GPIO_WRITE",1,UsbDialect.JSON)}}
    @Test fun modernRequestIncludesCorrelationId(){val j=JSONObject(UsbProtocol.request("STATUS",42,UsbDialect.JSON));assertEquals(42,j.getInt("id"));assertEquals("STATUS",j.getString("cmd"))}
}
