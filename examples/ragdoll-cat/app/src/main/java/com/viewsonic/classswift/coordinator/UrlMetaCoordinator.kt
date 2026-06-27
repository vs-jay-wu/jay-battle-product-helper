package com.viewsonic.classswift.coordinator

import com.viewsonic.classswift.api.response.GetLinkPreviewResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.api.retrofit.TaskApiService
import com.viewsonic.classswift.manager.AccountManager

class UrlMetaCoordinator(
    private var taskApiService: TaskApiService,
    private val accountManager: AccountManager
) {

    suspend fun fetchUrlMeta(url: String): ApiResponse<GetLinkPreviewResponse> {
        return taskApiService.fetchUrlMeta(
            token = accountManager.getBearerToken(),
            url = url
        )
    }
}