package com.viewsonic.classswift.uimanager.maintenance

import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaintenancePreDowntimeEligibilityTest {

    @Test
    fun `two-days prompt is allowed when not bound from mvb and prerequisites are met`() {
        val subject = buildSubject(isMvbBound = false)

        assertTrue(
            subject.shouldShowTwoDaysBeforePrompt(
                userId = "teacher-1",
                isServiceStarted = true
            )
        )
    }

    @Test
    fun `five-minutes prompt is blocked when service is not started`() {
        val subject = buildSubject(isMvbBound = false)

        assertFalse(
            subject.shouldShowFiveMinutesBeforePrompt(
                userId = "teacher-1",
                isServiceStarted = false
            )
        )
    }

    @Test
    fun `pre-downtime prompts are blocked when launched from mvb`() {
        val subject = buildSubject(isMvbBound = true)

        assertFalse(
            subject.shouldShowTwoDaysBeforePrompt(
                userId = "teacher-1",
                isServiceStarted = true
            )
        )
        assertFalse(
            subject.shouldShowFiveMinutesBeforePrompt(
                userId = "teacher-1",
                isServiceStarted = true
            )
        )
    }

    @Test
    fun `pre-downtime prompts are blocked when user is not logged in`() {
        val subject = buildSubject(isMvbBound = false)

        assertFalse(
            subject.shouldShowTwoDaysBeforePrompt(
                userId = "",
                isServiceStarted = true
            )
        )
        assertFalse(
            subject.shouldShowFiveMinutesBeforePrompt(
                userId = "",
                isServiceStarted = true
            )
        )
    }

    private fun buildSubject(isMvbBound: Boolean): MaintenancePreDowntimeEligibility {
        return MaintenancePreDowntimeEligibility(
            myViewBoardConnectionStateProvider = FakeMyViewBoardConnectionStateProvider(isMvbBound)
        )
    }

    private class FakeMyViewBoardConnectionStateProvider(
        private val bound: Boolean
    ) : MyViewBoardConnectionStateProvider {
        override fun isBound(): Boolean = bound
        override fun isBoundFlow(): StateFlow<Boolean> = MutableStateFlow(bound)
    }
}
