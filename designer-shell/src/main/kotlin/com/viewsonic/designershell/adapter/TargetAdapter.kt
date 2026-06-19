package com.viewsonic.designershell.adapter

import java.io.File

/** A widget/composable the user selected in the target app. */
data class SelectedNode(
    val desc: String,
    val file: String,
    val line: Int,
    val col: Int,
    val bounds: String = "",
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

    /** Launch (if needed) and connect to the target. */
    fun start()

    /** Toggle "design mode": taps select instead of (only) acting. */
    fun setDesignMode(on: Boolean)

    /** Apply the latest source edits to the running target (hot reload). */
    fun hotReload()

    fun stop()
}
