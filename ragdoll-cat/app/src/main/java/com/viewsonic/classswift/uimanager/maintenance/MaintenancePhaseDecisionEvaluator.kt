package com.viewsonic.classswift.uimanager.maintenance

class MaintenancePhaseDecisionEvaluator {

    data class TwoDaysDecision(
        val shouldShow: Boolean,
        val shouldMarkAsViewed: Boolean
    )

    fun evaluateTwoDaysBefore(
        isEligible: Boolean,
        isAlreadyViewed: Boolean,
        isCurrentTimeWithinPromptWindow: Boolean
    ): TwoDaysDecision {
        if (!isEligible) {
            return TwoDaysDecision(
                shouldShow = false,
                shouldMarkAsViewed = false
            )
        }

        val shouldShow = !isAlreadyViewed && isCurrentTimeWithinPromptWindow
        return TwoDaysDecision(
            shouldShow = shouldShow,
            shouldMarkAsViewed = shouldShow
        )
    }

    fun evaluateFiveMinutesBefore(
        isEligible: Boolean,
        hasViewedAnnouncement: Boolean,
        isCurrentTimeWithinPromptWindow: Boolean
    ): Boolean {
        return isEligible && !hasViewedAnnouncement && isCurrentTimeWithinPromptWindow
    }
}
