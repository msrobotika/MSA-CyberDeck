package com.msarobotika.cyberdeck
import org.junit.Test
import org.junit.Assert.*
class ExposurePolicyTest {
 @Test fun boundedPorts(){assertEquals(listOf(80,443),ExposurePolicy.ports("80,443,80"));assertTrue(runCatching{ExposurePolicy.ports("1,2,3,4,5,6,7,8,9")}.isFailure)}
 @Test fun rejectsRangesAndInvalidPorts(){listOf("1-65535","0","65536","-2","","80,word").forEach{assertTrue(it,runCatching{ExposurePolicy.ports(it)}.isFailure)}}
 private fun result(state:PortState,address:String="127.0.0.1",cancelled:Boolean=false)=ExposureResult("fixture",address,"now",10,listOf(PortObservation(443,state,10,"fixture")),cancelled,"fixture-network")
 @Test fun differentNetworkNotComparable(){val r=result(PortState.OPEN);assertTrue(ExposurePolicy.changes(r,r.copy(networkKey="other")).single().contains("No comparable"));assertTrue(ExposurePolicy.changes(r,r.copy(networkKey=null)).single().contains("No comparable"))}
 @Test fun networkChangeStopsBeforeConnecting(){val ip=java.net.InetAddress.getByName("127.0.0.1");val plan=ExposurePlan("fixture",listOf(ip),listOf(443),"now","one");val r=ExposureRunner().run(plan,ip,networkUnchanged={false}){};assertTrue(r.cancelled);assertTrue(r.probes.isEmpty())}
 @Test fun changesAreStatesNotAttackClaims(){val changes=ExposurePolicy.changes(result(PortState.OPEN),result(PortState.TIMEOUT));assertEquals(1,changes.size);assertTrue(changes.first().contains("Sin respuesta"))}
 @Test fun differentIpNotComparable(){assertTrue(ExposurePolicy.changes(result(PortState.OPEN),result(PortState.OPEN,"192.0.2.1")).first().contains("No comparable"))}
 @Test fun interruptedNotComparable(){assertTrue(ExposurePolicy.changes(result(PortState.OPEN),result(PortState.OPEN,cancelled=true)).first().contains("No comparable"))}
 @Test fun matchingStates(){assertTrue(ExposurePolicy.changes(result(PortState.OPEN),result(PortState.OPEN)).isEmpty())}
}
