package com.viewsonic.classswift.ui.windowmodel.tool

import com.viewsonic.classswift.constant.AppConstants.STOPWATCH_INTERVAL_TIME
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.stopwatch.StopwatchTool
import com.viewsonic.classswift.utils.extension.millionSecondToStopwatch
import com.viewsonic.classswift.utils.extension.countdownInSeconds
import com.viewsonic.classswift.utils.extension.secondToTimeNoColon
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class TimerToolWindowModel: IWindowModel {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val _timeSharedFlow = MutableSharedFlow<String>()
    val timeSharedFlow = _timeSharedFlow.asSharedFlow()
    private val _soundPlaySharedFlow = MutableSharedFlow<String>()
    val soundPlaySharedFlow = _soundPlaySharedFlow.asSharedFlow()
    private var stopwatchJob: Job? = null
    private var timerJob: Job? = null
    private var stopwatch: StopwatchTool? = null

    companion object {
        const val SOUND_TIK = "Tik"
        const val SOUND_DING = "Ding"
    }

    /**
     * timer 的 開始
     * @param time 格式是4個數字 e.g. 1234 - 倒數 12分34秒
     */
    fun timerStart(time: String) {
        val minute = time.substring(0, 2).toLong()
        val second = time.substring(2, 4).toLong()
        val totalSeconds = minute * 60 + second

        timerJob?.cancel()
        timerJob = coroutineScope.countdownInSeconds(
            durationInSeconds = totalSeconds,
            onTick = { tickSecond, _ ->
                coroutineScope.launch(Dispatchers.IO) {
                    _timeSharedFlow.emit(tickSecond.secondToTimeNoColon())
                    if (tickSecond < 6) {
                        _soundPlaySharedFlow.emit(SOUND_TIK)
                    }
                }
            },
            onMiss = {},
            onFinish = {
                coroutineScope.launch(Dispatchers.IO) {
                    _timeSharedFlow.emit("finish")
                    _soundPlaySharedFlow.emit(SOUND_DING)
                }
            }
        )
    }

    fun timerStop() {
        timerJob?.cancel()
    }

    fun stopwatchStart() {
        stopwatchJob?.cancel()
        stopwatch = StopwatchTool()
        ensureTicker()
    }

    fun stopwatchContinue() {
        stopwatch?.start()
        ensureTicker()
    }

    fun stopwatchStop() {
        stopwatch?.pause()
    }

    fun stopwatchReset() {
        stopwatchJob?.cancel()
        stopwatch?.reset()
        coroutineScope.launch(Dispatchers.IO) {
            _timeSharedFlow.emit("00:00.00")
        }
        stopwatch = null
    }

    private fun ensureTicker() {
        if (stopwatchJob?.isActive == true) return
        stopwatchJob = coroutineScope.launch(Dispatchers.IO) {
            stopwatch?.let {
                it.start()
                while (it.isRunning) {
                    _timeSharedFlow.emit(it.elapsedMs().millionSecondToStopwatch())
                    delay(STOPWATCH_INTERVAL_TIME)
                }
            }
        }
    }


    override fun onCleared() {
        stopwatch?.reset()
        stopwatch = null
        timerJob?.cancel()
        coroutineScope.cancel()
    }
}