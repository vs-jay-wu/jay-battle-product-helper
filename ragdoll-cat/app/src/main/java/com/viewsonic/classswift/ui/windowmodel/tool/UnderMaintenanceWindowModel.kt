package com.viewsonic.classswift.ui.windowmodel.tool

import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.windowmodel.ToolbarManager
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class UnderMaintenanceWindowModel(
    private val toolbarManager: ToolbarManager
) : IWindowModel {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    override fun onCleared() {}

    fun checkIfNeedToEndLesson() {
        coroutineScope.launch {
            if (toolbarManager.toolbarUiState.value.participationState == ToolbarManager.ParticipationState.LESSON_STARTED) {
                toolbarManager.endLesson()
            }
        }
    }
}