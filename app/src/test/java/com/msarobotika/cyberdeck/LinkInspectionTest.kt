package com.msarobotika.cyberdeck
import org.junit.Test
import org.junit.Assert.*
class LinkInspectionTest {
 @Test fun destinationAfterCredentials(){val r=LinkInspection.inspect("https://trusted.example@evil.example/login?secret=x");assertEquals("evil.example",r.asciiHost);assertEquals(1,r.signals);assertFalse(r.diagnostic().toString().contains("secret=x"))}
 @Test fun internationalDomain(){assertTrue(LinkInspection.inspect("https://bücher.example/").asciiHost.startsWith("xn--"))}
 @Test fun transportAndIp(){assertEquals(3,LinkInspection.inspect("http://192.0.2.1:8080/").signals)}
 @Test fun ipv6(){assertEquals("[::1]",LinkInspection.inspect("https://[::1]:443/").asciiHost)}
 @Test fun shortener(){assertEquals(1,LinkInspection.inspect("https://bit.ly/abc").signals)}
 @Test fun ordinaryIsNotGuarantee(){assertEquals(0,LinkInspection.inspect("https://example.org/").signals)}
 @Test fun invalidInputs(){listOf("javascript:alert(1)","https://","https://a.example:wat","https://a.example:99999","https://a.example/\nwrong").forEach{assertTrue(it,runCatching{LinkInspection.inspect(it)}.isFailure)}}
}
