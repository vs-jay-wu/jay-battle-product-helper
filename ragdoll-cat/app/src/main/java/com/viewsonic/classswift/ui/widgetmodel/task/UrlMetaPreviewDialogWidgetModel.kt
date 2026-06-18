package com.viewsonic.classswift.ui.widgetmodel.task

import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.coordinator.UrlMetaCoordinator
import com.viewsonic.classswift.data.task.UrlPreviewInfo
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widgetmodel.task.state.UrlMetaPreviewUiState
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class UrlMetaPreviewDialogWidgetModel(
    private val urlMetaCoordinator: UrlMetaCoordinator
) : IWindowModel {

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val _uiStateFlow = MutableStateFlow<UrlMetaPreviewUiState>(UrlMetaPreviewUiState.Idle)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    fun fetchUrlMeta(url: String) {

        if (url.isEmpty()) return

        coroutineScope.launch {

            val response = urlMetaCoordinator.fetchUrlMeta(url = url)
            Timber.d("fetchUrlMeta response: ${response.toString()}")

            when (response) {
                is ApiResponse.Success -> {
                    val responseData = response.data
                    responseData.let {
                        val data = UrlPreviewInfo(
                            title = it.title,
                            description = it.description,
                            imageUrl = it.imageUrl,
                            siteName = it.siteName,
                            url = url,
                            isValid = true
                        )
                        _uiStateFlow.emit(
                            UrlMetaPreviewUiState.UrlMetaPreviewUpdate(data = data)
                        )
                    }
                }

                is ApiResponse.Rfc7807Failure -> {
                    Timber.e("fetchUrlMeta error : ${response.error.toString()}")
                    val data = UrlPreviewInfo(
                        title = "",
                        description = "",
                        imageUrl = "",
                        siteName = "",
                        url = url,
                        isValid = false
                    )
                    _uiStateFlow.emit(UrlMetaPreviewUiState.UrlMetaFetchFail(data = data))
                }

                else -> {
                    val data = UrlPreviewInfo(
                        title = "",
                        description = "",
                        imageUrl = "",
                        siteName = "",
                        url = url,
                        isValid = false
                    )
                    _uiStateFlow.emit(UrlMetaPreviewUiState.UrlMetaFetchFail(data = data))
                }
            }
        }
    }

    override fun onCleared() {
        coroutineScope.cancel()
    }
}