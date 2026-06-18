package com.viewsonic.classswift.api

import com.viewsonic.classswift.api.body.BatchUpdateStudentPointBody
import com.viewsonic.classswift.api.body.BatchingUpdateStudentPointsBody
import com.viewsonic.classswift.api.body.StartLessonBody
import com.viewsonic.classswift.api.body.UpdateMultipleStudentPointsBody
import com.viewsonic.classswift.api.body.UpdateStudentPointBody
import com.viewsonic.classswift.api.body.UpdateStudentPointsBody
import com.viewsonic.classswift.api.response.GetUnclosedMissionsResponse
import com.viewsonic.classswift.api.response.LessonResponse
import com.viewsonic.classswift.api.response.StartLessonResponse
import com.viewsonic.classswift.api.response.StudentListResponse
import com.viewsonic.classswift.api.response.UpdateAllStudentsPointResponse
import com.viewsonic.classswift.api.response.UpdateStudentPointResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import org.json.JSONObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface LessonApiService {
    @PUT("/api/v3/lessons/{lesson_id}/time")
    suspend fun startLesson(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Body body: StartLessonBody = StartLessonBody()
    ): ApiResponse<StartLessonResponse>

    @POST("/api/v3/lessons/{lesson_id}/end")
    suspend fun endLesson(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
    ): ApiResponse<JSONObject>

    @GET("/api/v3/lessons/{lesson_id}")
    suspend fun getLessonInfo(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
    ): ApiResponse<LessonResponse>

    @GET("/api/v3/lessons/{lesson_id}/attendant_list")
    suspend fun getStudentList(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Query("group") group: Boolean = true,
        @Query("occupied_only") occupiedOnly: Boolean = false
    ): ApiResponse<StudentListResponse>

    @GET("/api/v3/lessons/{lesson_id}/unclosed_missions")
    suspend fun getUnclosedMissions(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String
    ): ApiResponse<GetUnclosedMissionsResponse>

    @PUT("/api/v3/lessons/{lesson_id}/points")
    suspend fun updateAllStudentsPoint(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Body body: UpdateStudentPointBody = UpdateStudentPointBody()
    ): ApiResponse<UpdateAllStudentsPointResponse>

    @PUT("/api/v3/lessons/{lesson_id}/attendant/{student_id}/points")
    suspend fun updateStudentPoint(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Path("student_id") studentId: String,
        @Body body: UpdateStudentPointsBody = UpdateStudentPointsBody()
    ): ApiResponse<UpdateStudentPointResponse>

    @PUT("/api/v3/lessons/{lesson_id}/batch_points")
    suspend fun batchUpdateStudentPoint(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Body body: BatchUpdateStudentPointBody = BatchUpdateStudentPointBody()
    ): ApiResponse<UpdateStudentPointResponse>

    @PUT("/api/v3/lessons/{lesson_id}/attendant_points")
    suspend fun updateMultipleStudentsPoint(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Body body: UpdateMultipleStudentPointsBody = UpdateMultipleStudentPointsBody()
    ): ApiResponse<JSONObject>

    @DELETE("/api/v3/lessons/{lesson_id}/attendant/{student_id}")
    suspend fun removeStudent(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Path("student_id") studentId: String,
        @Query("role") role: String = "teacher"
    ): ApiResponse<JSONObject>
}