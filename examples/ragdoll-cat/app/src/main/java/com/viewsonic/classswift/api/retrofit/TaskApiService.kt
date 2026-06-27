package com.viewsonic.classswift.api.retrofit

import com.viewsonic.classswift.api.body.BatchCreateTasksBody
import com.viewsonic.classswift.api.body.CreateTaskBody
import com.viewsonic.classswift.api.body.UpdateTaskResultBody
import com.viewsonic.classswift.api.response.BatchCreateTasksResponse
import com.viewsonic.classswift.api.response.BatchTasksLatestResponse
import com.viewsonic.classswift.api.response.CloseBatchTaskResponse
import com.viewsonic.classswift.api.response.CloseTaskResponse
import com.viewsonic.classswift.api.response.CreateTaskResponse
import com.viewsonic.classswift.api.response.GetLinkPreviewResponse
import com.viewsonic.classswift.api.response.GetStudentTaskResponse
import com.viewsonic.classswift.api.response.GetTaskByIdResponse
import com.viewsonic.classswift.api.response.GetTaskRecordsByLessonResponse
import com.viewsonic.classswift.api.response.GetTasksResponse
import com.viewsonic.classswift.api.response.UpdateTaskResultResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TaskApiService {
    @POST("/api/v3/lessons/{lesson_id}/tasks")
    suspend fun createTask(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Body body: CreateTaskBody
    ): ApiResponse<CreateTaskResponse>

    @POST("/api/v3/lessons/{lesson_id}/tasks/batch_tasks")
    suspend fun batchCreateTasks(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Body body: BatchCreateTasksBody
    ): ApiResponse<BatchCreateTasksResponse>

    @GET("/api/v3/lessons/{lesson_id}/tasks")
    suspend fun getTask(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Query(value = "filter", encoded = true) filter: String = ""
    ): ApiResponse<GetTasksResponse>

    @GET("/api/v3/lessons/{lesson_id}/student_tasks")
    suspend fun getTaskRecordsByLesson(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String
    ): ApiResponse<GetTaskRecordsByLessonResponse>

    @GET("/api/v3/lessons/{lesson_id}/tasks/{task_id}")
    suspend fun getTaskById(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Path("task_id") taskId: String,
        @Query("student_status") studentStatus: Boolean = true
    ): ApiResponse<GetTaskByIdResponse>

    @GET("/api/v3/lessons/{lesson_id}/students/{student_id}/tasks")
    suspend fun getStudentTasks(
        @Path("lesson_id") lessonId: String,
        @Path("student_id") studentId: String,
        @Query("task_id") taskId: String
    ): ApiResponse<List<GetStudentTaskResponse>>

    @GET("/api/v3/tools/link_preview")
    suspend fun fetchUrlMeta(
        @Header("Authorization") token: String,
        @Query("url",encoded = true) url: String = "",
    ): ApiResponse<GetLinkPreviewResponse>

    @PUT("/api/v3/tasks/{task_id}")
    suspend fun closeTask(
        @Header("Authorization") token: String,
        @Path("task_id") taskId: String
    ): ApiResponse<CloseTaskResponse>

    @POST("/api/v3/tasks/{task_id}/task_results")
    suspend fun updateTaskResult(
        @Header("Authorization") token: String,
        @Path("task_id") taskId: String,
        @Body body: List<UpdateTaskResultBody>
    ): ApiResponse<UpdateTaskResultResponse>

    @GET("/api/v3/lessons/{lesson_id}/tasks/batch_tasks/latest")
    suspend fun getBatchTasksLatest(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String
    ): ApiResponse<BatchTasksLatestResponse>

    /**
     * VSFT-8453/8454：關閉整批 batch task（Sketch Response 結果頁 [X] 關窗時呼叫）。
     * 取代舊的 task-by-task [closeTask]。batch_tasks 系列 API 由後端新增（dev/staging 已部署）。
     */
    @PUT("/api/v3/lessons/{lesson_id}/tasks/batch_tasks/{batch_task_id}")
    suspend fun closeBatchTask(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Path("batch_task_id") batchTaskId: String
    ): ApiResponse<CloseBatchTaskResponse>
}