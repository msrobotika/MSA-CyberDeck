package com.msarobotika.cyberdeck

import org.junit.Assert.*
import org.junit.Test

class DiagnosticPresentationTest {
 private val result=DiagnosticResult("fixture","owned.example","2026-10-20T10:00:00Z","Explicit unit fixture; not a device observation",listOf("Método" to "Consulta HEAD","Cabecera" to "Content-Type","Cabecera" to "Content-Length","Estado" to "200"))
 @Test fun blankFilterPreservesAllFieldsAndDuplicates(){assertEquals(result.details,DiagnosticPresentation.details(result," \n "))}
 @Test fun searchIgnoresCaseAndAccents(){assertEquals(listOf(result.details.first()),DiagnosticPresentation.details(result,"METODO"))}
 @Test fun multipleWordsCanMatchKeyAndValue(){assertEquals(listOf(result.details[2]),DiagnosticPresentation.details(result,"cabecera length"))}
 @Test fun unmatchedSearchDoesNotModifyOriginal(){assertTrue(DiagnosticPresentation.details(result,"missing fixture value").isEmpty());assertEquals(4,result.details.size)}
 @Test fun emptyResultRemainsEmpty(){assertTrue(DiagnosticPresentation.details(result.copy(details=emptyList()),"").isEmpty())}
 @Test fun copiedSummaryKeepsSourceDateAndTargetWithoutPretendingToBeTheReport(){val text=DiagnosticPresentation.summary(result);assertTrue(text.contains(result.checkedAt));assertTrue(text.contains(result.target));assertTrue(text.contains(result.summary));assertFalse(text.contains("Content-Type"));assertTrue(text.contains("sin campos técnicos"))}
}
