package com.viewsonic.classswift.ui.webInterface

import android.webkit.JavascriptInterface
import com.viewsonic.classswift.api.moshi.MoshiProvider
import com.viewsonic.classswift.data.leaderboard.LessonIdInfo
import com.viewsonic.classswift.data.spinner.SpinnerStudentInfo
import timber.log.Timber

class LeaderBoardWebInterface {

    private val lessonIdAdapter = MoshiProvider.moshiNormal.adapter(
        LessonIdInfo::class.java
    )

    private var leaderBoardListener: LeaderBoardWebListener? = null

    fun setListener(listener: LeaderBoardWebListener) {
        leaderBoardListener = listener
    }

    @JavascriptInterface
    fun getAccessToken(): String {
        Timber.tag("LeaderBoardWebInterface").d("getAccessToken")
        return leaderBoardListener?.getAccessToken() ?: ""
    }

    @JavascriptInterface
    fun viewFullRecord(lessonId: String) {
        Timber.tag("LeaderBoardWebInterface").d("viewFullRecord")
        leaderBoardListener?.viewFullRecord(lessonIdAdapter.fromJson(lessonId)?.lessonId ?: "")
    }

    @JavascriptInterface
    fun enterNextClass() {
        Timber.tag("LeaderBoardWebInterface").d("enterNextClass")
        leaderBoardListener?.enterNextClass()
    }

    interface LeaderBoardWebListener {
        fun getAccessToken(): String
        fun viewFullRecord(lessonId: String)
        fun enterNextClass()
    }
}