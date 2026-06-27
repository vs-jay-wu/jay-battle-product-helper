package com.viewsonic.classswift.utils.extension

import android.os.SystemClock
import com.viewsonic.classswift.constant.AppConstants.ONE_SEC_DELAY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


fun CoroutineScope.countdownInSeconds(
    durationInSeconds: Long,
    onTick: suspend (tickSecond: Long, remainingMillis: Long) -> Unit,
    onMiss: (missedSeconds: List<Long>) -> Unit = {},
    onFinish: () -> Unit
): Job {
    return launch(Dispatchers.Main) {
        val startTime = SystemClock.elapsedRealtime()
        val endTime = startTime + (durationInSeconds * ONE_SEC_DELAY)
        var tickIndex = 0L
        var lastTickIndex = 0L

        while (isActive) {
            val now = SystemClock.elapsedRealtime()
            val remaining = endTime - now

            if (remaining <= 0L) {
                onFinish()
                break
            } else {
                tickIndex = ((now - startTime) / ONE_SEC_DELAY)

                val missed = (lastTickIndex + 1 until tickIndex)
                    .map { missedIndex -> durationInSeconds - missedIndex }
                    .filter { it > 0 }

                if (missed.isNotEmpty()) {
                    onMiss(missed)
                }

                lastTickIndex = tickIndex
                val reversedTickSecond = (durationInSeconds - tickIndex).coerceAtLeast(1L)
                val currentRemaining = endTime - SystemClock.elapsedRealtime()

                onTick(reversedTickSecond, currentRemaining)

                val nextTickTime = startTime + (tickIndex + 1) * ONE_SEC_DELAY
                val delayTime = nextTickTime - SystemClock.elapsedRealtime()
                if (delayTime > 0) {
                    withContext(Dispatchers.IO) {
                        delay(delayTime)
                    }
                }
            }
        }
    }
}

fun CoroutineScope.startTimerInMilliSec(startTimeInMillis: Long, timeDiffInMillis: Long, onTick: suspend (tickMilliSecond: Long) -> Unit): Job {
    return launch(Dispatchers.IO) {
        try {
            while (true) {
                delay(100)
                val elapsedTime = System.currentTimeMillis() - startTimeInMillis + timeDiffInMillis
                onTick(elapsedTime)
            }
        } catch (e: Exception) {
           Timber.d("startTimerInSec exception: ${e.message}")
        }
    }
}

fun CoroutineScope.repeatWithDelay(
    intervalMillis: Long,
    suspendAction: suspend () -> Unit
): Job {
    return launch(Dispatchers.IO) {
        try {
            while (true) {
                suspendAction()
                delay(intervalMillis)
            }
        } catch (e: Exception) {
            Timber.d("repeatWithDelay exception: ${e.message}")
        }
    }
}