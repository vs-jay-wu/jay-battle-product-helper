package com.viewsonic.classswift.ui.widget.stopwatch

import android.os.SystemClock
import kotlin.jvm.Volatile

/**
 * Simple StopwatchTool for tracking elapsed time without drift.
 *
 * This class uses [SystemClock.elapsedRealtime] (time since boot, including sleep) to
 * accumulate elapsed time, which makes it robust against system clock changes and
 * avoids drift that can occur when repeatedly adding small deltas from scheduled
 * callbacks or animations.
 *
 * Typical usage:
 * - Call [start] to begin measuring time. If already running, additional calls are ignored.
 * - Call [pause] to stop measuring and accumulate the elapsed time so far.
 * - Call [reset] to clear any accumulated time and return to the initial state.
 * - Call [elapsedMs] at any time to obtain the total elapsed time in milliseconds.
 * - Inspect [isRunning] to check whether the stopwatch is currently running.
 */

class StopwatchTool {

    @Volatile
    private var accumulatedMs: Long = 0L // total elapsed time in milliseconds
    @Volatile
    private var startRealtimeMs: Long? = null // start time in elapsed realtime milliseconds

    val isRunning: Boolean
        get() = startRealtimeMs != null

    fun start() {
        if (startRealtimeMs != null) return
        startRealtimeMs = SystemClock.elapsedRealtime()
    }

    fun pause() {
        val start = startRealtimeMs ?: return
        val now = SystemClock.elapsedRealtime()
        accumulatedMs += (now - start)
        startRealtimeMs = null
    }

    fun reset() {
        accumulatedMs = 0L
        startRealtimeMs = null
    }

    fun elapsedMs(): Long {
        val start = startRealtimeMs
        val now = SystemClock.elapsedRealtime()
        return if (start == null) accumulatedMs else accumulatedMs + (now - start)
    }
}