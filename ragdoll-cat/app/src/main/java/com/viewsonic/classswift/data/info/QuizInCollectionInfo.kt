package com.viewsonic.classswift.data.info

import com.viewsonic.classswift.api.response.QuizzesInCollectionFolderResponse

data class QuizInCollectionInfo(
    val quizData: QuizzesInCollectionFolderResponse.QuizInCollectionData = QuizzesInCollectionFolderResponse.QuizInCollectionData(),
    val subjectDisplayName: String = "",
) {
    fun isTextQuiz() = quizData.imgUrl.isBlank()

    /** Mirrors WPF Quiz.standards.Count → "{n} standards" gating. */
    val standardsCount: Int
        get() = quizData.standards.size
}
