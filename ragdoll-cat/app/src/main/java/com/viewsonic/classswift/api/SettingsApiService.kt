package com.viewsonic.classswift.api

import com.viewsonic.classswift.api.response.UserPreferenceResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface SettingsApiService {
    @GET("/api/v3/organization/{org_id}/user_preference")
    suspend fun getUserPreference(@Header("Authorization") token: String, @Path("org_id") orgId: String): ApiResponse<UserPreferenceResponse>

    @PUT("/api/v3/organization/{org_id}/user_preference")
    suspend fun setUserPreference(
        @Header("Authorization") token: String,
        @Path("org_id") orgId: String,
        @Query("preference_type") type: String,
        @Body body: RequestBody
    ): ApiResponse<UserPreferenceResponse>
}