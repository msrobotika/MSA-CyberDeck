package com.msarobotika.cyberdeck
import org.junit.Test
import org.junit.Assert.*
class AuditWorkTest {
 private val id="00000000-0000-4000-8000-000000000001"
 private val evidenceId="00000000-0000-4000-8000-000000000002"
 private fun work()=AuditWork(id,"Laboratorio","Solo equipos propios","127.0.0.1","Notas","2026-10-19T00:00:00Z")
 private fun entry(hash:String="a".repeat(64))=Evidence(evidenceId,"diagnostico-propio.json",hash,10,"2026-10-19T00:00:00Z")
 @Test fun metadataAndPinnedReferencesRoundTrip(){val state=AuditWorkState(id,listOf(AuditWorkPolicy.attach(work(),listOf(entry()))));assertEquals(state,AuditWorkCodec.decode(AuditWorkCodec.encode(state)))}
 @Test fun duplicateAssociationDoesNotDuplicateOrReplaceHash(){val w=AuditWorkPolicy.attach(work(),listOf(entry()));assertEquals(w,AuditWorkPolicy.attach(w,listOf(entry())));assertTrue(runCatching{AuditWorkPolicy.attach(w,listOf(entry("b".repeat(64))))}.isFailure)}
 @Test fun archivedWorkCannotBeActiveOrReceiveAutomaticEvidence(){val w=work().copy(archived=true);assertTrue(runCatching{AuditWorkPolicy.validate(AuditWorkState(id,listOf(w)))}.isFailure);assertTrue(runCatching{AuditWorkPolicy.attach(w,listOf(entry()))}.isFailure)}
 @Test fun pinMatchesSizeAndHashNotOnlyId(){val e=entry();val ref=AuditWorkPolicy.attach(work(),listOf(e)).evidence.single();assertTrue(AuditWorkPolicy.matches(ref,e));assertFalse(AuditWorkPolicy.matches(ref,e.copy(bytes=11)));assertFalse(AuditWorkPolicy.matches(ref,e.copy(sha256="b".repeat(64))))}
 @Test fun malformedReferencesAndOversizedMetadataRejected(){assertFalse(AuditWorkPolicy.validId("../index.json"));assertTrue(runCatching{AuditWorkCodec.encode(AuditWorkState(works=listOf(work().copy(name=""))))}.isFailure);assertTrue(runCatching{AuditWorkCodec.encode(AuditWorkState(works=listOf(work().copy(notes="a".repeat(6001)))))}.isFailure)}
 @Test fun unknownActiveOrFormatNotSilentlyAccepted(){assertTrue(runCatching{AuditWorkCodec.encode(AuditWorkState(id))}.isFailure);assertTrue(runCatching{AuditWorkCodec.decode("{\"format\":\"unknown\",\"works\":[]}")}.isFailure)}
}
