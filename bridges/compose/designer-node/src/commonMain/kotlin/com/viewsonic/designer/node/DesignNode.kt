package com.viewsonic.designer.node

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics

/**
 * Stable, explicit design-node id used by the Designer Shell's design-mode inspector.
 *
 * Tag components with [designNode] (e.g. `Modifier.designNode("score_card")`) so the
 * inspector — and the AI agent receiving the selection — can refer to the exact element
 * by a name that survives rebuilds (unlike auto-generated indices).
 */
val DesignNodeIdKey = SemanticsPropertyKey<String>("DesignNodeId")

var SemanticsPropertyReceiver.designNodeId: String by DesignNodeIdKey

/** A selected node and its bounds (px), relative to the canvas host. */
data class DesignNodeInfo(val id: String, val rect: Rect)

/**
 * Collects the live bounds of every [designNode]-tagged element in the canvas.
 * Because the canvas runs in the shell's own process, this is plain in-process state —
 * no IPC. Bounds update on every layout pass (and after each hot reload).
 */
class DesignNodeRegistry {
    private val nodes = mutableStateMapOf<String, LayoutCoordinates>()

    fun report(id: String, coordinates: LayoutCoordinates) {
        nodes[id] = coordinates
    }

    fun remove(id: String) {
        nodes.remove(id)
    }

    /** Bounds of [id] relative to [host], or null if not laid out. */
    fun rectOf(id: String, host: LayoutCoordinates): Rect? {
        val coords = nodes[id] ?: return null
        if (!coords.isAttached || !host.isAttached) return null
        return host.localBoundingBoxOf(coords, clipBounds = false)
    }

    /** All tagged nodes containing [point], outermost (largest) first — for Figma-style
     *  click-to-drill: repeated taps at the same spot step inward through this stack. */
    fun hitStack(point: Offset, host: LayoutCoordinates): List<DesignNodeInfo> {
        if (!host.isAttached) return emptyList()
        val out = mutableListOf<DesignNodeInfo>()
        nodes.forEach { (id, coords) ->
            if (!coords.isAttached) return@forEach
            val rect = host.localBoundingBoxOf(coords, clipBounds = false)
            if (rect.contains(point)) out.add(DesignNodeInfo(id, rect))
        }
        return out.sortedByDescending { it.rect.width * it.rect.height }
    }

    /** The smallest (most specific) tagged node containing [point] in [host]'s space. */
    fun hitTest(point: Offset, host: LayoutCoordinates): DesignNodeInfo? {
        if (!host.isAttached) return null
        var best: DesignNodeInfo? = null
        var bestArea = Float.MAX_VALUE
        nodes.forEach { (id, coords) ->
            if (!coords.isAttached) return@forEach
            val rect = host.localBoundingBoxOf(coords, clipBounds = false)
            if (rect.contains(point)) {
                val area = rect.width * rect.height
                if (area < bestArea) {
                    bestArea = area
                    best = DesignNodeInfo(id, rect)
                }
            }
        }
        return best
    }
}

/** Provided by the design canvas / target host; null when not previewing in the shell. */
val LocalDesignNodeRegistry = compositionLocalOf<DesignNodeRegistry?> { null }

/**
 * Tags an element with a stable [id] (for AI/testing via semantics) and, when running
 * inside a design canvas, reports its live bounds to the registry for selection.
 */
fun Modifier.designNode(id: String): Modifier = this
    .semantics { designNodeId = id }
    .composed {
        val registry = LocalDesignNodeRegistry.current ?: return@composed Modifier
        DisposableEffect(id, registry) {
            onDispose { registry.remove(id) }
        }
        Modifier.onGloballyPositioned { registry.report(id, it) }
    }
