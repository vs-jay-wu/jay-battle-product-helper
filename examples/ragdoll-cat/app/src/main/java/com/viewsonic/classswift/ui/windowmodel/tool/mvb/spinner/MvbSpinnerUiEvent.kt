package com.viewsonic.classswift.ui.windowmodel.tool.mvb.spinner

sealed class MvbSpinnerUiEvent {
    data class NetworkStatusChange(
        val isNetworkConnected: Boolean
    ) : MvbSpinnerUiEvent()
}
