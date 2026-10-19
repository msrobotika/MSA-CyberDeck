package com.msarobotika.cyberdeck
import org.junit.Test
import org.junit.Assert.*
class MagneticPolicyTest {
 @Test fun vectorMagnitudeHasMicroteslaUnits(){assertEquals(13.0,MagneticPolicy.magnitude(3.0,4.0,12.0),.0001);assertEquals(0.0,MagneticPolicy.magnitude(0.0,0.0,0.0),.0)}
 @Test fun orientationSignDoesNotChangeMagnitude(){assertEquals(MagneticPolicy.magnitude(3.0,4.0,12.0),MagneticPolicy.magnitude(-3.0,-4.0,-12.0),.0001)}
 @Test fun rejectsNonFiniteSensorData(){assertTrue(runCatching{MagneticPolicy.magnitude(Double.NaN,0.0,0.0)}.isFailure)}
}
