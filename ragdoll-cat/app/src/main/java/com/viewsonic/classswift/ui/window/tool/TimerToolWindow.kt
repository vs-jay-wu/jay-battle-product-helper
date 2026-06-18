package com.viewsonic.classswift.ui.window.tool

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager.LayoutParams
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.WindowTimerToolBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.SoundEffectManager
import com.viewsonic.classswift.ui.windowmodel.tool.TimerToolWindowModel
import com.viewsonic.classswift.utils.extension.fade
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.ui.windowmodel.tool.enums.TimerToolStatus
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import com.viewsonic.classswift.ui.windowmodel.tool.listener.OnTimerNumberPickerListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

class TimerToolWindow(val context: Context) : IWindow<WindowTimerToolBinding> {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val soundEffectManager: SoundEffectManager by inject(SoundEffectManager::class.java)
    private val windowModel: TimerToolWindowModel by inject(TimerToolWindowModel::class.java)
    private var timeOption: TimerToolType = TimerToolType.TIMER
    private var timerToolStatus = TimerToolStatus.STOP
    private var soundTik = R.raw.timer_tik
    private var soundDing = R.raw.timer_ding

    override var tag: WindowTag = WindowTag.TIMER_TOOL

    override var size: SizeInPixels = SizeInPixels(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    override val binding: WindowTimerToolBinding = WindowTimerToolBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Theme_MaterialComponents
            )
        )
    )

    override fun getCurrentSize(): SizeInPixels {
        if (binding.root.width <= 0 || binding.root.height <= 0) {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            return SizeInPixels(binding.root.measuredWidth, binding.root.measuredHeight)
        }
        return SizeInPixels(binding.root.width, binding.root.height)
    }


    override fun onViewCreated() {
        super.onViewCreated()
        setTimerPicker()
        setButton()
        initCollection()
        soundEffectManager.preload(soundTik)
        soundEffectManager.preload(soundDing)
    }

    private fun setTimerPicker() {
        binding.cstnpMinuteTens.setTimerNumberPickerClickListener(timerNumberPickerListener)
        binding.cstnpMinuteUnits.setTimerNumberPickerClickListener(timerNumberPickerListener)
        binding.cstnpSecondTens.setTimerNumberPickerClickListener(timerNumberPickerListener)
        binding.cstnpSecondUnits.setTimerNumberPickerClickListener(timerNumberPickerListener)
    }

    private val timerNumberPickerListener = object : OnTimerNumberPickerListener {
        override fun onClick(value: String) {
            binding.buttonStart.isEnabled = getTime() != "0000"
        }
    }

    private fun getTime() =
        "${binding.cstnpMinuteTens.getValue()}${binding.cstnpMinuteUnits.getValue()}${binding.cstnpSecondTens.getValue()}${binding.cstnpSecondUnits.getValue()}"


    private fun setButton() {
        binding.buttonClose.setOnClickListener {
            csWindowManager.removeWindow(tag)
        }

        binding.buttonStart.setOnClickListener {
            if (timeOption == TimerToolType.TIMER) {
                timerAction()
            } else {
                stopwatchAction()
            }
        }

        binding.rgOption.setOnCheckedChangeListener { _, id ->
            when (id) {
                binding.rbTimer.id -> {
                    timeOption = TimerToolType.TIMER
                    binding.tvTitle.text = context.getString(R.string.common_timer)
                    binding.clTimepicker.visibility = View.VISIBLE
                    binding.buttonStart.isEnabled = false
                    timerReset()
                }

                binding.rbStopwatch.id -> {
                    timeOption = TimerToolType.STOPWATCH
                    binding.tvTitle.text = context.getString(R.string.common_stopwatch)
                    binding.clTimepicker.visibility = View.INVISIBLE
                    binding.buttonStart.isEnabled = true
                    resetTimerNumberPicker()
                    stopwatchReset()
                }
            }
        }

        binding.buttonContinue.setOnClickListener {
            windowModel.stopwatchContinue()
            binding.buttonStart.text = context.getString(R.string.common_stop)
            timerToolStatus = TimerToolStatus.START
            binding.rbTimer.isEnabled = false
            showStopwatchButton(false)
        }

        //stopwatch 所使用的 try again
        binding.buttonTryAgain.setOnClickListener {
            windowModel.stopwatchReset()
            binding.buttonStart.text = context.getString(R.string.common_start)
            timerToolStatus = TimerToolStatus.STOP
            showStopwatchButton(false)
        }
    }

    private fun timerAction() {
        when (timerToolStatus) {
            TimerToolStatus.STOP -> {
                timerToolStatus = TimerToolStatus.START
                binding.tvTime.visibility = View.VISIBLE
                showTimerNumberPicker(false)
                binding.buttonStart.text = context.getString(R.string.common_stop)
                binding.rbStopwatch.isEnabled = false
                windowModel.timerStart(getTime())
                soundEffectManager.stop(soundTik)
            }

            TimerToolStatus.START -> {
                windowModel.timerStop()
                timerReset()
            }

            TimerToolStatus.TRY_AGAIN -> {
                timerReset()
            }

            else -> {}
        }
    }

    private fun stopwatchAction() {
        when (timerToolStatus) {
            TimerToolStatus.STOP -> {
                timerToolStatus = TimerToolStatus.START
                windowModel.stopwatchStart()
                binding.buttonStart.text = context.getString(R.string.common_stop)
                binding.rbTimer.isEnabled = false
            }

            TimerToolStatus.START -> {
                windowModel.stopwatchStop()
                binding.rbTimer.isEnabled = true
                showStopwatchButton(true)
            }

            else -> {}
        }

    }


    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            windowModel.timeSharedFlow.collect { time ->
                withContext(Dispatchers.Main) {
                    when (timeOption) {
                        TimerToolType.TIMER -> {
                            if (time != "finish") {
                                binding.tvTime.text = time
                                binding.tvColon.fade()
                            } else {
                                //countdown end
                                binding.tvColon.visibility = View.INVISIBLE
                                binding.tvTime.text = context.getString(R.string.tools_timer_times_up)
                                binding.tvTime.letterSpacing = 0f
                                binding.tvTime.setTextColor(ContextCompat.getColor(context, R.color.red_600))
                                binding.buttonStart.text = context.getString(R.string.common_try_again)
                                timerToolStatus = TimerToolStatus.TRY_AGAIN
                                binding.rbStopwatch.isEnabled = true
                            }
                        }

                        TimerToolType.STOPWATCH -> {
                            binding.tvTime.text = time
                        }
                    }

                }
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            windowModel.soundPlaySharedFlow.collect { sound ->
                withContext(Dispatchers.Main) {
                    when (sound) {
                        TimerToolWindowModel.SOUND_TIK -> {
                            soundEffectManager.play(soundTik)
                        }

                        TimerToolWindowModel.SOUND_DING -> {
                            soundEffectManager.play(soundDing)
                        }
                    }
                }
            }
        }
    }

    private fun timerReset() {
        binding.tvColon.visibility = View.VISIBLE
        showTimerNumberPicker(true)
        binding.tvTime.setTextColor(ContextCompat.getColor(context, R.color.neutral_900))
        binding.buttonStart.text = context.getString(R.string.common_start)
        binding.tvTime.visibility = View.INVISIBLE
        binding.tvTime.text = "00:00"
        binding.tvTime.letterSpacing = 0.5f
        timerToolStatus = TimerToolStatus.STOP
        binding.rbStopwatch.isEnabled = true
        soundEffectManager.stop(soundTik)
        showStopwatchButton(false)
    }

    private fun resetTimerNumberPicker() {
        binding.cstnpMinuteTens.setValue("0")
        binding.cstnpMinuteUnits.setValue("0")
        binding.cstnpSecondTens.setValue("0")
        binding.cstnpSecondUnits.setValue("0")
    }

    private fun stopwatchReset() {
        binding.clTimepicker.visibility = View.INVISIBLE
        binding.tvTime.visibility = View.VISIBLE
        binding.tvTime.setTextColor(ContextCompat.getColor(context, R.color.neutral_900))
        binding.buttonStart.text = context.getString(R.string.common_start)
        binding.buttonStart.visibility = View.VISIBLE
        timerToolStatus = TimerToolStatus.STOP
        showStopwatchButton(false)
        windowModel.stopwatchReset()
        binding.tvTime.letterSpacing = 0.3f
    }


    private fun showTimerNumberPicker(isShow: Boolean) {
        val visibility = if (isShow) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
        binding.cstnpMinuteTens.visibility = visibility
        binding.cstnpMinuteUnits.visibility = visibility
        binding.cstnpSecondTens.visibility = visibility
        binding.cstnpSecondUnits.visibility = visibility
    }

    private fun showStopwatchButton(isShow: Boolean) {
        binding.buttonStart.visibility = if (isShow) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }
        val visibility = if (isShow) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }
        binding.buttonTryAgain.visibility = visibility
        binding.buttonContinue.visibility = visibility
    }


    override fun onDestroy() {
        coroutineScope.cancel()
        windowModel.onCleared()
        soundEffectManager.release()
    }

    enum class TimerToolType {
        TIMER,
        STOPWATCH
    }
}