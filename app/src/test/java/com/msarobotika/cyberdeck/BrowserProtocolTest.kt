package com.msarobotika.cyberdeck

import org.junit.Assert.*
import org.junit.Test

class BrowserProtocolTest {
    private fun raw(extra: String = "", origin: String = "http://192.168.1.2:1234", size: String = "3") =
        "POST /s/secret/upload HTTP/1.1\r\nHost: 192.168.1.2:1234\r\nOrigin: $origin\r\nX-CyberDeck-Upload: selected-file\r\nX-File-Name: test%20file.txt\r\nContent-Type: application/octet-stream\r\nContent-Length: $size\r\n$extra\r\n"
    private fun parse(raw: String) = BrowserProtocol.request(raw.byteInputStream())
    @Test fun headersDoNotConsumeFileBody() {
        val input = (raw() + "abc").byteInputStream()
        val request = BrowserProtocol.request(input)
        assertEquals("test file.txt" to 3, BrowserProtocol.upload(request,"192.168.1.2:1234","/s/secret"))
        assertEquals("abc", input.readBytes().toString(Charsets.UTF_8))
    }
    @Test(expected = IllegalArgumentException::class) fun duplicateLengthsRejected() { parse(raw("Content-Length: 4\r\n")) }
    @Test(expected = IllegalArgumentException::class) fun chunkedBodiesRejected() { parse(raw("Transfer-Encoding: chunked\r\n")) }
    @Test(expected = IllegalArgumentException::class) fun foreignOriginRejected() {
        BrowserProtocol.upload(parse(raw(origin="https://evil.invalid")),"192.168.1.2:1234","/s/secret")
    }
    @Test(expected = IllegalArgumentException::class) fun wrongSecretRejected() {
        BrowserProtocol.upload(parse(raw()),"192.168.1.2:1234","/s/other")
    }
    @Test(expected = IllegalArgumentException::class) fun oversizedUploadRejected() {
        BrowserProtocol.upload(parse(raw(size="16777217")),"192.168.1.2:1234","/s/secret")
    }
    @Test(expected = IllegalArgumentException::class) fun oversizedHeadersRejected() { parse(raw("X-Large: " + "a".repeat(8200) + "\r\n")) }
    @Test(expected = IllegalArgumentException::class) fun incompleteRequestRejected() { parse("GET / HTTP/1.1\r\n") }
}
