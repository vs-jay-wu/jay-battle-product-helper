package com.viewsonic.classswift.uimanager.maintenance

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenancePhaseDecisionEvaluatorTest {

    private val subject = MaintenancePhaseDecisionEvaluator()

    @Test
    fun `two-days should show and mark viewed when eligible not viewed and within window`() {
        val result = subject.evaluateTwoDaysBefore(
            isEligible = true,
            isAlreadyViewed = false,
            isCurrentTimeWithinPromptWindow = true
        )

        assertTrue(result.shouldShow)
        assertTrue(result.shouldMarkAsViewed)
    }

    @Test
    fun `two-days should not show again when already viewed and should not mark viewed state`() {
        val result = subject.evaluateTwoDaysBefore(
            isEligible = true,
            isAlreadyViewed = true,
            isCurrentTimeWithinPromptWindow = true
        )

        assertFalse(result.shouldShow)
        assertFalse(result.shouldMarkAsViewed)
    }

    @Test
    fun `two-days should not show or mark when window is not active`() {
        val result = subject.evaluateTwoDaysBefore(
            isEligible = true,
            isAlreadyViewed = false,
            isCurrentTimeWithinPromptWindow = false
        )

        assertFalse(result.shouldShow)
        assertFalse(result.shouldMarkAsViewed)
    }

    @Test
    fun `two-days should not show or mark when ineligible`() {
        val result = subject.evaluateTwoDaysBefore(
            isEligible = false,
            isAlreadyViewed = false,
            isCurrentTimeWithinPromptWindow = true
        )

        assertFalse(result.shouldShow)
        assertFalse(result.shouldMarkAsViewed)
    }

    @Test
    fun `five-minutes should show only when eligible not viewed and within window`() {
        assertTrue(
            subject.evaluateFiveMinutesBefore(
                isEligible = true,
                hasViewedAnnouncement = false,
                isCurrentTimeWithinPromptWindow = true
            )
        )

        assertFalse(
            subject.evaluateFiveMinutesBefore(
                isEligible = true,
                hasViewedAnnouncement = true,
                isCurrentTimeWithinPromptWindow = true
            )
        )
    }

}
