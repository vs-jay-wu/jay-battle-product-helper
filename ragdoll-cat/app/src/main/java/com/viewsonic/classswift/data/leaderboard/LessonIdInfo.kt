package com.viewsonic.classswift.data.leaderboard

import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.data.spinner.CandidateStudentInfo

@JsonClass(generateAdapter = true)
data class LessonIdInfo(
    val lessonId: String
)