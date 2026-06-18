package com.viewsonic.classswift.ui.window.quiz.edit

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import com.viewsonic.classswift.R
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.constant.AppConstants.ONE_SEC_DELAY
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.quiz.QuizOption
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.data.state.SelectionOptionType
import com.viewsonic.classswift.databinding.WindowMvbPollQuizEditBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.ScreenshotManager
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.widget.quiz.MvbImageUploadView
import com.viewsonic.classswift.ui.widget.quiz.MvbOptionPanel
import com.viewsonic.classswift.ui.widget.quiz.enums.MvbOptionPanelMode
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionValueType
import com.viewsonic.classswift.ui.window.ToastWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbPollQuizStartWindow
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizEditWindowModel
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.extension.handleQuizStartWithOngoingMission
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

class MvbPollQuizEditWindow(private val applicationContext: Context) : IWindow<WindowMvbPollQuizEditBinding> {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val screenshotManager: ScreenshotManager by inject(ScreenshotManager::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    private val quizEditWindowModel: QuizEditWindowModel by inject(QuizEditWindowModel::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private val mvbImageUploadViewListener: MvbImageUploadView.Listener = object : MvbImageUploadView.Listener {
        override fun onTryAgainButtonClicked() {
            binding.miuvImageUploadView.let {
                it.setUploadFailedContainerVisibility(isShown = false)
                it.setProgressbarVisibility(isShown = true)
                it.setCaptureAgainButtonEnabled(isEnabled = false)
                it.startProgressAnimation()
            }
            quizEditWindowModel.startUploadImage(screenshotManager.getScreenshotImageUri())
        }

        override fun onCaptureAgainButtonClicked() {
            startCaptureScreenshot()
        }
    }

    override var tag: WindowTag = WindowTag.MVB_POLL_EDIT_QUIZ
    override var size: SizeInPixels = SizeInPixels(
        applicationContext.resources.getDimensionPixelSize(R.dimen.quiz_mvb_poll_edit_window_width),
        applicationContext.resources.getDimensionPixelSize(R.dimen.quiz_mvb_poll_edit_window_height),
    )

    override val binding: WindowMvbPollQuizEditBinding = WindowMvbPollQuizEditBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                applicationContext,
                com.google.android.material.R.style.Theme_MaterialComponents
            )
        )
    )

    override fun onViewCreated() {
        super.onViewCreated()
        quizCommonWindowModel.addOpenedQuizWindowTag(tag)
        initUI()
        initListener()
        initOnClick()
        initCollection()
        quizEditWindowModel.startUploadImage(screenshotManager.getScreenshotImageUri())
    }

    private fun initUI() {
        binding.miuvImageUploadView.let {
            it.setImage(uri = quizCommonWindowModel.getScreenImageUri())
            it.setUploadFailedContainerVisibility(isShown = false)
            it.setMaskVisibility(isShown = true)
            it.setCaptureAgainButtonEnabled(isEnabled = false)
            it.setListener(mvbImageUploadViewListener)
        }
        binding.mvbOpOptionPanel.apply {
            setMode(MvbOptionPanelMode.POLL)
            setOptionNumber(QuizSharedUiInfo.quizOptionCount)
            setOptionValueType(
                when (QuizSharedUiInfo.quizOptionType) {
                    QuizOptionType.NUMBER -> OptionValueType.NUMBER
                    else -> OptionValueType.ALPHABET
                }
            )
            setSelectionOptionType(QuizSharedUiInfo.singleOrMultipleSelectionType)
        }
        disableStartQuizButton()
    }

    private fun initListener() {
        binding.mvbOpOptionPanel.setOnChangeListener(object : MvbOptionPanel.OnChangeListener {
            override fun onOptionValueTypeChanged(type: OptionValueType) {
                QuizSharedUiInfo.quizOptionType = when (type) {
                    OptionValueType.NUMBER -> QuizOptionType.NUMBER
                    OptionValueType.ALPHABET -> QuizOptionType.ALPHABET
                }
                QuizSharedUiInfo.setQuizTypeByTag(tag)
            }

            override fun onSelectionOptionTypeChanged(type: SelectionOptionType) {
                QuizSharedUiInfo.singleOrMultipleSelectionType = type
                QuizSharedUiInfo.setQuizTypeByTag(tag)
            }

            override fun onOptionCountChanged(count: Int) {}
        })
    }

    private fun initOnClick() {
        binding.apply {
            buttonStartQuestion.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    binding.buttonStartQuestion.setLoading()
                    binding.csMessageDialog.handleQuizStartWithOngoingMission(
                        coroutineScope = coroutineScope,
                        unclosedMissionUiManager = unclosedMissionUiManager,
                        onStartQuiz = { hasPushAndRespond ->
                            createQuiz(hasPushAndRespond)
                        },
                        onCanceled = {
                            binding.buttonStartQuestion.setEnable()
                        },
                        onBatchCloseFailed = {
                            showErrorToast(applicationContext.getString(R.string.quiz_error_msg_ongoing_quiz))
                            binding.buttonStartQuestion.setEnable()
                        },
                        onMaskClicked = {
                            csWindowManager.bringWindowToTop(windowTag = this@MvbPollQuizEditWindow.tag)
                        }
                    )
                }
            })

            buttonCancelQuestion.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    showCloseConfirmDialog()
                }
            })

            WindowControlButtonsUiHelper.setupForMvbView(
                ivClose = ivBtnClose,
                ivMinimizeWindow = ivBtnMinimize,
                windowTag = tag,
                csWindowManager = csWindowManager,
                onCloseClick = {
                    showCloseConfirmDialog()
                },
                onAfterMinimize = { unclosedMissionUiManager.notifyMissionMinimizedIfNeeded(MissionType.QUIZ) }
            )
        }
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            quizEditWindowModel.imageUploadSharedFlow.collect { isUploadSuccess ->
                when (isUploadSuccess) {
                    true -> {
                        Timber.d("[MvbPollQuizEditWindow] isUploadSuccess: true")
                        withContext(Dispatchers.Main) {
                            with(binding.miuvImageUploadView) {
                                startProgressAnimation(fromPercentage = 90, toPercentage = 100)
                                withContext(Dispatchers.IO) { delay(500L) }
                                setMaskVisibility(false)
                                setProgressbarVisibility(false)
                                setCaptureAgainButtonEnabled(isEnabled = true)
                            }
                            enableStartQuizButton()
                        }
                    }

                    false -> {
                        Timber.d("[MvbPollQuizEditWindow] isUploadSuccess: false")
                        withContext(Dispatchers.Main) {
                            binding.miuvImageUploadView.apply {
                                setProgressbarVisibility(isShown = false)
                                setUploadFailedContainerVisibility(isShown = true)
                                setCaptureAgainButtonEnabled(isEnabled = true)
                            }
                            disableStartQuizButton()
                        }
                    }
                }
            }
        }
    }

    private fun showCloseConfirmDialog() {
        binding.msdvCloseConfirm.setup(
            title = applicationContext.getString(R.string.quiz_disclose_close_confirm_title),
            message = applicationContext.getString(R.string.quiz_disclose_close_confirm_body),
            positiveText = applicationContext.getString(R.string.quiz_disclose_close_confirm_positive),
            onPositive = { binding.msdvCloseConfirm.dismiss(); close() },
            negativeText = applicationContext.getString(R.string.quiz_disclose_close_confirm_negative),
        ).show()
    }

    private fun close() {
        binding.flCloseLoadingOverlay.visibility = View.VISIBLE
        binding.mlaLoading.playAnimation()
        coroutineScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                delay(ONE_SEC_DELAY)
            }
            csWindowManager.removeWindow(tag)
            unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
        }
    }

    private fun showErrorToast(message: String) {
        coroutineScope.launch(Dispatchers.Main) {
            with(binding.mtToast) {
                setText(message)
                show()
            }
        }
    }

    private fun startQuizWindow() {
        csWindowManager.removeWindow(tag)
        csWindowManager.createWindow(get(MvbPollQuizStartWindow::class.java), Gravity.CENTER)
    }

    private fun startCaptureScreenshot() {
        screenshotManager.startCaptureScreenshot(
            screenshotSource = AmplitudeConstant.EventProperties.Value.SELECT_AGAIN,
            onSuccess = {
                csWindowManager.removeWindow(tag)
                QuizSharedUiInfo.screenshotImageUri = screenshotManager.getScreenshotImageUri()
                QuizSharedUiInfo.setQuizTypeByTag(tag)
                csWindowManager.createWindow(get(MvbPollQuizEditWindow::class.java), Gravity.CENTER)
            },
            onFailed = {
                ToastWindow.MakeText(
                    applicationContext,
                    applicationContext.getString(R.string.quiz_error_msg_screenshot),
                    3000
                ).build().show()
            },
            onCancel = {}
        )
    }

    private fun disableStartQuizButton() {
        binding.buttonStartQuestion.setDisable()
    }

    private fun enableStartQuizButton() {
        binding.buttonStartQuestion.setEnable()
    }

    private suspend fun createQuiz(isNeedToStopPushAndRespond: Boolean = false) = withContext(Dispatchers.Main) {
        val optionCount = binding.mvbOpOptionPanel.getOptionNumber()
        val optionType = when (binding.mvbOpOptionPanel.getOptionValueType()) {
            OptionValueType.NUMBER -> QuizOptionType.NUMBER
            OptionValueType.ALPHABET -> QuizOptionType.ALPHABET
        }
        val quizOptions = (1..optionCount).map { QuizOption(optionId = it) }.toMutableList()

        val isSuccessful = quizEditWindowModel.createQuiz(
            optionType = optionType,
            quizType = QuizSharedUiInfo.quizType,
            quizOptions = quizOptions
        )

        when (isSuccessful) {
            true -> {
                if (isNeedToStopPushAndRespond) {
                    unclosedMissionUiManager.closeMission(MissionType.PUSH_AND_RESPOND_TASK)
                }
                QuizSharedUiInfo.quizOptionType = optionType
                QuizSharedUiInfo.quizOptionCount = optionCount
                quizEditWindowModel.saveMultipleOptionInfos()
                startQuizWindow()
            }
            false -> {
                if (isNeedToStopPushAndRespond) {
                    showErrorToast(applicationContext.getString(R.string.error_msg_end_task_and_start_quiz))
                } else {
                    showErrorToast(applicationContext.getString(R.string.quiz_error_msg_start_quiz))
                }
                binding.buttonStartQuestion.setEnable()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        quizEditWindowModel.onCleared()
        quizCommonWindowModel.removeOpenedQuizWindowTag(tag)
        quizCommonWindowModel.onCleared()
    }
}
