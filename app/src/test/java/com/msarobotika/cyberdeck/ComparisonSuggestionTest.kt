package com.msarobotika.cyberdeck

import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.CancellationException

class ComparisonSuggestionTest {
 private fun entry(id:String)=Evidence(id,"diagnostico-phone-$id.json","0".repeat(64),20,"2026-10-19T00:00:00Z")
 private fun result(kind:String="phone",target:String="Owned phone")=DiagnosticResult(kind,target,"2026-10-19T00:00:00Z","Fixture only",emptyList())
 @Test fun newestCurrentWinsEvenWhenItsMateIsOlder(){
  val entries=listOf("new-A","new-B","old-B","old-A").map(::entry)
  val pair=suggestComparison(entries){result(target=if(it.id.endsWith("A"))"A" else "B")}!!
  assertEquals("old-A",pair.baseline.entry.id);assertEquals("new-A",pair.current.entry.id)
 }
 @Test fun saveOrderDoesNotPretendObservationChronology(){
  val pair=suggestComparison(listOf(entry("new"),entry("old"))){result().copy(checkedAt=if(it.id=="new")"2020-01-01T00:00:00Z" else "2026-01-01T00:00:00Z")}!!
  assertEquals("new",pair.current.entry.id)
 }
 @Test fun differentTypesAndCaseSensitiveTargetsNeverMix(){
  assertNull(suggestComparison(listOf(entry("a"),entry("b"))){result(kind=it.id)})
  assertNull(suggestComparison(listOf(entry("a"),entry("b"))){result(target=if(it.id=="a")"Owned" else "owned")})
 }
 @Test fun brokenCopiesAreExcludedWithoutBlockingOtherGroups(){
  val pair=suggestComparison(listOf("corrupt","a","b").map(::entry)){if(it.id=="corrupt")error("hash mismatch")else result()}!!
  assertEquals("b",pair.baseline.entry.id);assertEquals("a",pair.current.entry.id)
 }
 @Test fun duplicateIdsCannotBecomeTwoSources(){assertNull(suggestComparison(listOf(entry("same"),entry("same"))){result()})}
 @Test fun scanningIsBoundedTo100Inputs(){
  var calls=0
  val pair=suggestComparison((1..102).map{entry(it.toString())}){calls++;result(target=if(it.id.toInt()>100)"pair" else it.id)}
  assertEquals(100,calls);assertNull(pair)
 }
 @Test fun invalidAndDerivedComparisonsCannotBeSuggested(){
  for(invalid in listOf(result(kind=""),result(target=""),result(kind="comparison"),result().copy(details=(1..257).map{"k" to "v"}))){assertNull(suggestComparison(listOf(entry("a"),entry("b"))){invalid})}
 }
 @Test fun cancellationIsNeverTreatedAsBadEvidence(){
  try{suggestComparison(listOf(entry("a"))){throw CancellationException("stop")};fail("Cancellation lost")}catch(_:CancellationException){}
 }
}
