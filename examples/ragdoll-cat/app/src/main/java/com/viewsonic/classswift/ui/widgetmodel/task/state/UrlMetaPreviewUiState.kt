package com.viewsonic.classswift.ui.widgetmodel.task.state

import com.viewsonic.classswift.data.task.UrlPreviewInfo

sealed class UrlMetaPreviewUiState {
    data object Idle : UrlMetaPreviewUiState()
    data class UrlMetaPreviewUpdate(val data: UrlPreviewInfo) : UrlMetaPreviewUiState()
    data class UrlMetaFetchFail(val data: UrlPreviewInfo) : UrlMetaPreviewUiState()
}