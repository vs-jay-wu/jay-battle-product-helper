package com.viewsonic.classswift.api

import com.viewsonic.classswift.api.body.CreateQuizBody
import com.viewsonic.classswift.api.body.DiscloseQuizBody
import com.viewsonic.classswift.api.body.UpdateQuizStatusBody
import com.viewsonic.classswift.api.response.CreateQuizResponse
import com.viewsonic.classswift.api.response.UpdateQuizStatusResponse
import com.viewsonic.classswift.api.body.PreSignedUrlBody
import com.viewsonic.classswift.api.response.DiscloseQuizResponse
import com.viewsonic.classswift.api.response.PreSignedUrlResponse
import com.viewsonic.classswift.api.response.UnclosedQuizResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface QuizApiService {

    @POST("/api/v3/lessons/{lesson_id}/resources")
    suspend fun getPreSignedUrl(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Body body: PreSignedUrlBody = PreSignedUrlBody()
    ): ApiResponse<PreSignedUrlResponse>

    @POST("/api/v3/lessons/{lesson_id}/quizzes")
    suspend fun createQuiz(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Body body: CreateQuizBody
    ): ApiResponse<CreateQuizResponse>

    @PUT("/api/v3/quizzes/{quiz_id}")
    suspend fun updateQuizStatus(
        @Header("Authorization") token: String,
        @Path("quiz_id") quizId: String,
        @Body body: UpdateQuizStatusBody
    ): ApiResponse<UpdateQuizStatusResponse>

    @PUT("/api/v3/quizzes/{quiz_id}/disclose")
    suspend fun discloseQuiz(
        @Header("Authorization") token: String,
        @Path("quiz_id") quizId: String,
        @Body body: DiscloseQuizBody
    ): ApiResponse<DiscloseQuizResponse>

    @GET("/api/v3/lessons/{lesson_id}/unclosed_quiz")
    suspend fun unclosedQuiz(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String
    ): ApiResponse<UnclosedQuizResponse>


}