package com.msarobotika.cyberdeck
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CancellationException
class FieldInvestigationTest {
 private fun e(id:String)=Evidence(id,"diagnostico-fixture.json","a".repeat(64),10,"2026-10-20T00:00:00Z")
 private fun r(target:String="Owned",kind:String="phone",value:String="one")=DiagnosticResult(kind,target,"2026-10-20T00:00:00Z","Fixture",listOf("state" to value))
 @Test fun allGroupsUseNewestTwoCopiesAndKeepSources(){
  val result=investigateSavedReadings(listOf("A3","B2","A2","A1","B1").map(::e)){r(target=it.id.take(1),value=it.id)}
  assertEquals(2,result.pairs.size);assertEquals(2,result.changed);assertEquals(5,result.verified)
  assertEquals("A2",result.pairs[0].first.id);assertEquals("A3",result.pairs[0].second.id)
  assertTrue(result.diagnostic().details.any{it.second.contains("A3 · SHA-256")})
 }
 @Test fun noPairIsNotNoRisk(){val result=investigateSavedReadings(listOf(e("a"))){r()};assertTrue(result.pairs.isEmpty());assertEquals(1,result.unpaired);assertTrue(result.diagnostic().summary.contains("no acredita ataques"))}
 @Test fun exactTypeAndTargetAreRequired(){val result=investigateSavedReadings(listOf(e("a"),e("b"),e("c"))){r(target=if(it.id=="a")"Owned" else "owned",kind=if(it.id=="c")"connection" else "phone")};assertEquals(3,result.unpaired);assertTrue(result.pairs.isEmpty())}
 @Test fun duplicatesDerivedAndCorruptAreExcluded(){val result=investigateSavedReadings(listOf("bad","a","a","derived","mission","b").map(::e)){when(it.id){"bad"->error("hash mismatch");"derived"->r(kind="comparison");"mission"->r(kind="investigation");else->r()}};assertEquals(1,result.unreadable);assertEquals(2,result.verified);assertEquals(1,result.pairs.size)}
 @Test fun scanIsBoundedAndCancellationPropagates(){var n=0;investigateSavedReadings((1..105).map{e("$it")}){n++;r()};assertEquals(100,n);try{investigateSavedReadings(listOf(e("a"))){throw CancellationException()};fail()}catch(_:CancellationException){}}
 @Test fun savedSummaryDoesNotCopyOriginalValues(){val result=investigateSavedReadings(listOf(e("a"),e("b"))){r(value="PRIVATE_ORIGINAL_VALUE")};assertFalse(result.diagnostic().toEvidenceJson().contains("PRIVATE_ORIGINAL_VALUE"));assertEquals(0,result.changed)}
 @Test fun largeReportsStayReadable(){val inputs=(0..99).map{e("$it")};val result=investigateSavedReadings(inputs){r(target="界".repeat(400)+"${it.id.toInt()/2}").copy(checkedAt="x".repeat(10000))};assertEquals(50,result.pairs.size);assertTrue(result.diagnostic().toEvidenceJson().toByteArray(Charsets.UTF_8).size<=65536)}
 @Test fun invalidShapesAreNotCompared(){for(invalid in listOf(r(target=""),r(kind=""),r().copy(details=(1..257).map{"key" to "value"}))){assertTrue(investigateSavedReadings(listOf(e("a"),e("b"))){invalid}.pairs.isEmpty())}}
}
