package com.viewsonic.classswift.api.response.data

sealed class QuizAnswerData {
    data class Text(val content: String) : QuizAnswerData()
    data class Numbers(val list: List<Int>) : QuizAnswerData()
}