package com.viewsonic.classswift.api

import com.viewsonic.classswift.api.response.OrganizationPermissionResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface SelectOrgApiService {
    @GET("/api/v3/organization/{org_id}/permissions")
    suspend fun getOrgPermissions(@Header("Authorization")token : String, @Path("org_id") orgId : String, @Query("user_id") userId: String): ApiResponse<OrganizationPermissionResponse>
}