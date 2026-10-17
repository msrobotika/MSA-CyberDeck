package com.msarobotika.cyberdeck

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.*
import org.junit.Test

class ReceiverQrTest {
    @Test fun temporaryAddressRoundTripsExactlyWithoutNetwork() {
        val address = "http://192.168.1.18:49152/s/" + "a1".repeat(24)
        val pixels = receiverQrPixels(address)
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(512,512,pixels)))
        assertEquals(address, QRCodeReader().decode(bitmap).text)
    }
}
