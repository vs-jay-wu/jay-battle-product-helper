package com.viewsonic.classswift.uimanager.maintenance

import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider

class MaintenancePreDowntimeEligibility(
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider
) {
    fun shouldShowTwoDaysBeforePrompt(
        userId: String,
        isServiceStarted: Boolean
    ): Boolean {
        return shouldShowPreDowntimePrompt(
            userId = userId,
            isServiceStarted = isServiceStarted
        )
    }

    fun shouldShowFiveMinutesBeforePrompt(
        userId: String,
        isServiceStarted: Boolean
    ): Boolean {
        return shouldShowPreDowntimePrompt(
            userId = userId,
            isServiceStarted = isServiceStarted
        )
    }

    private fun shouldShowPreDowntimePrompt(
        userId: String,
        isServiceStarted: Boolean
    ): Boolean {
        return userId.isNotBlank() &&
            isServiceStarted &&
            !myViewBoardConnectionStateProvider.isBound()
    }
}
