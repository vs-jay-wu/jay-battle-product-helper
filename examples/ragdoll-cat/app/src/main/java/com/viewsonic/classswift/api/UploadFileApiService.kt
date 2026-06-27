package com.viewsonic.classswift.api

import com.viewsonic.classswift.api.body.PreSignedUrlBody
import com.viewsonic.classswift.api.response.PreSignedUrlResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface UploadFileApiService {
    @POST("/api/v3/lessons/{lesson_id}/resources")
    suspend fun getPreSignedUrl(
        @Header("Authorization") token: String,
        @Path("lesson_id") lessonId: String,
        @Body body: PreSignedUrlBody = PreSignedUrlBody()
    ): ApiResponse<PreSignedUrlResponse>
}