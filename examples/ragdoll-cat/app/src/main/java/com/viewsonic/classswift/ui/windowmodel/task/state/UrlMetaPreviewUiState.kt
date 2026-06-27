package com.viewsonic.classswift.ui.windowmodel.task.state

import com.viewsonic.classswift.data.task.UrlPreviewInfo

sealed class UrlMetaPreviewUiState {
    data object Idle : UrlMetaPreviewUiState()
    data class UrlMetaPreviewUpdate(val data: UrlPreviewInfo) : UrlMetaPreviewUiState()
    data object UrlMetaFetchFail : UrlMetaPreviewUiState()
}