package com.viewsonic.classswift.ui.widgetmodel.records.state

import com.viewsonic.classswift.api.response.UpdateTaskResult

sealed class MarkUpdateState {

    data object Idle : MarkUpdateState()

    data class SingleMarkSuccess(
        val id: String,
        val success: UpdateTaskResult?,
        val failed: UpdateTaskResult?
    ) : MarkUpdateState()

    data class MultiMarkSuccess(
        val id: String,
        val success: List<UpdateTaskResult>,
        val failed: List<UpdateTaskResult>
    ) : MarkUpdateState()

    data class Failed(
        val id: String,
        val errorMessage: String,
        val isMultiMark: Boolean
    ) : MarkUpdateState()
}