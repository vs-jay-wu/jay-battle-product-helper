package com.viewsonic.classswift.ui.window.tool

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.viewsonic.classswift.R
import com.viewsonic.classswift.feature.servicescreens.ui.TimerToolScreen
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.SoundEffectManager
import com.viewsonic.classswift.ui.window.compose.ComposeHostWindow
import com.viewsonic.classswift.ui.windowmodel.tool.TimerToolWindowModel
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

/**
 * Timer/Stopwatch tool (service path) wired via a ComposeView. Reproduces the native state machine
 * (timer countdown with stepper picker + "Time's up", stopwatch run/pause/continue) and plays the
 * tik/ding sounds from TimerToolWindowModel.
 */
class TimerToolWindow(val context: Context) : ComposeHostWindow(context) {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val soundEffectManager: SoundEffectManager by inject(SoundEffectManager::class.java)
    private val windowModel: TimerToolWindowModel by inject(TimerToolWindowModel::class.java)
    private val soundTik = R.raw.timer_tik
    private val soundDing = R.raw.timer_ding

    override var tag: WindowTag = WindowTag.TIMER_TOOL
    override var size: SizeInPixels = SizeInPixels(346.66f.dpToPx().toInt(), 336f.dpToPx().toInt())
    override fun getCurrentSize(): SizeInPixels = size

    private enum class Mode { TIMER, STOPWATCH }
    private enum class Status { STOP, RUNNING, TIMER_FINISHED, SW_PAUSED }
    private data class Ui(
        val mode: Mode = Mode.TIMER,
        val status: Status = Status.STOP,
        val digits: List<Int> = listOf(0, 0, 0, 0),
        val display: String = "00:00",
        val timesUp: Boolean = false,
    )
    private val ui = MutableStateFlow(Ui())

    override fun onViewCreated() {
        super.onViewCreated()
        initCollection()
        soundEffectManager.preload(soundTik)
        soundEffectManager.preload(soundDing)
    }

    @Composable
    override fun Content() {
        val s by ui.collectAsState()
        TimerToolScreen(
            title = if (s.mode == Mode.TIMER) context.getString(R.string.common_timer) else context.getString(R.string.common_stopwatch),
            showPicker = s.mode == Mode.TIMER && s.status == Status.STOP,
            digits = s.digits,
            display = s.display,
            timesUp = s.timesUp,
            timerSelected = s.mode == Mode.TIMER,
            timerRadioEnabled = !(s.mode == Mode.STOPWATCH && (s.status == Status.RUNNING || s.status == Status.SW_PAUSED)),
            stopwatchRadioEnabled = !(s.mode == Mode.TIMER && s.status == Status.RUNNING),
            startText = when (s.status) {
                Status.RUNNING -> context.getString(R.string.common_stop)
                Status.TIMER_FINISHED -> context.getString(R.string.common_try_again)
                else -> context.getString(R.string.common_start)
            },
            startEnabled = if (s.mode == Mode.STOPWATCH) true else s.digits.any { it != 0 },
            showStopwatchControls = s.status == Status.SW_PAUSED,
            onUp = { i -> step(i, up = true) },
            onDown = { i -> step(i, up = false) },
            onStart = { onStart() },
            onContinue = { onContinue() },
            onStopwatchTryAgain = { onStopwatchTryAgain() },
            onSelectTimer = { selectMode(Mode.TIMER) },
            onSelectStopwatch = { selectMode(Mode.STOPWATCH) },
            onClose = { csWindowManager.removeWindow(tag) },
        )
    }

    private fun step(index: Int, up: Boolean) {
        val base = if (index == 0 || index == 2) 6 else 10
        ui.update { s ->
            val d = s.digits.toMutableList()
            d[index] = if (up) (d[index] + 1) % base else (d[index] + base - 1) % base
            s.copy(digits = d)
        }
    }

    private fun timeString() = ui.value.digits.joinToString("")

    private fun onStart() {
        val s = ui.value
        if (s.mode == Mode.TIMER) {
            when (s.status) {
                Status.STOP -> { windowModel.timerStart(timeString()); ui.update { it.copy(status = Status.RUNNING, timesUp = false) } }
                Status.RUNNING -> { windowModel.timerStop(); resetTimer() }
                Status.TIMER_FINISHED -> resetTimer()
                else -> Unit
            }
        } else {
            when (s.status) {
                Status.STOP -> { windowModel.stopwatchStart(); ui.update { it.copy(status = Status.RUNNING) } }
                Status.RUNNING -> { windowModel.stopwatchStop(); ui.update { it.copy(status = Status.SW_PAUSED) } }
                else -> Unit
            }
        }
    }

    private fun onContinue() {
        windowModel.stopwatchContinue()
        ui.update { it.copy(status = Status.RUNNING) }
    }

    private fun onStopwatchTryAgain() {
        windowModel.stopwatchReset()
        ui.update { it.copy(status = Status.STOP, display = "00:00") }
    }

    private fun resetTimer() {
        ui.update { it.copy(status = Status.STOP, display = "00:00", timesUp = false) }
        soundEffectManager.stop(soundTik)
    }

    private fun selectMode(mode: Mode) {
        if (mode == Mode.TIMER) {
            windowModel.stopwatchReset()
            ui.value = Ui(mode = Mode.TIMER, status = Status.STOP)
        } else {
            windowModel.stopwatchReset()
            ui.value = Ui(mode = Mode.STOPWATCH, status = Status.STOP, display = "00:00")
        }
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            windowModel.timeSharedFlow.collect { time ->
                withContext(Dispatchers.Main) {
                    if (ui.value.mode == Mode.TIMER) {
                        if (time != "finish") {
                            ui.update { it.copy(display = time) }
                        } else {
                            ui.update { it.copy(display = context.getString(R.string.tools_timer_times_up), timesUp = true, status = Status.TIMER_FINISHED) }
                        }
                    } else {
                        ui.update { it.copy(display = time) }
                    }
                }
            }
        }
        coroutineScope.launch(Dispatchers.IO) {
            windowModel.soundPlaySharedFlow.collect { sound ->
                withContext(Dispatchers.Main) {
                    when (sound) {
                        TimerToolWindowModel.SOUND_TIK -> soundEffectManager.play(soundTik)
                        TimerToolWindowModel.SOUND_DING -> soundEffectManager.play(soundDing)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        windowModel.onCleared()
        soundEffectManager.release()
    }
}
