package com.viewsonic.classswift.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Platform-neutral theme entry point for the Designer-to-Code preview.
 *
 * Phase 0: a thin wrapper over MaterialTheme. Design tokens (colors / spacing /
 * typography extracted from the existing XML resources) will be layered in here
 * during Phase 1 and exposed via CompositionLocals.
 */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
