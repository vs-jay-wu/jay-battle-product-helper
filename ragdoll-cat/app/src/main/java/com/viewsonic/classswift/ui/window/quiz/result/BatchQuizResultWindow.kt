package com.viewsonic.classswift.ui.window.quiz.result

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.quiz.QuizType.Companion.getString
import com.viewsonic.classswift.databinding.WindowBatchQuizResultBinding
import com.viewsonic.classswift.feature.servicescreens.ui.BatchQuizResultItemUi
import com.viewsonic.classswift.feature.servicescreens.ui.BatchQuizResultList
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.window.quiz.mvb.ComposeWindowHost
import com.viewsonic.classswift.ui.windowmodel.quiz.BatchQuizResultWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.utils.extension.toPercent
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject

class BatchQuizResultWindow(val context: Context) : IWindow<WindowBatchQuizResultBinding> {
    override var tag: WindowTag = WindowTag.BATCH_QUIZ_RESULT
    override var size: SizeInPixels = SizeInPixels(933.33f.dpToPx().toInt(), 569.33f.dpToPx().toInt())
    override val binding: WindowBatchQuizResultBinding = WindowBatchQuizResultBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Theme_MaterialComponents
            )
        )
    )
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val windowModel: BatchQuizResultWindowModel by inject(BatchQuizResultWindowModel::class.java)
    private val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private val composeHost = ComposeWindowHost()
    // Per-question result rows for the Compose list (cv_list); the rest of the window stays native.
    private val itemsFlow = MutableStateFlow<List<BatchQuizResultItemUi>>(emptyList())

    override fun onViewCreated() {
        unclosedMissionUiManager.notifyMissionOngoingIfNeeded(MissionType.BATCH_QUIZZES)
        initClickAction()
        initComposeList()
        initCollection()
        windowModel.getBatchQuizSummary()
    }

    override fun onDestroy() {
        super.onDestroy()
        composeHost.destroy()
        windowModel.onCleared()
        quizCommonWindowModel.onCleared()
        coroutineScope.cancel()
    }

    private fun initComposeList() {
        composeHost.attach(binding.cvList) {
            val items by itemsFlow.collectAsState()
            BatchQuizResultList(items = items, onItemClick = { sequence -> showDetailsResult(sequence) })
        }
    }

    private fun initClickAction() {
        binding.apply {
            viewNetworkDisconnect.bindCloseAction(ibClose)
            WindowControlButtonsUiHelper.setup(
                ivClose = ibClose,
                ivMinimizeWindow = ivMinimizeWindow,
                ivToolbarBringToFront = ivToolbarBringToFront,
                windowTag = tag,
                isMvbBound = quizCommonWindowModel.isMyViewBoardBound(),
                csWindowManager = csWindowManager,
                coroutineScope = coroutineScope,
                onCloseClick = {
                    showEndLoadingUi()
                    windowModel.closeBatchQuiz()
                },
                onAfterMinimize = { unclosedMissionUiManager.notifyMissionMinimizedIfNeeded(MissionType.BATCH_QUIZZES) }
            )
            viewNetworkDisconnect.setOnClickListener {
                // cause avoid click event to parent view, so need to call bringWindowToTop function.
                coroutineScope.launch {
                    csWindowManager.bringWindowToTop(tag)
                    csWindowManager.bringWindowToTop(WindowTag.TOOLBAR)
                }
            }
            cswDetailsResult.onWidgetClose = {
                binding.apply {
                    viewShowDetailMask.isVisible = false
                    cswDetailsResult.isVisible = false
                }
            }
            cslbRefresh.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    windowModel.getBatchQuizSummary()
                    cslbRefresh.setLoading()
                }
            })
        }
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.updateUiEventFlow.collect { uiEvent ->
                binding.apply {
                    when (uiEvent) {
                        is BatchQuizResultWindowModel.BatchQuizResultUiEvent.EndQuizResult -> {
                            dismissLoadingUi()
                            if (uiEvent.result) {
                                csWindowManager.removeWindow(tag)
                            } else {
                                with(binding.cstToast) {
                                    setText(context.getString(R.string.quiz_error_msg_close_quiz))
                                    show(3000L)
                                }
                            }
                        }
                        BatchQuizResultWindowModel.BatchQuizResultUiEvent.Init -> {}
                        is BatchQuizResultWindowModel.BatchQuizResultUiEvent.GetResultFailed -> {
                            dismissGetDataLoadingUi()
                            cslbRefresh.setEnable()
                            llRetry.isVisible = true
                        }
                    }
                }
            }
        }

        coroutineScope.launch(Dispatchers.Main) {
            windowModel.resultInfoFlow.collect { state ->
                if (state is BatchQuizResultWindowModel.BatchQuizResultDataState.ResultData) {
                    val result = state.data
                    binding.apply {
                        dismissGetDataLoadingUi()
                        hideRetryUi()
                        tvSubmittedStudentNumber.text =
                            if (result.isNotEmpty()) result[0].submittedStudentCount.toString() else
                                "0"
                    }
                    itemsFlow.value = result.map {
                        BatchQuizResultItemUi(
                            quizNo = it.sequence.toString(),
                            category = it.quizType.getString(context),
                            correct = it.correctStudentIds.size,
                            incorrect = it.incorrectStudentIds.size,
                            noAnswer = it.noAnswerStudentIds.size,
                            percent = it.accuracyRate.toPercent(),
                            sequence = it.sequence,
                        )
                    }
                }
            }
        }

        coroutineScope.launch(Dispatchers.Main) {
            quizCommonWindowModel.networkAvailabilityState.collect { hasNetwork ->
                binding.viewNetworkDisconnect.isVisible = !hasNetwork
            }
        }
    }

    private fun hideRetryUi() {
        binding.apply {
            llRetry.isVisible = false
        }
    }

    private fun dismissGetDataLoadingUi() {
        binding.apply {
            llLoading.isVisible = false
            lavLoadingAnimation.cancelAnimation()
        }
    }


    private fun showEndLoadingUi() {
        binding.apply {
            windowCloseMask.isVisible = true
            cswLoadingAnimation.isVisible = true
            cswLoadingAnimation.playAnimation()
        }
    }

    private fun dismissLoadingUi() {
        binding.apply {
            windowCloseMask.isVisible = false
            cswLoadingAnimation.isVisible = false
            cswLoadingAnimation.cancelAnimation()
        }
    }

    /** Title-card tap → open the native detail popup for that question (looked up by sequence). */
    private fun showDetailsResult(sequence: Int) {
        binding.apply {
            val target = windowModel.batchQuizSummaryResultList.firstOrNull { it.sequence == sequence }
            target?.let {
                cswDetailsResult.setSummaryInfo(it)
                viewShowDetailMask.isVisible = true
                cswDetailsResult.isVisible = true
            } ?: run {
                viewShowDetailMask.isVisible = false
                cswDetailsResult.isVisible = false
                coroutineScope.launch(Dispatchers.Main) {
                    with(binding.cstToast) {
                        setText("no quiz detail data")
                        show(3000L)
                    }
                }
            }
        }
    }
}
