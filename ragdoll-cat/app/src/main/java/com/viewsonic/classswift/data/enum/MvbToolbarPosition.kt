package com.viewsonic.classswift.data.enum

/**
 * Represents the current position of the myViewBoard main toolbar.
 *
 * Sourced from `MessageToolbarPositionChanged` IPC payload. ClassSwift uses this to lay out
 * floating windows (e.g., Join Class) on the opposite side of the toolbar.
 */
enum class MvbToolbarPosition {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT;

    companion object {
        /** Parse the raw IPC payload string, returning `null` for unknown values. */
        fun fromIpcValue(value: String?): MvbToolbarPosition? {
            return runCatching { valueOf(value.orEmpty().trim().uppercase()) }.getOrNull()
        }
    }
}
