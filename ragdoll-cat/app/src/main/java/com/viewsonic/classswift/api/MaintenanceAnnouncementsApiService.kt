package com.viewsonic.classswift.api

import com.viewsonic.classswift.api.response.MaintenanceAnnouncementsResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface MaintenanceAnnouncementsApiService {
    @GET("/{file_name}")
    suspend fun fetchMaintenanceAnnouncements(
        @Path("file_name") fileName: String,
    ): ApiResponse<MaintenanceAnnouncementsResponse>
}