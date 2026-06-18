package com.viewsonic.classswift.api

import com.viewsonic.classswift.api.body.GetTokenBody
import com.viewsonic.classswift.api.response.TokenResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthorizationApiService {
    @POST("/api/v3/token")
    suspend fun getToken(@Body body : GetTokenBody):ApiResponse<TokenResponse>
}