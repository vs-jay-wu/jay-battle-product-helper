package com.viewsonic.classswift.handler

import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

class TimerHandler {
    private val _timerEventFlow = MutableSharedFlow<Long>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val timerEventFlow = _timerEventFlow.asSharedFlow()

    private var tickerScope: CoroutineScope? = null

    fun startTimer(createAtSeconds: Long) {
        Timber.d("start timer : createAt = $createAtSeconds")
        stopTimer()
        val timeDiffInSeconds = TimeUtils.getTimeDiffFromCurrentTimeInMillis(createAtSeconds * 1000) / 1000

        tickerScope = CoroutineManager.getScope(this)
        tickerScope?.launch {
            val tickerChannel = ticker(delayMillis = 1000, initialDelayMillis = 0)

            for (unit in tickerChannel) {
                if (!isActive) break
                val currentTimeSeconds = System.currentTimeMillis() / 1000
                val elapsedTime = currentTimeSeconds - createAtSeconds + timeDiffInSeconds

                _timerEventFlow.tryEmit(elapsedTime)
            }
        }
    }

    fun stopTimer() {
        tickerScope?.cancel()
        CoroutineManager.cancelScope(this)
        tickerScope = null
        Timber.d("timer stopped")
    }
}
