package com.viewsonic.classswift.ui.window.quiz.edit

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.databinding.WindowShortAnswerEditQuizBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.ScreenshotManager
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.widget.quiz.ImageUploadView.ReUploadImageListener
import com.viewsonic.classswift.ui.window.ToastWindow
import com.viewsonic.classswift.ui.window.quiz.start.ShortAnswerStartWindow
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizEditWindowModel
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.utils.extension.handleQuizStartWithOngoingMission
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
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

class ShortAnswerEditWindow(private val applicationContext: Context) : IWindow<WindowShortAnswerEditQuizBinding> {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val screenshotManager: ScreenshotManager by inject(ScreenshotManager::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    private val quizEditWindowModel: QuizEditWindowModel by inject(QuizEditWindowModel::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    override var tag: WindowTag = WindowTag.SHORT_ANSWER_EDIT_QUIZ
    override var size: SizeInPixels = SizeInPixels(385f.dpToPx().toInt(), 515f.dpToPx().toInt())

    override val binding: WindowShortAnswerEditQuizBinding = WindowShortAnswerEditQuizBinding.inflate(
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
        initOnClick()
        initListener()
        initCollection()
        quizEditWindowModel.startUploadImage(screenshotManager.getScreenshotImageUri())
    }

    private fun initUI() {
        binding.csImageUploadView.let {
            it.setImage(uri = quizCommonWindowModel.getScreenImageUri())
            it.setUploadFailedContainerVisibility(isShown = false)
            it.setMaskVisibility(isShown = true)
        }

        disableSelectAgainButton()
        disableStartQuizButton()
    }

    private fun disableSelectAgainButton() {
        binding.buttonSelectAgain.setDisable()
    }

    private fun enableSelectAgainButton() {
        binding.buttonSelectAgain.setEnable()
    }

    private fun disableStartQuizButton() {
        binding.buttonStartQuiz.setDisable()
    }

    private fun enableStartQuizButton() {
        binding.buttonStartQuiz.setEnable()
    }

    private fun initListener() {
        // For ImageUploadView re-upload
        val reUploadImageListener: ReUploadImageListener = object : ReUploadImageListener {
            override fun onUploadImage() {
                binding.csImageUploadView.let {
                    it.setUploadFailedContainerVisibility(isShown = false)
                    it.setCircleProgressbarVisibility(isShown = true)
                    it.startProgressAnimation()
                }
                quizEditWindowModel.startUploadImage(screenshotManager.getScreenshotImageUri())
            }
        }
        binding.csImageUploadView.setOnUploadImageListener(reUploadImageListener)
    }

    private fun initOnClick() {
        binding.apply {
            buttonStartQuiz.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    binding.buttonStartQuiz.setLoading()
                    binding.csMessageDialog.handleQuizStartWithOngoingMission(
                        coroutineScope = coroutineScope,
                        unclosedMissionUiManager = unclosedMissionUiManager,
                        onStartQuiz = { hasPushAndRespond ->
                            createQuiz(hasPushAndRespond)
                        },
                        onCanceled = {
                            binding.buttonStartQuiz.setEnable()
                        },
                        onBatchCloseFailed = {
                            showErrorToast(applicationContext.getString(R.string.quiz_error_msg_ongoing_quiz))
                            binding.buttonStartQuiz.setEnable()
                        },
                        onMaskClicked = {
                            csWindowManager.bringWindowToTop(windowTag = this@ShortAnswerEditWindow.tag)
                        }
                    )
                }
            })

            buttonSelectAgain.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    startCaptureScreenshot()
                }
            })

            viewNetworkDisconnect.bindCloseAction(buttonClose)
            WindowControlButtonsUiHelper.setup(
                ivClose = buttonClose,
                ivMinimizeWindow = ivMinimizeWindow,
                ivToolbarBringToFront = ivToolbarBringToFront,
                windowTag = tag,
                isMvbBound = quizCommonWindowModel.isMyViewBoardBound(),
                csWindowManager = csWindowManager,
                coroutineScope = coroutineScope,
                onCloseClick = {
                    // Disable reminder when closing the window before the quiz begins
                    binding.csLoadingAnimation.apply {
                        // play Lottie animation
                        visibility = View.VISIBLE
                        bringToFront()
                        playAnimation()
                    }
                    coroutineScope.launch(Dispatchers.IO) {
                        delay(1000L)
                        withContext(Dispatchers.Main) {
                            // UI updates happen in onCSWindowCountChanged, so switch to Dispatchers.Main
                            csWindowManager.removeWindow(tag)
                            unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
                        }
                    }
                },
                onAfterMinimize = { unclosedMissionUiManager.notifyMissionMinimizedIfNeeded(MissionType.QUIZ) }
            )

            viewNetworkDisconnect.setOnClickListener {
                coroutineScope.launch(Dispatchers.Main) {
                    csWindowManager.bringWindowToTop(tag)
                    csWindowManager.bringWindowToTop(WindowTag.TOOLBAR)
                }
            }
        }
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            quizEditWindowModel.imageUploadSharedFlow.collect { isUploadSuccess ->
                when (isUploadSuccess) {
                    true -> {
                        withContext(Dispatchers.Main) {
                            with(binding.csImageUploadView) {
                                startProgressAnimation(fromPercentage = 90, toPercentage = 100)
                                withContext(Dispatchers.IO) { delay(500L) }
                                setMaskVisibility(false)
                                setCircleProgressbarVisibility(false)
                            }
                            enableSelectAgainButton()
                            enableStartQuizButton()
                        }
                    }

                    false -> {
                        withContext(Dispatchers.Main) {
                            binding.csImageUploadView.apply {
                                setCircleProgressbarVisibility(isShown = false)
                                setUploadFailedContainerVisibility(isShown = true)
                            }
                            enableSelectAgainButton()
                            disableStartQuizButton()
                        }
                    }
                }
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            quizCommonWindowModel.networkAvailabilityState.collect { hasNetwork ->
                withContext(Dispatchers.Main) {
                    binding.viewNetworkDisconnect.isVisible = !hasNetwork
                }
            }
        }
    }

    private fun showErrorToast(message: String) {
        coroutineScope.launch(Dispatchers.Main) {
            with(binding.cstToast) {
                setText(message)
                show()
            }
        }
    }

    private fun startQuizWindow() {
        csWindowManager.removeWindow(tag)
        val shortAnswerStartWindow: ShortAnswerStartWindow = get(ShortAnswerStartWindow::class.java)
        csWindowManager.createWindow(
            shortAnswerStartWindow,
            Gravity.CENTER
        )
    }

    private fun startCaptureScreenshot() {
        screenshotManager.startCaptureScreenshot(
            screenshotSource = AmplitudeConstant.EventProperties.Value.SELECT_AGAIN,
            onSuccess = {
                csWindowManager.removeWindow(tag)
                QuizSharedUiInfo.screenshotImageUri = screenshotManager.getScreenshotImageUri()
                QuizSharedUiInfo.setQuizTypeByTag(tag)
                csWindowManager.createWindow(get(ShortAnswerEditWindow::class.java), Gravity.CENTER)
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

    private suspend fun createQuiz(isNeedToStopPushAndRespond: Boolean = false) = withContext(Dispatchers.Main) {
        val isSuccessful = quizEditWindowModel.createQuiz(
            optionType = QuizOptionType.NO_OPTION,
            quizType = QuizType.SHORT_ANSWER,
            quizOptions = emptyList()
        )

        when (isSuccessful) {
            true -> {
                if (isNeedToStopPushAndRespond) {
                    unclosedMissionUiManager.closeMission(MissionType.PUSH_AND_RESPOND_TASK)
                }
                startQuizWindow()
            }
            false -> {
                if (isNeedToStopPushAndRespond) {
                    showErrorToast(applicationContext.getString(R.string.error_msg_end_task_and_start_quiz))
                } else {
                    showErrorToast(applicationContext.getString(R.string.quiz_error_msg_start_quiz))
                }
                binding.buttonStartQuiz.setEnable()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        quizEditWindowModel.onCleared()
        quizCommonWindowModel.removeOpenedQuizWindowTag(tag)
    }
}
