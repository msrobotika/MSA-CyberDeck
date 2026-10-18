package com.msarobotika.cyberdeck

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Encodes locally. The temporary address never goes to an external QR service. */
fun receiverQrPixels(address: String, size: Int = 512): IntArray {
    require(address.length in 1..512 && size in 128..1024)
    val matrix = QRCodeWriter().encode(address, BarcodeFormat.QR_CODE, size, size,
        mapOf(EncodeHintType.MARGIN to 4, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M))
    return IntArray(size * size) { index -> if(matrix[index % size, index / size]) 0xff000000.toInt() else 0xffffffff.toInt() }
}
