package com.viewsonic.classswift.ui.windowmodel.tool.spinner

sealed class SpinnerUiEvent {
    data class NetworkStatusChange(
        val isNetworkConnected: Boolean
    ) : SpinnerUiEvent()
}