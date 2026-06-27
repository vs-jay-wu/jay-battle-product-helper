package com.viewsonic.classswift.manager

import com.viewsonic.classswift.data.enum.MvbToolbarPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks the current mVB main toolbar position and (optionally) the mVB
 * whiteboard top edge position.
 *
 * Updated by [com.viewsonic.classswift.data.clientapp.myviewboard.message.MyViewBoardMessageHandler]
 * when a `MessageToolbarPositionChanged` IPC arrives. Observed by floating-window
 * components (e.g., Join Class) to align with the whiteboard region.
 *
 * Backwards compatibility: older mVB builds that don't carry `whiteboard_top_dp`
 * leave [whiteboardTopDp] at its last known value (or `null` if never reported);
 * observers fall back to a hardcoded default in that case.
 */
class MvbToolbarStateManager {

    private val _position = MutableStateFlow<MvbToolbarPosition?>(null)

    /** Latest known toolbar position; `null` if mVB has not yet reported one. */
    val position: StateFlow<MvbToolbarPosition?> = _position.asStateFlow()

    private val _whiteboardTopDp = MutableStateFlow<Double?>(null)

    /** Latest known whiteboard top offset (dp). `null` if mVB has not reported it. */
    val whiteboardTopDp: StateFlow<Double?> = _whiteboardTopDp.asStateFlow()

    /**
     * Records the latest toolbar position and (optional) whiteboard top edge.
     * A `null` [newWhiteboardTopDp] keeps the previously known value so callers
     * that don't (yet) carry the field don't accidentally reset state to null.
     */
    fun update(newPosition: MvbToolbarPosition, newWhiteboardTopDp: Double? = null) {
        _position.value = newPosition
        if (newWhiteboardTopDp != null) {
            _whiteboardTopDp.value = newWhiteboardTopDp
        }
    }
}
