package com.viewsonic.classswift.data.task

data class HasTaskInProgressInfo(
    val hasTaskInProgress: Boolean = false,
    val isUnexpectedState: Boolean = false
)
