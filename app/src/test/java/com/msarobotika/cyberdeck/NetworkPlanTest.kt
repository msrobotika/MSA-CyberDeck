package com.msarobotika.cyberdeck
import org.junit.Test
import org.junit.Assert.*
class NetworkPlanTest {
 @Test fun subnet24(){val p=NetworkPlan.parse("192.168.1.20/24");assertEquals("192.168.1.0",NetworkPlan.ip(p.network));assertEquals("192.168.1.255",NetworkPlan.ip(p.broadcast));assertEquals(254L,p.usable)}
 @Test fun defaultRouteDoesNotOverflow(){val p=NetworkPlan.parse("0.0.0.0/0");assertEquals(4294967296L,p.count);assertEquals("255.255.255.255",NetworkPlan.ip(p.broadcast));assertEquals(4294967294L,p.usable)}
 @Test fun pointToPoint31(){val p=NetworkPlan.parse("192.0.2.3/31");assertEquals("192.0.2.2",NetworkPlan.ip(p.first));assertEquals("192.0.2.3",NetworkPlan.ip(p.last));assertEquals(2L,p.usable)}
 @Test fun host32(){val p=NetworkPlan.parse("192.0.2.10/32");assertEquals(p.address,p.first);assertEquals(p.first,p.last);assertEquals(1L,p.usable)}
 @Test fun rejectsAmbiguousAndInvalid(){listOf("example.com/24","010.0.0.1/8","1.2.3.256/24","1.2.3.4/33","1.2.3.4/-1","1.2.3.4/024","1.2.3.4").forEach{assertTrue(it,runCatching{NetworkPlan.parse(it)}.isFailure)}}
}
