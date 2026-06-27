package com.viewsonic.classswift.ui.windowmodel

import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.utils.LanguageUtils
import timber.log.Timber

class LeaderboardWindowModel(
    private val accountManager: AccountManager,
    private val classroomManager: ClassroomManager,
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider
) {

    //After onReceivedError will receive onPageFinished, so need loadUrlHadError to know page complete is success or failed
    var loadUrlHadError = false

    fun isMyViewBoardBound() = myViewBoardConnectionStateProvider.isBound()

    fun getLeaderBoardUrl(): String {
        //keep for debug in dev
//        return "https://african-golden-cat-681.classswift-dev.com/leaderboard/${classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.id}"
        return "${BuildConfig.CLASS_SWIFT_HUB_URL}/leaderboard/${classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.id}" +
                "?lang=${LanguageUtils.webLanguageCode}"
    }

    fun getFullRecordUrl(lessonId: String): String {
        val recordUrl = "${BuildConfig.CLASS_SWIFT_HUB_URL}/signin?orgId=${accountManager.selectedOrg?.orgId}&lessonId=$lessonId" +
                "&lang=${LanguageUtils.webLanguageCode}"
        Timber.tag("Leaderboard").d("getFullRecordUrl: $recordUrl")
        return recordUrl
    }

    fun getAccessToken(): String {
        return accountManager.getAccessToken()
    }
}