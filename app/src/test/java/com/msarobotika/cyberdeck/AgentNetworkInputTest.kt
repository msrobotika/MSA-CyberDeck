package com.msarobotika.cyberdeck
import org.junit.Assert.*
import org.junit.Test
class AgentNetworkInputTest {
 @Test fun networkRequestsHandleTheSameConceptAcrossCommonSpellings(){listOf("analiza mi red wifi","Analiza mi red Wi-Fi","revisa la red inalámbrica","comprueba mi red","Jarvis, analiza mi red wifi por favor","analiza   mi   red   wi fi","mira mi conexión wifi","dime cómo está mi red wifi actual","Analiza mi red Wi‑Fi").forEach{assertEquals(it,AgentTasks.connection,AgentTasks.match(it))}}
 @Test fun wifiRequestIsALocalDiagnosticNotAnEspScanOrUnauthorizedLanProbe(){val task=AgentTasks.match("analiza mi red wifi")!!;assertTrue(task.instant);assertEquals(listOf(AgentTool.CONNECTION,AgentTool.RESOURCES),task.steps);assertTrue(task.steps.none{it.network || it==AgentTool.RADIO || it==AgentTool.USB});assertFalse(task.inputs.networkApproved)}
 @Test fun negatedCompoundSecurityAuditAndExternalTargetsAreNotSilentlyReducedToConnectivity(){listOf("no analices mi red wifi","analiza mi red wifi y lee NFC","analiza mi red wifi y borra archivos","analiza la seguridad de mi red wifi","escanea mi red wifi","analiza mi red wifi 192.168.1.1").forEach{assertNull(it,AgentTasks.match(it))}}
 @Test fun aNewTaskRoutesIdenticallyInTheInitialAndContinuationFields(){for(hasRun in listOf(false,true))assertEquals(AgentInput.Task(AgentTasks.connection),AgentInputPolicy.resolve("analiza mi red wifi",hasRun));assertEquals(AgentInput.MissingContext,AgentInputPolicy.resolve("repítelo",false));assertEquals(AgentInput.Followup(AgentFollowup.REPEAT),AgentInputPolicy.resolve("repítelo",true));assertEquals(AgentInput.Unknown,AgentInputPolicy.resolve("no analices mi red wifi",true))}
}
