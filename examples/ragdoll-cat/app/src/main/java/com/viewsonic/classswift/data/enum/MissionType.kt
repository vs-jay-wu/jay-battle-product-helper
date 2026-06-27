package com.viewsonic.classswift.data.enum

enum class MissionType(val serverValue: String) {
    NONE(""),
    QUIZ("quiz"),
    BATCH_QUIZZES("batch_quizzes"),
    PUSH_AND_RESPOND_TASK("task");

    companion object {
        fun valueOfServerValue(serverValue: String): MissionType {
            return when (serverValue) {
                QUIZ.serverValue -> QUIZ
                BATCH_QUIZZES.serverValue -> BATCH_QUIZZES
                PUSH_AND_RESPOND_TASK.serverValue -> PUSH_AND_RESPOND_TASK
                else -> NONE
            }
        }
    }
}
