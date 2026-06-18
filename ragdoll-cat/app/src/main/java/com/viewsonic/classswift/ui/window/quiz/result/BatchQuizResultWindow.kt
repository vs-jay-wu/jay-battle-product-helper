package com.viewsonic.classswift.ui.window.quiz.result

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.info.BatchQuizSummaryInfo
import com.viewsonic.classswift.databinding.WindowBatchQuizResultBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.widget.batchquiz.adapter.BatchQuizResultAdapter
import com.viewsonic.classswift.ui.windowmodel.quiz.BatchQuizResultWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import kotlin.getValue

class BatchQuizResultWindow(val context: Context) : IWindow<WindowBatchQuizResultBinding>, BatchQuizResultAdapter.OnItemTitleClickedListener {
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

    private val resultAdapter = BatchQuizResultAdapter(this)

    override fun onViewCreated() {
        unclosedMissionUiManager.notifyMissionOngoingIfNeeded(MissionType.BATCH_QUIZZES)
        initClickAction()
        initCollection()
        initRecyclerView()
        windowModel.getBatchQuizSummary()
    }

    override fun onDestroy() {
        super.onDestroy()
        windowModel.onCleared()
        quizCommonWindowModel.onCleared()
        coroutineScope.cancel()
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
                        resultAdapter.submitList(result)
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

    private fun initRecyclerView() {
        binding.apply {
            rvResultList.layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            rvResultList.adapter = resultAdapter
            rvResultList.setHasFixedSize(true)
        }
    }

    override fun onShowDetailsResult(summaryInfo: BatchQuizSummaryInfo) {
        binding.apply {
            val target: BatchQuizSummaryInfo? =
                windowModel.batchQuizSummaryResultList.firstOrNull { it.sequence == summaryInfo.sequence }
            target?.let {
                cswDetailsResult.setSummaryInfo(target)
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
