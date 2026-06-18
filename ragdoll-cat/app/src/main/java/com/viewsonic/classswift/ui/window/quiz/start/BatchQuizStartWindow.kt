package com.viewsonic.classswift.ui.window.quiz.start

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.databinding.WindowBatchQuizStartBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.LoadingButtonState
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.window.CSSystemDialogWindow
import com.viewsonic.classswift.ui.window.quiz.result.BatchQuizResultWindow
import com.viewsonic.classswift.ui.windowmodel.quiz.BatchQuizStartWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject
import java.util.Locale

class BatchQuizStartWindow(val context: Context): IWindow<WindowBatchQuizStartBinding> {

    override var tag: WindowTag = WindowTag.BATCH_START_QUIZ
    override var size: SizeInPixels = SizeInPixels(380f.dpToPx().toInt(), 336f.dpToPx().toInt())
    private val windowModel: BatchQuizStartWindowModel by inject(BatchQuizStartWindowModel::class.java)
    private val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private var dialogWindow: CSSystemDialogWindow? = null

    override val binding: WindowBatchQuizStartBinding = WindowBatchQuizStartBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Theme_MaterialComponents
            )
        )
    )

    override fun onViewCreated() {
        unclosedMissionUiManager.notifyMissionOngoingIfNeeded(MissionType.BATCH_QUIZZES)
        initView()
        initCollection()
        initClickAction()
        windowModel.startTimer()
        quizCommonWindowModel.addOpenedQuizWindowTag(tag)
    }

    override fun onDestroy() {
        super.onDestroy()
        dialogWindow?.dismiss()
        dialogWindow = null
        coroutineScope.cancel()
        windowModel.onCleared()
        quizCommonWindowModel.onCleared()
        quizCommonWindowModel.removeOpenedQuizWindowTag(tag)
    }

    private fun initView() {
        binding.apply {
            tvTimer.text = "00:00"
            updateStudentNumber(0, 0)
        }
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.updateUiEventFlow.collect { uiEvent ->
                when (uiEvent) {
                    is BatchQuizStartWindowModel.BatchQuizStartingUiEvent.UpdateTime -> {
                        binding.tvTimer.text = uiEvent.time
                    }

                    is BatchQuizStartWindowModel.BatchQuizStartingUiEvent.CancelQuizResult -> {
                        dismissEndQuizDialog()
                        if (uiEvent.result) {
                            csWindowManager.removeWindow(tag)
                        } else {
                            showErrorToast(context.getString(R.string.quiz_error_msg_close_quiz))
                        }
                    }

                    is BatchQuizStartWindowModel.BatchQuizStartingUiEvent.DiscloseQuizResult -> {
                        if (uiEvent.result) {
                            csWindowManager.getWindow(WindowTag.BATCH_QUIZ_RESULT)?.hoistWindowZOrder() ?: run {
                                val window: BatchQuizResultWindow = get(BatchQuizResultWindow::class.java)
                                csWindowManager.createWindow(
                                    window, Gravity.CENTER
                                )
                            }
                            csWindowManager.removeWindow(tag)
                        } else {
                            showErrorToast(context.getString(R.string.quiz_error_msg_close_quiz))
                            binding.cslbEndQuiz.setEnable()
                        }
                    }
                }
            }
        }

        coroutineScope.launch(Dispatchers.Main) {
            windowModel.uiStateFlow.collect { answeredCountUiState ->
                updateStudentNumber(answeredCountUiState.submittedCount, answeredCountUiState.joinCount)
            }
        }

        coroutineScope.launch(Dispatchers.Main) {
            quizCommonWindowModel.networkAvailabilityState.collect { hasNetwork ->
                binding.viewNetworkDisconnect.isVisible = !hasNetwork
            }
        }
    }

    private fun initClickAction() {
        binding.apply {
            viewNetworkDisconnect.bindCloseAction(ibClose)
            ibClose.setOnClickListener {
                showEndQuizDialog()
            }
            viewNetworkDisconnect.setOnClickListener {
                // cause avoid click event to parent view, so need to call bringWindowToTop function.
                coroutineScope.launch(Dispatchers.Main) {
                    csWindowManager.bringWindowToTop(tag)
                    csWindowManager.bringWindowToTop(WindowTag.TOOLBAR)
                }
            }

            cslbEndQuiz.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onStateChange(state: LoadingButtonState) {
                }
                override fun onEnableClicked() {
                    coroutineScope.launch(Dispatchers.Main) {
                        cslbEndQuiz.setLoading()
                        windowModel.finishAndDiscloseBatchQuiz()
                    }
                }
            })
        }
    }

    private fun updateStudentNumber(current: Int, total: Int) {
        binding.tvAnswerQuizStudentNumber.text =
            String.format(Locale.getDefault(), "%02d/%02d", current, total)
    }

    private fun showEndQuizDialog() {
        if (dialogWindow?.isShowing() == true) return
        coroutineScope.launch(Dispatchers.Main) {
            dialogWindow =
                CSSystemDialogWindow.Builder(context)
                    .setTitle(context.getString(R.string.common_close_quiz))
                    .setMessage(context.getString(R.string.end_batch_quiz_dialog_message))
                    .setNegativeButton(
                        context.getString(R.string.common_cancel),
                        context.getColor(R.color.cs_system_dialog_text_color)
                    ) {
                        dismissEndQuizDialog()
                    }
                    .setPositiveButton(
                        context.getString(R.string.common_close),
                        context.getColor(R.color.window_my_class_dialog_delete)
                    ) {
                        coroutineScope.launch(Dispatchers.Main) {
                            dialogWindow?.startPositiveButtonLoading()
                            dialogWindow?.setNegativeButtonEnable(false)
                            windowModel.cancelBatchQuiz()
                        }
                    }
                    .build()
            dialogWindow?.show()
        }
    }

    private fun dismissEndQuizDialog() {
        dialogWindow?.dismiss()
        dialogWindow = null
    }

    private fun showErrorToast(msg: String) {
        coroutineScope.launch(Dispatchers.Main) {
            with(binding.cstToast) {
                setText(msg)
                show(3000L)
            }
        }
    }
}