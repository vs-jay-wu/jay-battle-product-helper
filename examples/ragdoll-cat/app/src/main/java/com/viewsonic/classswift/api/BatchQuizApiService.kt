package com.viewsonic.classswift.api

import com.viewsonic.classswift.api.body.CreateBatchQuizBody
import com.viewsonic.classswift.api.body.UpdateQuizStatusBody
import com.viewsonic.classswift.api.response.CreateBatchQuizResponse
import com.viewsonic.classswift.api.response.GetBatchQuizResultDetailResponse
import com.viewsonic.classswift.api.response.GetBatchQuizSummaryResponse
import com.viewsonic.classswift.api.response.GetLatestBatchQuizResponse
import com.viewsonic.classswift.api.response.UpdateBatchQuizStatusResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import org.json.JSONObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface BatchQuizApiService {

    @POST("/api/v3/lessons/{lesson_id}/quizzes/batch_quizzes")
    suspend fun createBatchQuiz(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Body body: CreateBatchQuizBody
    ): ApiResponse<CreateBatchQuizResponse>

    @PUT("/api/v3/lessons/{lesson_id}/quizzes/batch_quizzes/{batch_quizzes_id}")
    suspend fun updateBatchQuizStatus(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Path("batch_quizzes_id") batchQuizzesId: String,
        @Body body: UpdateQuizStatusBody
    ): ApiResponse<UpdateBatchQuizStatusResponse>

    @PUT("/api/v3/lessons/{lesson_id}/quizzes/batch_quizzes/{batch_quizzes_id}/disclose")
    suspend fun discloseQuiz(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Path("batch_quizzes_id") batchQuizzesId: String,
    ): ApiResponse<JSONObject>

    @GET("/api/v3/lessons/{lesson_id}/quizzes/batch_quizzes/{batch_quizzes_id}/summary")
    suspend fun getBatchQuizSummary(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Path("batch_quizzes_id") batchQuizzesId: String,
    ): ApiResponse<GetBatchQuizSummaryResponse>

    @GET("/api/v3/lessons/{lesson_id}/quizzes/batch_quizzes/latest")
    suspend fun getLatestBatchQuiz(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String
    ): ApiResponse<GetLatestBatchQuizResponse>

    @GET("/api/v3/lessons/{lesson_id}/quiz_records/{quiz_id}")
    suspend fun getBatchQuizResultDetailInfo(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Path("quiz_id") quizId: String,
    ): ApiResponse<GetBatchQuizResultDetailResponse>

}