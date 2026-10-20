package com.msarobotika.cyberdeck
import org.junit.Assert.*
import org.junit.Test
class PhoneBriefPolicyTest {
 private fun c(title:String,tone:PhoneTone,section:String="Seguridad")=PhoneCheck(title,"Observed","Explanation",tone,section)
 @Test fun unknownsAreSeparatedFromReviewAndInformationalFactsAreNotEscalated(){val checks=listOf(c("ok",PhoneTone.OK),c("unknown-1",PhoneTone.UNKNOWN),c("info",PhoneTone.INFO),c("review",PhoneTone.REVIEW),c("unknown-2",PhoneTone.UNKNOWN));assertEquals(listOf("review","unknown-1","unknown-2"),PhoneBriefPolicy.priorities(checks).map{it.title});assertEquals("Dato no disponible",PhoneBriefPolicy.label(PhoneTone.UNKNOWN))}
 @Test fun noPriorityDoesNotBecomeAGlobalSafetyVerdict(){assertTrue(PhoneBriefPolicy.priorities(listOf(c("lock",PhoneTone.OK),c("debug",PhoneTone.INFO))).isEmpty());assertFalse(PhoneBriefPolicy.label(PhoneTone.OK).contains("seguro",true))}
 @Test fun sameTitleInDifferentSectionsDoesNotSelectAnotherObservation(){assertNotEquals(PhoneBriefPolicy.key(c("state",PhoneTone.INFO,"Seguridad")),PhoneBriefPolicy.key(c("state",PhoneTone.INFO,"Conexión")))}
}
