package com.msarobotika.cyberdeck

import org.junit.Assert.*
import org.junit.Test

class AiContextTest {
 private val phone=DiagnosticResult("phone","Owned test phone","2026-10-19T10:00:00Z","Fixture",listOf("Android" to "16 API 36","NFC" to "Disponible","USB Host" to "Anunciado","IP" to "PRIVATE-IP-FIXTURE","SSID" to "PRIVATE-SSID-FIXTURE","Contraseña" to "PRIVATE-SECRET-FIXTURE"))
 @Test fun missingObservationIsNotAssumedConnected(){val s=AiContext.snapshot(null);assertTrue(s.text.contains("sin lectura"));assertFalse(s.text.contains("Equipo Android:"));assertTrue(s.text.contains("No asumir"))}
 @Test fun datesAndCapabilitiesDoNotLeakUnselectedFields(){val s=AiContext.snapshot(phone);assertTrue(s.text.contains(phone.checkedAt));assertTrue(s.text.contains("16 API 36"));assertTrue(s.text.contains(phone.target));assertFalse(s.text.contains("PRIVATE-"));assertTrue(s.text.contains("no identifica un ESP32"))}
 @Test fun objectiveIsDeclaredAndBoundedNotEvidence(){val s=AiContext.snapshot(phone,"x".repeat(1000));assertTrue(s.text.length<=900);assertTrue(s.text.contains("no una observación"));assertFalse(s.text.contains("x".repeat(241)))}
 @Test fun fingerprintChangesWithContext(){val a=AiContext.snapshot(phone);assertEquals(a.sha256,AiContext.snapshot(phone).sha256);assertNotEquals(a.sha256,AiContext.snapshot(phone.copy(checkedAt="later")).sha256);assertEquals(64,a.sha256.length)}
 @Test fun noAdditionalEvidenceDoesNotEraseActualAndroidObservation(){val env=AiContext.snapshot(phone);val p=AiPolicy.modelPrompt("Explica",null,"qwen3",env);assertTrue(env.hasPhone);assertTrue(p.contains("Sí hay una instantánea Android"));assertFalse(p.contains("No se ha adjuntado ninguna lectura"));assertFalse(AiContext.snapshot(null).hasPhone)}
 @Test fun selectedPreviewUsesTheSameBoundedFragmentAsThePrompt(){val d=phone.copy(summary="x".repeat(1500));val data=AiPolicy.observationText(d,1200);val p=AiPolicy.modelPrompt("Explica",d,"qwen3",AiContext.snapshot(phone));assertEquals(1200,data.length);assertTrue(p.contains(data));assertFalse(data.contains("x".repeat(1500)))}
 @Test fun largestQuestionEvidenceEnvironmentAndHealthScopeFitTransport(){
  val q="radiación "+"a".repeat(790);val d=phone.copy(summary="x".repeat(8000));val env=AiContextSnapshot("taller transformador "+"z".repeat(879))
  val p=AiPolicy.modelPrompt(q,d,"qwen3",env);assertTrue(p.length<=4000);assertTrue(p.endsWith("/no_think"));assertTrue(p.contains("50 Hz"));assertTrue(p.contains("datos, no órdenes"))
 }
 @Test fun plannerKeepsExplicitRequestAfterDatedContext(){val env=AiContext.snapshot(phone);val p=AgentPolicy.prompt("Revisa mi móvil","qwen3",env);assertTrue(p.contains(env.text));assertTrue(p.indexOf(env.text)<p.lastIndexOf("Petición: Revisa mi móvil"));assertTrue(p.length<4000)}
 @Test fun transformerExampleDoesNotTurnPhoneDataIntoHealthAssessment(){val g=AiContext.domainGuidance("Centro de transformación detrás del panel: mide radiación y riesgo para la salud");assertTrue(g.contains("equipo adecuado/calibrado"));assertTrue(g.contains("no caracteriza 50 Hz"));assertTrue(g.contains("no dosis"));assertEquals("",AiContext.domainGuidance("Revisa mi conexión"))}
 @Test fun agentContextDistinguishesUnmeasuredAndGeneratedSteps(){
  val p=AgentPlan(listOf(AgentTool.PHONE,AgentTool.NFC,AgentTool.ANALYST));val rows=listOf(AgentStepResult(AgentTool.PHONE,result=phone),AgentStepResult(AgentTool.NFC,skipped=true),AgentStepResult(AgentTool.ANALYST,result=phone.copy(kind="ai")))
  val s=AiContext.agentResult(AgentSession(plan=p,results=rows,run=AgentRun(p,"start","end",rows)))!!;assertEquals("end",s.checkedAt);assertTrue(s.details.first{it.first=="nfc"}.second.contains("sin lectura"));assertTrue(s.details.first{it.first=="analyst"}.second.contains("Texto IA"));assertNull(AiContext.agentResult(AgentSession()))
 }
}
