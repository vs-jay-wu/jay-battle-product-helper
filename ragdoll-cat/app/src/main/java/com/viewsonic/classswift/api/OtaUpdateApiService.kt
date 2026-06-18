package com.viewsonic.classswift.api

import com.viewsonic.classswift.api.response.OtaVersionInfoResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface OtaUpdateApiService {
    @GET("/{file_name}")
    @Streaming
    suspend fun downloadApkWithFileName(
        @Path("file_name") fileName: String,
    ): ApiResponse<ResponseBody>

    @GET("/{file_name}")
    suspend fun fetchVersionInfo(
        @Path("file_name") fileName: String,
    ): ApiResponse<OtaVersionInfoResponse>
}