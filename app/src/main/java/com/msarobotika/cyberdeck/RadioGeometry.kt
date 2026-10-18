package com.msarobotika.cyberdeck

object RadioGeometry {
    fun channelAngle(channel:Int):Double{require(channel in 1..13);return -Math.PI/2+(channel-1)*Math.PI*2/13}
    fun signalRadius(rssi:Int)=((-rssi-30f)/70f).coerceIn(0f,1f)
}
