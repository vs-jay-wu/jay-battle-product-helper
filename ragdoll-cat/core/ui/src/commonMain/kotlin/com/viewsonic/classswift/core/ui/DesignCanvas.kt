package com.viewsonic.classswift.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.viewsonic.classswift.core.designsystem.AppTheme
import kotlin.math.roundToInt

/**
 * Hosts a screen as a selectable canvas. In design mode, a transparent top layer intercepts
 * taps (so the screen's own clicks don't fire — like Figma's non-interactive canvas), hit-tests
 * against the [DesignNodeRegistry], and draws a selection outline + dimension label over the hit.
 *
 * All in-process: selection is a direct callback, no IPC (see docs/desktop-app-architecture.md).
 */
@Composable
fun DesignCanvas(
    designMode: Boolean,
    selectedId: String?,
    onSelect: (DesignNodeInfo?) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val registry = remember { DesignNodeRegistry() }
    var host by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(modifier.onGloballyPositioned { host = it }) {
        CompositionLocalProvider(LocalDesignNodeRegistry provides registry) {
            content()
        }

        if (designMode) {
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            host?.let { onSelect(registry.hitTest(offset, it)) }
                        }
                    },
            ) {
                val currentHost = host
                val rect = if (selectedId != null && currentHost != null) {
                    registry.rectOf(selectedId, currentHost)
                } else {
                    null
                }
                if (rect != null) SelectionOverlay(rect)
            }
        }
    }
}

@Composable
private fun BoxScope.SelectionOverlay(rect: Rect) {
    val color = AppTheme.tokens.colors.primary
    val density = LocalDensity.current

    Canvas(Modifier.matchParentSize()) {
        drawRect(
            color = color,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(width = 2.dp.toPx()),
        )
    }

    val widthDp = (rect.width / density.density).roundToInt()
    val heightDp = (rect.height / density.density).roundToInt()
    Box(
        Modifier
            .offset { IntOffset(rect.left.roundToInt(), (rect.top - 16.dp.toPx()).roundToInt()) }
            .background(color)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = "$widthDp × $heightDp",
            color = AppTheme.tokens.colors.neutral0,
            fontSize = AppTheme.tokens.type.xs,
        )
    }
}
