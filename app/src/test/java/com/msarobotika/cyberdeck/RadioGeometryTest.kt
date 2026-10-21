package com.msarobotika.cyberdeck
import org.junit.Test
import org.junit.Assert.*
class RadioGeometryTest {
    @Test fun angleDependsOnMeasuredChannel(){assertEquals(-Math.PI/2,RadioGeometry.channelAngle(1),.000001);assertEquals(13,(1..13).map{RadioGeometry.channelAngle(it)}.distinct().size)}
    @Test fun signalRadiusHasDeclaredScale(){assertEquals(0f,RadioGeometry.signalRadius(-30),.00001f);assertEquals(.5f,RadioGeometry.signalRadius(-65),.00001f);assertEquals(1f,RadioGeometry.signalRadius(-100),.00001f);assertEquals(1f,RadioGeometry.signalRadius(-120),.00001f)}
    @Test fun invalidChannelsCannotCreateFakeBearing(){assertThrows(IllegalArgumentException::class.java){RadioGeometry.channelAngle(14)}}
}
