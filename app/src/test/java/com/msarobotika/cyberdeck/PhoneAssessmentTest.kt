package com.msarobotika.cyberdeck

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PhoneAssessmentTest {
    @Test fun unavailableLockStateIsNotReportedAsInsecure(){assertEquals(PhoneTone.UNKNOWN,PhoneAssessment.lock(null).tone)}
    @Test fun configuredAndMissingLockAreDistinguished(){assertEquals(PhoneTone.OK,PhoneAssessment.lock(true).tone);assertEquals(PhoneTone.REVIEW,PhoneAssessment.lock(false).tone)}
    @Test fun adbIsInformationNotAnAttack(){assertEquals(PhoneTone.INFO,PhoneAssessment.debugging(1).tone);assertEquals(PhoneTone.INFO,PhoneAssessment.debugging(0).tone);assertEquals(PhoneTone.UNKNOWN,PhoneAssessment.debugging(-1).tone)}
    @Test fun futurePatchDoesNotGetAFalsePositive(){assertEquals(PhoneTone.UNKNOWN,PhoneAssessment.patch("2027-01-01",LocalDate.of(2026,10,18)).tone)}
    @Test fun invalidPatchIsUnknown(){assertEquals(PhoneTone.UNKNOWN,PhoneAssessment.patch("invalid",LocalDate.of(2026,10,18)).tone)}
    @Test fun patchReviewThresholdIsExplicit(){val now=LocalDate.of(2026,10,18);assertEquals(PhoneTone.INFO,PhoneAssessment.patch(now.minusDays(180).toString(),now).tone);assertEquals(PhoneTone.REVIEW,PhoneAssessment.patch(now.minusDays(181).toString(),now).tone)}
}
