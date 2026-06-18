package com.viewsonic.classswift.api

import com.viewsonic.classswift.api.response.AiSubjectDisplayNamesResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface AiApiService {

    @GET("/api/v3/ai/curriculum/subjects")
    suspend fun getSubjectDisplayNames(
        @Header("Authorization") token: String
    ): ApiResponse<AiSubjectDisplayNamesResponse>

}