package com.msarobotika.cyberdeck

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

object EvidenceHash {
    fun copyAndHash(input: InputStream, output: OutputStream): Pair<String, Long> {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(65536)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            output.write(buffer, 0, count)
            digest.update(buffer, 0, count)
            total += count
        }
        return digest.digest().joinToString("") { "%02x".format(it) } to total
    }
}

