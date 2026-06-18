package com.viewsonic.classswift.ui.window.tool

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.databinding.WindowRandomDrawBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.windowmodel.tool.RandomDrawWindowModel
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

class RandomDrawWindow(val context: Context) : IWindow<WindowRandomDrawBinding> {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val windowModel: RandomDrawWindowModel by inject(RandomDrawWindowModel::class.java)
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private var collectUiStateJob: Job? = null

    override var tag: WindowTag = WindowTag.RANDOM_DRAW_TOOL

    override var size: SizeInPixels = SizeInPixels(348f.dpToPx().toInt(), 336f.dpToPx().toInt())

    override val binding: WindowRandomDrawBinding = WindowRandomDrawBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Theme_MaterialComponents
            )
        )
    )

    override fun getCurrentSize(): SizeInPixels {
        return size
    }

    override fun onViewCreated() {
        initView()
        initCollection()
        initClickAction()
    }

    private fun initView() {
        binding.viewNetworkDisconnect.setOnClickListener {
            coroutineScope.launch(Dispatchers.Main) {
                // cause avoid click event to parent view, so need to call bringWindowToTop function.
                csWindowManager.bringWindowToTop(tag)
                csWindowManager.bringWindowToTop(WindowTag.TOOLBAR)
            }
        }
    }

    private fun initCollection() {
        collectUiStateJob = coroutineScope.launch {
            windowModel.uiState.collect { state ->
                withContext(Dispatchers.Main) {
                    when (state) {
                        RandomDrawWindowModel.RandomDrawUiState.InitUi -> showInitUi()
                        RandomDrawWindowModel.RandomDrawUiState.NoAttendedStudent -> showNoParticipantUI()
                        RandomDrawWindowModel.RandomDrawUiState.PlayDiceAnimation -> playDiceAnimation()
                        RandomDrawWindowModel.RandomDrawUiState.ShowStudentInfo -> showParticipantUI()
                        RandomDrawWindowModel.RandomDrawUiState.HasNetWork -> {
                            binding.viewNetworkDisconnect.visibility = View.GONE
                            if (!binding.clParticipantInfo.isVisible) {
                                showInitUi()
                            }
                        }
                        RandomDrawWindowModel.RandomDrawUiState.ShowNoNetworkView -> {
                            binding.viewNetworkDisconnect.visibility = View.VISIBLE
                            binding.lottieDice.cancelAnimation()
                            AmplitudeEventBuilder(AmplitudeConstant.EventName.RECONNECT_PROMPT_SHOWN).send()
                        }
                    }
                }
            }
        }
        coroutineScope.launch {
            windowModel.sendSocketEventError.collect {
                withContext(Dispatchers.Main) {
                    binding.cstErrorToast.show(coroutineScope)
                }
            }
        }
    }

    private fun initClickAction() {
        binding.apply {
            viewNetworkDisconnect.bindCloseAction(buttonClose)
            buttonClose.setOnClickListener {
                csWindowManager.removeWindow(tag)
            }
            clDice.setOnClickListener {
                windowModel.selectStudent()
            }
            llTryAgain.setOnClickListener {
                showInitUi()
            }
        }
    }

    private fun playDiceAnimation() {
        binding.apply {
            clParticipantInfo.isVisible = false
            clDice.isVisible = false
            lottieDice.isVisible = true
            tvNoParticipants.isVisible = false
            llTryAgain.isVisible = false
            lottieDice.playAnimation()
        }
    }

    private fun showInitUi() {
        binding.apply {
            clParticipantInfo.isVisible = false
            clDice.isVisible = true
            clDice.isClickable = true
            lottieDice.isVisible = false
            tvNoParticipants.isVisible = false
            llTryAgain.isVisible = false
        }
    }

    private fun showNoParticipantUI() {
        binding.apply {
            clParticipantInfo.isVisible = false
            clDice.isVisible = true
            clDice.isClickable = false
            lottieDice.isVisible = false
            tvNoParticipants.isVisible = true
            llTryAgain.isVisible = false
        }
    }

    private fun showParticipantUI() {
        binding.apply {
            lottieDice.cancelAnimation()
            clParticipantInfo.isVisible = true
            tvSeat.text = windowModel.selectedStudentInfo?.displaySeatNumber?.ifBlank { context.getString(R.string.common_empty_seat_number) }
                ?: windowModel.selectedStudentInfo?.displaySeatNumber
            tvName.text = windowModel.selectedStudentInfo?.displayName
            clDice.isVisible = false
            lottieDice.isVisible = false
            tvNoParticipants.isVisible = false
            llTryAgain.isVisible = true
        }
    }

    override fun onDestroy() {
        collectUiStateJob?.cancel()
        binding.lottieDice.cancelAnimation()
        windowModel.onCleared()
    }
}