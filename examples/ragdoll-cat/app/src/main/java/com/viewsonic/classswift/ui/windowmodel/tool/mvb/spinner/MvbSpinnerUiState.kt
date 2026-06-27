package com.viewsonic.classswift.ui.windowmodel.tool.mvb.spinner

sealed class MvbSpinnerUiState {
    data object Loading : MvbSpinnerUiState()
    data object Ready : MvbSpinnerUiState()
}
