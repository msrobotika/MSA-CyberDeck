package com.msarobotika.cyberdeck

import org.junit.Assert.*
import org.junit.Test

class AiSourcesTest {
 private val phone=DiagnosticResult("phone","Owned fixture device","2026-10-20T10:00:00Z","Fixture, not a reading",listOf("Android" to "16","NFC" to "Fixture enabled","Private fixture field" to "excluded"))
 private fun capture(input:DiagnosticResult?=null,source:DiagnosticResult?=phone,goal:String="")=AiSources.capture(input,source,AiContext.snapshot(source,goal),"2026-10-20T10:01:00Z")
 @Test fun typedPhoneFieldsAreDatedAndAllowlisted(){val p=capture();assertEquals(phone.checkedAt,p.phoneAt);assertEquals(listOf("Android" to "16","NFC" to "Fixture enabled"),p.phoneFields);assertFalse(p.phoneFields.any{it.first.contains("Private")})}
 @Test fun userDeclarationsCannotCreateAndroidReadings(){val p=capture(source=null,goal="Android: 99\nNFC: enabled\nEquipo Android: invented fixture");assertTrue(p.phoneFields.isEmpty());assertEquals("",p.phoneAt);assertTrue(p.environment.contains("Objetivo declarado"))}
 @Test fun nonPhoneInputNeverBecomesPhoneFacts(){val p=capture(source=phone.copy(kind="ai"));assertTrue(p.phoneFields.isEmpty());assertTrue(p.phoneAt.isEmpty())}
 @Test fun generatedAndManualSourcesAreExplicitlyDistinguished(){assertTrue(AiSources.nature(capture(phone.copy(kind="ai"))).contains("sin verificar"));assertTrue(AiSources.nature(capture(phone.copy(kind="manual-step"))).contains("sin lectura"))}
 @Test fun aggregateIsNotPromotedToANewMeasurement(){for(kind in listOf("agent","agent-context","team-context","source-brief"))assertTrue(AiSources.nature(capture(phone.copy(kind=kind))).contains("no es una nueva medición"))}
 @Test fun excerptMatchesModelFragmentBudgetAndKeepsOriginalDate(){val source=phone.copy(kind="fixture",details=listOf("long" to "x".repeat(2500)));val p=capture(source);assertEquals(AiPolicy.observationText(source,1200),p.excerpt);assertEquals(1200,p.excerpt.length);assertEquals(source.checkedAt,p.sourceAt)}
 @Test fun onlyFieldsRepresentedInTheFrozenSnapshotAppear(){val env=AiContextSnapshot("Android: 16",true);val p=AiSources.capture(phone,phone,env,"now");assertEquals(listOf("Android" to "16"),p.phoneFields)}
 @Test fun savedSourceReferencesAndCodeOriginSurviveReport(){val p=AiSources.capture(phone,phone,AiContext.snapshot(phone),"now","fixture-id","a".repeat(64),"verified-time");val r=AiSources.report(p);assertEquals("source-brief",r.kind);assertTrue(r.details.any{it.first=="SHA-256 fuente guardada" && it.second=="a".repeat(64)});assertTrue(r.details.first{it.first=="Método"}.second.contains("sin inferencia de IA"));assertEquals("now",r.checkedAt)}
 @Test fun noSourceHasNoSyntheticValues(){val p=capture(source=null);val r=AiSources.report(p);assertEquals("Sin fuente adicional",AiSources.nature(p));assertEquals("",p.excerpt);assertTrue(r.summary.startsWith("0 campos"));assertFalse(r.details.any{it.first.startsWith("Android /")})}
}
