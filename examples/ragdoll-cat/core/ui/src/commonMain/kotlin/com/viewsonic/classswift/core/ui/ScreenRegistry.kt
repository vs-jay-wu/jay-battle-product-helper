package com.viewsonic.classswift.core.ui

import androidx.compose.runtime.Composable

/**
 * A single previewable state of a screen (loading / empty / error / populated …).
 */
class ScreenState(
    val label: String,
    val content: @Composable () -> Unit,
)

/**
 * A screen plus all the states the designer can flip between in the canvas.
 *
 * Keeping the registry app-agnostic is what lets the Designer Shell be reused across
 * Compose apps (see docs/desktop-app-architecture.md §3): each app supplies its own
 * list of [ScreenSpec]s; the shell renders whatever it's given.
 */
class ScreenSpec(
    val name: String,
    val states: List<ScreenState>,
)
