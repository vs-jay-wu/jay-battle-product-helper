package com.viewsonic.classswift.ui.window.quiz.mvb

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import com.viewsonic.classswift.R
import com.viewsonic.classswift.constant.AppConstants.ONE_SEC_DELAY
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.databinding.WindowMvbSketchResponseEditBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.ScreenshotManager
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.widget.quiz.MvbImageUploadView
import com.viewsonic.classswift.ui.window.ToastWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbSketchResponseStartWindow
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.MvbSketchResponseEditWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.MvbSketchResponseEditWindowModel.Event
import com.viewsonic.classswift.ui.windowmodel.quiz.MvbSketchResponseEditWindowModel.UiState
import com.viewsonic.classswift.ui.windowmodel.quiz.MvbSketchResponseEditWindowModel.UploadState
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
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

class MvbSketchResponseEditWindow(
    private val applicationContext: Context,
) : IWindow<WindowMvbSketchResponseEditBinding> {

    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val screenshotManager: ScreenshotManager by inject(ScreenshotManager::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    private val windowModel: MvbSketchResponseEditWindowModel by inject(MvbSketchResponseEditWindowModel::class.java)

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private val mvbImageUploadViewListener: MvbImageUploadView.Listener = object : MvbImageUploadView.Listener {
        override fun onTryAgainButtonClicked() {
            binding.miuvImageUploadView.apply {
                setUploadFailedContainerVisibility(isShown = false)
                setProgressbarVisibility(isShown = true)
                setCaptureAgainButtonEnabled(isEnabled = false)
                startProgressAnimation()
            }
            windowModel.retryUpload()
        }

        override fun onCaptureAgainButtonClicked() {
            windowModel.cleanupBeforeClose()
            startCaptureScreenshot()
        }
    }

    override var tag: WindowTag = WindowTag.MVB_SKETCH_RESPONSE_EDIT_QUIZ
    override var size: SizeInPixels = SizeInPixels(
        applicationContext.resources.getDimensionPixelSize(R.dimen.quiz_mvb_sketch_response_edit_window_width),
        applicationContext.resources.getDimensionPixelSize(R.dimen.quiz_mvb_sketch_response_edit_window_height),
    )

    override val binding: WindowMvbSketchResponseEditBinding = WindowMvbSketchResponseEditBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                applicationContext,
                com.google.android.material.R.style.Theme_MaterialComponents,
            ),
        ),
    )

    override fun onViewCreated() {
        super.onViewCreated()
        quizCommonWindowModel.addOpenedQuizWindowTag(tag)
        initUploadView()
        initButtons()
        observeUiState()
        observeEvents()
        startInitialUpload()
    }

    private fun initUploadView() {
        binding.miuvImageUploadView.apply {
            setImage(uri = quizCommonWindowModel.getScreenImageUri())
            setUploadFailedContainerVisibility(isShown = false)
            setMaskVisibility(isShown = true)
            setCaptureAgainButtonEnabled(isEnabled = false)
            setListener(mvbImageUploadViewListener)
        }
        binding.buttonStartQuestion.setDisable()
    }

    private fun initButtons() {
        binding.buttonStartQuestion.setOnCustomClickListener(object : OnLoadingButtonStateListener {
            override fun onEnableClicked() {
                binding.buttonStartQuestion.setLoading()
                windowModel.startQuestion()
            }
        })
        binding.buttonCancelQuestion.setOnCustomClickListener(object : OnLoadingButtonStateListener {
            override fun onEnableClicked() {
                showCloseConfirmDialog()
            }
        })
        binding.ivBtnClose.setOnClickListener { showCloseConfirmDialog() }
    }

    private fun startInitialUpload() {
        val uri = screenshotManager.getScreenshotImageUri()
        if (uri.isNotBlank()) {
            windowModel.startUpload(uri)
        }
    }

    private fun observeUiState() {
        coroutineScope.launch(Dispatchers.IO) {
            windowModel.uiState.collect { state ->
                withContext(Dispatchers.Main) { renderState(state) }
            }
        }
    }

    private fun renderState(state: UiState) {
        when (val uploadState = state.uploadState) {
            is UploadState.Idle, is UploadState.Loading -> {
                binding.miuvImageUploadView.apply {
                    setUploadFailedContainerVisibility(isShown = false)
                    setMaskVisibility(isShown = true)
                    setProgressbarVisibility(isShown = true)
                    setCaptureAgainButtonEnabled(isEnabled = false)
                }
                binding.buttonStartQuestion.setDisable()
            }
            is UploadState.Success -> {
                binding.miuvImageUploadView.apply {
                    setMaskVisibility(isShown = false)
                    setProgressbarVisibility(isShown = false)
                    setUploadFailedContainerVisibility(isShown = false)
                    setCaptureAgainButtonEnabled(isEnabled = true)
                }
                binding.buttonStartQuestion.setEnable()
            }
            is UploadState.Failed -> {
                binding.miuvImageUploadView.apply {
                    setProgressbarVisibility(isShown = false)
                    setUploadFailedContainerVisibility(isShown = true)
                    setCaptureAgainButtonEnabled(isEnabled = true)
                }
                binding.buttonStartQuestion.setDisable()
            }
        }
        renderDispatchLoading(state.isDispatchInFlight)
    }

    private fun renderDispatchLoading(isDispatchInFlight: Boolean) {
        // INVISIBLE (not GONE) so the button keeps its slot in the ConstraintLayout horizontal
        // chain; overlay overlaps the same constraint slot and shows the Lottie on top.
        binding.buttonStartQuestion.visibility = if (isDispatchInFlight) View.INVISIBLE else View.VISIBLE
        binding.flStartLoadingOverlay.visibility = if (isDispatchInFlight) View.VISIBLE else View.GONE
        if (isDispatchInFlight) {
            binding.lavStartLoading.playAnimation()
        } else {
            binding.lavStartLoading.cancelAnimation()
        }
    }

    private fun observeEvents() {
        coroutineScope.launch(Dispatchers.IO) {
            windowModel.events.collect { event ->
                withContext(Dispatchers.Main) { handleEvent(event) }
            }
        }
    }

    private fun handleEvent(event: Event) {
        when (event) {
            is Event.OpenStartWindow -> openStartWindow()
            is Event.ShowErrorToast -> showErrorToast(applicationContext.getString(event.messageResId))
        }
    }

    private fun openStartWindow() {
        windowModel.cleanupBeforeClose()
        csWindowManager.removeWindow(tag)
        val startWindow: MvbSketchResponseStartWindow =
            get(MvbSketchResponseStartWindow::class.java)
        csWindowManager.createWindow(startWindow, Gravity.CENTER)
    }

    private fun showCloseConfirmDialog() {
        binding.msdvCloseConfirm.setup(
            title = applicationContext.getString(R.string.quiz_disclose_close_confirm_title),
            message = applicationContext.getString(R.string.quiz_disclose_close_confirm_body),
            positiveText = applicationContext.getString(R.string.quiz_disclose_close_confirm_positive),
            onPositive = {
                binding.msdvCloseConfirm.dismiss()
                windowModel.cleanupBeforeClose()
                close()
            },
            negativeText = applicationContext.getString(R.string.quiz_disclose_close_confirm_negative),
        ).show()
    }

    private fun startCaptureScreenshot() {
        screenshotManager.startCaptureScreenshot(
            screenshotSource = screenshotManager.getScreenShotSource(tag),
            onSuccess = {
                csWindowManager.removeWindow(tag)
                QuizSharedUiInfo.screenshotImageUri = screenshotManager.getScreenshotImageUri()
                QuizSharedUiInfo.setQuizTypeByTag(tag)
                val nextWindow: MvbSketchResponseEditWindow = get(MvbSketchResponseEditWindow::class.java)
                csWindowManager.createWindow(nextWindow, Gravity.CENTER)
            },
            onFailed = {
                ToastWindow.MakeText(
                    applicationContext,
                    applicationContext.getString(R.string.quiz_error_msg_screenshot),
                    3000,
                ).build().show()
            },
            onCancel = {},
        )
    }

    private fun close() {
        binding.mlaLoading.apply {
            visibility = View.VISIBLE
            playAnimation()
        }
        coroutineScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) { delay(ONE_SEC_DELAY) }
            csWindowManager.removeWindow(tag)
            unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
        }
    }

    private fun showErrorToast(message: String) {
        coroutineScope.launch(Dispatchers.Main) {
            binding.buttonStartQuestion.setEnable()
            binding.mtToast.apply {
                setText(message)
                show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        quizCommonWindowModel.removeOpenedQuizWindowTag(tag)
        windowModel.onCleared()
        quizCommonWindowModel.onCleared()
    }
}
