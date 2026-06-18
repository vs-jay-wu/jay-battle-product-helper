package com.viewsonic.classswift.data.info

import com.viewsonic.classswift.data.quiz.QuizType

data class BatchQuizSummaryInfo(
    val collectionId: String = "",
    val quizId: String = "",
    val sequence: Int = 1,
    val content: String ="",
    val quizType: QuizType = QuizType.UNSPECIFIED,
    val correctStudentIds: List<String> = emptyList(),
    val incorrectStudentIds: List<String> = emptyList(),
    val noAnswerStudentIds: List<String> = emptyList(),
) {
    val submittedStudentCount: Int
        get() = correctStudentIds.size + incorrectStudentIds.size + noAnswerStudentIds.size
    val accuracyRate: Float
        get() = if (submittedStudentCount != 0) {
            correctStudentIds.size / submittedStudentCount.toFloat()
        } else {
            0f
        }
}