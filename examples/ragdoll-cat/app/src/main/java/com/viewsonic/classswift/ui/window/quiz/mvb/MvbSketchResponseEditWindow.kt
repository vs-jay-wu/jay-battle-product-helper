package com.viewsonic.classswift.ui.window.quiz.mvb

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.viewsonic.classswift.R
import com.viewsonic.classswift.constant.AppConstants.ONE_SEC_DELAY
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.databinding.WindowMvbQuizEditBinding
import com.viewsonic.classswift.feature.servicescreens.ui.EditImageState
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizEditScreen
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizType
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.ScreenshotManager
import com.viewsonic.classswift.ui.window.ToastWindow
import com.viewsonic.classswift.ui.window.quiz.start.MvbSketchResponseStartWindow
import com.viewsonic.classswift.ui.windowmodel.quiz.MvbSketchResponseEditWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.MvbSketchResponseEditWindowModel.Event
import com.viewsonic.classswift.ui.windowmodel.quiz.MvbSketchResponseEditWindowModel.UiState
import com.viewsonic.classswift.ui.windowmodel.quiz.MvbSketchResponseEditWindowModel.UploadState
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject

/**
 * MvbSketchResponseEditWindow — CMP port (hybrid). Same [MvbSketchResponseEditWindowModel] upload/
 * dispatch flow as before, but the editor card is the Compose [MvbQuizEditScreen] (type = SKETCH,
 * image-only) hosted in [WindowMvbQuizEditBinding]. Sketch dispatch is a single [startQuestion] (no
 * ongoing-mission dialog); the model's [UploadState] drives the Compose image area and its events open
 * the START window / show errors. Close-confirm / toast / loading are Compose.
 */
class MvbSketchResponseEditWindow(private val applicationContext: Context) : IWindow<WindowMvbQuizEditBinding> {

    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val screenshotManager: ScreenshotManager by inject(ScreenshotManager::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    private val windowModel: MvbSketchResponseEditWindowModel by inject(MvbSketchResponseEditWindowModel::class.java)

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val composeHost = ComposeWindowHost()
    private var progressJob: Job? = null

    override var tag: WindowTag = WindowTag.MVB_SKETCH_RESPONSE_EDIT_QUIZ
    override var size: SizeInPixels = SizeInPixels(541.33f.dpToPx().toInt(), 426.66f.dpToPx().toInt())
    override val binding: WindowMvbQuizEditBinding = WindowMvbQuizEditBinding.inflate(
        LayoutInflater.from(ContextThemeWrapper(applicationContext, com.google.android.material.R.style.Theme_MaterialComponents)),
    )

    private data class Ui(
        val imageState: EditImageState = EditImageState.UPLOADING,
        val progress: Int = 0,
        val startEnabled: Boolean = false,
        val closeConfirm: Boolean = false,
        val dispatchLoading: Boolean = false,
        val closeLoading: Boolean = false,
        val errorToast: String? = null,
    )
    private val ui = MutableStateFlow(Ui())

    override fun onViewCreated() {
        composeHost.attach(binding.cvBody) { Content() }
        quizCommonWindowModel.addOpenedQuizWindowTag(tag)
        observeUiState()
        observeEvents()
        startInitialUpload()
    }

    @Composable
    private fun OT(text: String, color: Color, size: TextUnit, weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier) {
        BasicText(text, modifier = modifier, style = TextStyle(color = color, fontSize = size, fontWeight = weight))
    }

    @Composable
    private fun Content() {
        val s by ui.collectAsState()
        Box(Modifier.fillMaxSize()) {
            MvbQuizEditScreen(
                type = MvbQuizType.SKETCH,
                imageState = s.imageState,
                progress = s.progress,
                startEnabled = s.startEnabled,
                image = { m ->
                    val uri = quizCommonWindowModel.getScreenImageUri()
                    if (uri.isNotBlank()) AsyncImage(model = uri, contentDescription = null, modifier = m)
                },
                onClose = { showCloseConfirm() },
                onMinimize = {
                    csWindowManager.minimizeWindow(tag)
                    unclosedMissionUiManager.notifyMissionMinimizedIfNeeded(MissionType.QUIZ)
                },
                onCaptureAgain = { captureAgain() },
                onTryAgain = { retryUpload() },
                onCancel = { showCloseConfirm() },
                onStart = {
                    ui.update { it.copy(startEnabled = false) }
                    windowModel.startQuestion()
                },
            )
            s.errorToast?.let { msg ->
                Box(Modifier.align(Alignment.BottomCenter).padding(16.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF02B2B)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OT(msg, Color.White, 12.sp)
                }
            }
            if (s.closeConfirm) CloseConfirmDialog()
            if (s.dispatchLoading || s.closeLoading) {
                Box(Modifier.fillMaxSize().padding(8.dp).clip(RoundedCornerShape(5.33.dp)).background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                    AndroidView(factory = { ctx ->
                        LottieAnimationView(ctx).apply {
                            setAnimation("ani_loading.json")
                            repeatCount = LottieDrawable.INFINITE
                            playAnimation()
                        }
                    }, modifier = Modifier.width(45.dp))
                }
            }
        }
    }

    @Composable
    private fun CloseConfirmDialog() {
        Box(Modifier.fillMaxSize().padding(8.dp).clip(RoundedCornerShape(5.33.dp)).background(Color(0x59000000)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
            Column(Modifier.width(360.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(24.dp)) {
                OT(applicationContext.getString(R.string.quiz_disclose_close_confirm_title), Color(0xFF2E3133), 18.sp, FontWeight.Bold)
                OT(applicationContext.getString(R.string.quiz_disclose_close_confirm_body), Color(0xFF2E3133), 14.sp, modifier = Modifier.padding(top = 12.dp))
                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.End) {
                    OT(
                        applicationContext.getString(R.string.quiz_disclose_close_confirm_negative), Color(0xFF5C6266), 14.sp, FontWeight.Medium,
                        modifier = Modifier.clickable { ui.update { it.copy(closeConfirm = false) } }.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    OT(
                        applicationContext.getString(R.string.quiz_disclose_close_confirm_positive), Color(0xFFF02B2B), 14.sp, FontWeight.Bold,
                        modifier = Modifier.clickable { ui.update { it.copy(closeConfirm = false) }; windowModel.cleanupBeforeClose(); close() }.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }

    private fun startInitialUpload() {
        val uri = screenshotManager.getScreenshotImageUri()
        ui.update { it.copy(imageState = EditImageState.UPLOADING, progress = 0, startEnabled = false) }
        animateProgress(0, 90)
        if (uri.isNotBlank()) windowModel.startUpload(uri)
    }

    private fun retryUpload() {
        ui.update { it.copy(imageState = EditImageState.UPLOADING, progress = 0, startEnabled = false) }
        animateProgress(0, 90)
        windowModel.retryUpload()
    }

    private fun observeUiState() {
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.uiState.collect { state -> renderState(state) }
        }
    }

    private fun renderState(state: UiState) {
        when (state.uploadState) {
            is UploadState.Idle, is UploadState.Loading -> Unit // progress animates locally
            is UploadState.Success -> {
                animateProgress(90, 100)
                ui.update { it.copy(imageState = EditImageState.UPLOADED, progress = 100, startEnabled = true) }
            }
            is UploadState.Failed -> {
                progressJob?.cancel()
                ui.update { it.copy(imageState = EditImageState.FAILED, startEnabled = false) }
            }
        }
        ui.update { it.copy(dispatchLoading = state.isDispatchInFlight) }
    }

    private fun animateProgress(from: Int, to: Int) {
        progressJob?.cancel()
        progressJob = coroutineScope.launch(Dispatchers.Main) {
            for (p in from..to) {
                ui.update { it.copy(progress = p) }
                delay(100L)
            }
        }
    }

    private fun observeEvents() {
        coroutineScope.launch(Dispatchers.Main) {
            windowModel.events.collect { event ->
                when (event) {
                    is Event.OpenStartWindow -> openStartWindow()
                    is Event.ShowErrorToast -> {
                        ui.update { it.copy(startEnabled = true) }
                        showErrorToast(applicationContext.getString(event.messageResId))
                    }
                }
            }
        }
    }

    private fun openStartWindow() {
        windowModel.cleanupBeforeClose()
        csWindowManager.removeWindow(tag)
        csWindowManager.createWindow(get<MvbSketchResponseStartWindow>(MvbSketchResponseStartWindow::class.java), Gravity.CENTER)
    }

    private fun captureAgain() {
        windowModel.cleanupBeforeClose()
        screenshotManager.startCaptureScreenshot(
            screenshotSource = screenshotManager.getScreenShotSource(tag),
            onSuccess = {
                csWindowManager.removeWindow(tag)
                QuizSharedUiInfo.screenshotImageUri = screenshotManager.getScreenshotImageUri()
                QuizSharedUiInfo.setQuizTypeByTag(tag)
                csWindowManager.createWindow(get<MvbSketchResponseEditWindow>(MvbSketchResponseEditWindow::class.java), Gravity.CENTER)
            },
            onFailed = {
                ToastWindow.MakeText(applicationContext, applicationContext.getString(R.string.quiz_error_msg_screenshot), 3000).build().show()
            },
            onCancel = {},
        )
    }

    private fun showCloseConfirm() {
        ui.update { it.copy(closeConfirm = true) }
    }

    private fun close() {
        ui.update { it.copy(closeLoading = true) }
        coroutineScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) { delay(ONE_SEC_DELAY) }
            csWindowManager.removeWindow(tag)
            unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
        }
    }

    private fun showErrorToast(message: String) {
        coroutineScope.launch(Dispatchers.Main) {
            ui.update { it.copy(errorToast = message) }
            withContext(Dispatchers.IO) { delay(3000L) }
            ui.update { it.copy(errorToast = null) }
        }
    }

    override fun onDestroy() {
        composeHost.destroy()
        progressJob?.cancel()
        quizCommonWindowModel.removeOpenedQuizWindowTag(tag)
        windowModel.onCleared()
        quizCommonWindowModel.onCleared()
        coroutineScope.cancel()
    }
}
