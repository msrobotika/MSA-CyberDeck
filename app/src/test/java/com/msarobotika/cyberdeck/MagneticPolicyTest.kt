package com.msarobotika.cyberdeck
import org.junit.Test
import org.junit.Assert.*
class MagneticPolicyTest {
 @Test fun vectorMagnitudeHasMicroteslaUnits(){assertEquals(13.0,MagneticPolicy.magnitude(3.0,4.0,12.0),.0001);assertEquals(0.0,MagneticPolicy.magnitude(0.0,0.0,0.0),.0)}
 @Test fun orientationSignDoesNotChangeMagnitude(){assertEquals(MagneticPolicy.magnitude(3.0,4.0,12.0),MagneticPolicy.magnitude(-3.0,-4.0,-12.0),.0001)}
 @Test fun rejectsNonFiniteSensorData(){assertTrue(runCatching{MagneticPolicy.magnitude(Double.NaN,0.0,0.0)}.isFailure)}
 @Test fun summaryUsesActualLastVectorAndDoesNotInventRoomOrSafety(){val sample=MagneticSample("now",1,3.0,4.0,12.0,3);val report=MagneticPolicy.report("own test sensor",listOf(sample),null);assertTrue(report.summary.contains("13.0 µT"));assertTrue(report.summary.contains("1 lecturas reales"));assertFalse(report.summary.contains("baño"))}
 @Test fun emptySeriesHasNoInventedFieldValue(){assertEquals("Sin lecturas del magnetómetro",MagneticPolicy.report("own",emptyList(),null).summary)}
}
