package com.viewsonic.designershell.adapter

import java.io.File

/** A widget/composable the user selected in the target app. */
data class SelectedNode(
    val desc: String,
    val file: String,
    val line: Int,
    val col: Int,
    val bounds: String = "",
    /** App-authored design-node id, when the selection came from a DesignNode. */
    val id: String? = null,
)

/** A switchable page/screen the target exposes. */
data class PageInfo(val id: String, val label: String)

/** A node in the target's structure tree (Figma-style hierarchy panel). */
data class TreeNode(
    val label: String,
    val file: String?,
    val line: Int,
    val id: String? = null, // target-specific selection handle (e.g. Flutter valueId)
    val x: Float = 0f,
    val y: Float = 0f,
    val w: Float = 0f,
    val h: Float = 0f,
    val children: List<TreeNode> = emptyList(),
)

/**
 * A pluggable connection to one running target app. The Designer Shell drives
 * every target through this interface; concrete adapters (Flutter via the Dart
 * VM Service, Android via adb + a debug bridge, …) hide the per-tech transport.
 *
 * All targets are hosted *out-of-process* — the shell never compiles against the
 * target, so one standalone shell can serve many independent app repos.
 */
interface TargetAdapter {
    /** Human label for the target (shown in the shell). */
    val displayName: String

    /** Directory Claude runs in / edits for this target. */
    val workingDir: File

    var onStatus: (String) -> Unit
    var onSelection: (SelectedNode) -> Unit

    /** Receives the target's structure tree after [requestTree]. */
    var onTree: (List<TreeNode>) -> Unit

    /** Receives the target's switchable pages after [requestPages]. */
    var onPages: (List<PageInfo>) -> Unit

    /** Receives an error / exception block detected in the target's output. */
    var onError: (String) -> Unit

    /** Launch (if needed) and connect to the target. */
    fun start()

    /** Toggle "design mode": taps select instead of (only) acting. */
    fun setDesignMode(on: Boolean)

    /** Ask the target for its full (framework) structure tree (answered via [onTree]). */
    fun requestTree()

    /**
     * Ask the target for its clean, app-authored design-node tree (answered via
     * [onTree]). Targets without design nodes may return an empty tree.
     */
    fun requestDesignTree()

    /** Select a node picked from the structure tree (highlights it in the target). */
    fun selectNode(node: TreeNode)

    /** Ask the target for its switchable pages (answered via [onPages]). */
    fun requestPages()

    /** Switch the target to the given page. */
    fun setPage(id: String)

    /** Apply the latest source edits to the running target (hot reload). */
    fun hotReload()

    /**
     * Restart the running target from scratch, re-running its entry point and
     * dropping app state (hot restart). Heavier than [hotReload].
     */
    fun hotRestart()

    fun stop()
}
