package com.viewsonic.classswift.api.body

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateQuizStatusBody(
    val status: UpdateQuizStatusType
)


enum class UpdateQuizStatusType {
    CANCEL, //Quizzing Close Window -> update status to CANCEL
    FINISH,  //Quizzing Click End Quiz -> update quiz api to FINISH status
    CLOSE  //Quiz Result Close Window -> update status to CLOSE
}