package com.viewsonic.classswift.ui.window.tool

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.databinding.WindowBuzzerBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.windowmodel.tool.BuzzerWindowModel
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.millionSecondToStopwatch
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class BuzzerWindow(val context: Context) : IWindow<WindowBuzzerBinding> {

    override var tag: WindowTag = WindowTag.BUZZER_TOOL
    override var size: SizeInPixels = SizeInPixels(348f.dpToPx().toInt(), 336f.dpToPx().toInt())
    private val windowModel: BuzzerWindowModel by inject(BuzzerWindowModel::class.java)
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    override val binding: WindowBuzzerBinding = WindowBuzzerBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Theme_MaterialComponents
            )
        )
    )

    override fun onViewCreated() {
        setBuzzerInitUI()
        initClickAction()
        initCollection()
    }

    override fun onDestroy() {
        windowModel.onCleared()
    }

    private fun initClickAction() {
        binding.apply {
            viewNetworkDisconnect.bindCloseAction(buttonClose)
            buttonClose.setOnClickListener {
                if (windowModel.stopBuzzer()) {
                    csWindowManager.removeWindow(tag)
                } else {
                    binding.cstErrorToast.show(coroutineScope)
                }
            }
            tvStart.setOnClickListener {
                if (windowModel.startBuzzer()) {
                    setStartingBuzzerUI()
                } else {
                    binding.cstErrorToast.show(coroutineScope)
                }
            }
            llTryAgain.setOnClickListener {
                if (windowModel.stopBuzzer()) {
                    setBuzzerInitUI()
                } else {
                    binding.cstErrorToast.show(coroutineScope)
                }
            }
            viewNetworkDisconnect.setOnClickListener {
                // cause avoid click event to parent view, so need to call bringWindowToTop function.
                coroutineScope.launch(Dispatchers.Main) {
                    csWindowManager.bringWindowToTop(tag)
                    csWindowManager.bringWindowToTop(WindowTag.TOOLBAR)
                }
            }
        }
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            windowModel.updateUIFlow.collect { state ->
                withContext(Dispatchers.Main) {
                    when (state) {
                        is BuzzerWindowModel.BuzzerUIEvent.UpdateTime -> {
                            if (windowModel.isStop) return@withContext
                            binding.tvTime.text = state.time
                        }
                        is BuzzerWindowModel.BuzzerUIEvent.AnsweredStudentInfo -> {
                            Timber.d("[BuzzerWindow] update student info: ${state.studentInfo}")
                            binding.apply {
                                tvStart.isVisible = false
                                clParticipantInfo.isVisible = true
                                tvSeat.text = state.studentInfo.displaySeatNumber.ifBlank { context.getString(R.string.common_empty_seat_number) }
                                tvName.text = state.studentInfo.displayName
                                llTryAgain.isVisible = true
                            }
                        }
                        is BuzzerWindowModel.BuzzerUIEvent.NetworkAvailabilityState -> {
                            Timber.d("[BuzzerWindow] NetworkAvailabilityState: ${state.available} ")
                            binding.viewNetworkDisconnect.isVisible = !state.available
                            if (!state.available) {
                                AmplitudeEventBuilder(AmplitudeConstant.EventName.RECONNECT_PROMPT_SHOWN).send()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setStartingBuzzerUI() {
        binding.apply {
            tvStart.isVisible = false
            llTryAgain.isVisible = true
        }
    }

    private fun setBuzzerInitUI() {
        binding.apply {
            binding.tvTime.text = 0L.millionSecondToStopwatch()
            tvStart.isVisible = true
            clParticipantInfo.isVisible = false
            llTryAgain.isVisible = false
        }
    }
}