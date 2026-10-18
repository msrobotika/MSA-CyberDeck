package com.msarobotika.cyberdeck

import org.junit.Assert.*
import org.junit.Test

class ReportContextTest {
    @Test fun contextKeepsDeclaredScopeSeparateFromCollectedResults() {
        val result=ReportContext("Laboratorio","127.0.0.1 solamente","Repetir después de corregir").markdown()
        assertTrue(result.contains("Alcance declarado"));assertTrue(result.contains("> 127.0.0.1 solamente"))
        assertTrue(result.contains("Notas y conclusiones del autor"))
    }
    @Test fun emptyContextAddsNoInventedProject() { assertEquals("",ReportContext().markdown()) }
    @Test(expected=IllegalArgumentException::class) fun excessiveNotesRejected(){ReportContext(notes="a".repeat(6001)).markdown()}
    @Test fun codeFencesInNotesDoNotEscapeTheQuotedBlock(){assertFalse(ReportContext(notes="```\nraw").markdown().contains("```"))}
}
