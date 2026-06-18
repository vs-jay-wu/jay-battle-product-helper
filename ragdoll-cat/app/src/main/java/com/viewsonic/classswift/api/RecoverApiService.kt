package com.viewsonic.classswift.api

import com.viewsonic.classswift.api.response.BuzzerResultResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

// for recovery class status relative api when app crash or socket disconnect
interface RecoverApiService {
    @GET("/api/v3/lessons/{lesson_id}/student_buzzer")
    suspend fun getBuzzerResult(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String
    ): ApiResponse<BuzzerResultResponse>
}