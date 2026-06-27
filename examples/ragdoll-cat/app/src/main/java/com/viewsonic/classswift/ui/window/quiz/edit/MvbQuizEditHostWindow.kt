package com.viewsonic.classswift.ui.window.quiz.edit

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
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.quiz.QuizOption
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.databinding.WindowMvbQuizEditBinding
import com.viewsonic.classswift.feature.servicescreens.ui.EditImageState
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizEditScreen
import com.viewsonic.classswift.feature.servicescreens.ui.MvbQuizType
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.ScreenshotManager
import com.viewsonic.classswift.ui.window.ToastWindow
import com.viewsonic.classswift.ui.window.quiz.mvb.ComposeWindowHost
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizEditWindowModel
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.handleQuizStartWithOngoingMission
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import androidx.viewbinding.ViewBinding
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
 * Shared base for the CMP-ported quiz editors (TF / Short Answer / Audio image-only; MC / Poll add the
 * option panel). The editor card is the Compose [MvbQuizEditScreen] hosted in [WindowMvbQuizEditBinding];
 * the captured screenshot uploads via [QuizEditWindowModel] (the progress/uploaded/failed states drive
 * the Compose image area), and Start runs the existing ongoing-mission flow against the native
 * `CSMessageDialog` overlay before [createQuiz] → opening the matching START window. Subclasses provide
 * the per-type [editType], [buildCreateArgs], and the start / reopen window classes.
 */
abstract class MvbQuizEditHostWindow(protected val applicationContext: Context) : IWindow<WindowMvbQuizEditBinding> {

    protected val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    protected val screenshotManager: ScreenshotManager by inject(ScreenshotManager::class.java)
    protected val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    protected val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    protected val quizEditWindowModel: QuizEditWindowModel by inject(QuizEditWindowModel::class.java)

    protected val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val composeHost = ComposeWindowHost()
    private var progressJob: Job? = null

    /** Quiz type for the editor card (drives the option panel for MC/Poll). */
    protected abstract val editType: MvbQuizType

    /** `createQuiz` arguments for this type (read live for MC/Poll). */
    protected data class CreateArgs(
        val optionType: QuizOptionType,
        val quizType: QuizType,
        val options: List<QuizOption>,
        val saveOptions: Boolean = false,
    )
    protected abstract fun buildCreateArgs(): CreateArgs
    protected abstract val startWindowClass: Class<*>
    protected abstract val reopenSelfClass: Class<*>

    /** Option-panel config (MC/Poll override the initials from QuizSharedUiInfo + track changes). */
    protected open val initialOptionCount: Int = 4
    protected open val initialLetters: Boolean = true
    protected open val initialSingle: Boolean = true
    protected open fun onOptionConfig(count: Int, letters: Boolean, single: Boolean) {}

    override var size: SizeInPixels = SizeInPixels(541.33f.dpToPx().toInt(), 426.66f.dpToPx().toInt())
    override val binding: WindowMvbQuizEditBinding = WindowMvbQuizEditBinding.inflate(
        LayoutInflater.from(ContextThemeWrapper(applicationContext, com.google.android.material.R.style.Theme_MaterialComponents)),
    )

    protected data class Ui(
        val imageState: EditImageState = EditImageState.UPLOADING,
        val progress: Int = 0,
        val startEnabled: Boolean = false,
        val closeConfirm: Boolean = false,
        val closeLoading: Boolean = false,
        val errorToast: String? = null,
    )
    private val ui = MutableStateFlow(Ui())

    override fun onViewCreated() {
        composeHost.attach(binding.cvBody) { Content() }
        quizCommonWindowModel.addOpenedQuizWindowTag(tag)
        observeUpload()
        startUpload()
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
                type = editType,
                imageState = s.imageState,
                progress = s.progress,
                startEnabled = s.startEnabled,
                image = { m -> EditImage(m) },
                initialOptionCount = initialOptionCount,
                initialLetters = initialLetters,
                initialSingle = initialSingle,
                onOptionConfigChanged = { count, letters, single -> onOptionConfig(count, letters, single) },
                onClose = { showCloseConfirm() },
                onMinimize = {
                    csWindowManager.minimizeWindow(tag)
                    unclosedMissionUiManager.notifyMissionMinimizedIfNeeded(MissionType.QUIZ)
                },
                onCaptureAgain = { captureAgain() },
                onTryAgain = { retryUpload() },
                onCancel = { showCloseConfirm() },
                onStart = { onStartClicked() },
            )
            s.errorToast?.let { msg ->
                Box(Modifier.align(Alignment.BottomCenter).padding(16.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF02B2B)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OT(msg, Color.White, 12.sp)
                }
            }
            if (s.closeConfirm) CloseConfirmDialog()
            if (s.closeLoading) {
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

    /** The captured screenshot (`getScreenImageUri`) via Coil; shown in the image area's slot. */
    @Composable
    private fun EditImage(modifier: Modifier) {
        val uri = quizCommonWindowModel.getScreenImageUri()
        if (uri.isNotBlank()) AsyncImage(model = uri, contentDescription = null, modifier = modifier)
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
                        modifier = Modifier.clickable { ui.update { it.copy(closeConfirm = false) }; close() }.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }

    // ─── Upload ─────────────────────────────────────────────────────────────────

    private fun startUpload() {
        ui.update { it.copy(imageState = EditImageState.UPLOADING, progress = 0, startEnabled = false) }
        animateProgress(0, 90)
        quizEditWindowModel.startUploadImage(screenshotManager.getScreenshotImageUri())
    }

    private fun retryUpload() = startUpload()

    private fun observeUpload() {
        coroutineScope.launch(Dispatchers.IO) {
            quizEditWindowModel.imageUploadSharedFlow.collect { success ->
                withContext(Dispatchers.Main) {
                    if (success) {
                        animateProgress(90, 100)
                        delay(300L)
                        ui.update { it.copy(imageState = EditImageState.UPLOADED, progress = 100, startEnabled = true) }
                    } else {
                        progressJob?.cancel()
                        ui.update { it.copy(imageState = EditImageState.FAILED, startEnabled = false) }
                    }
                }
            }
        }
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

    // ─── Start / capture / close ──────────────────────────────────────────────────

    private fun onStartClicked() {
        ui.update { it.copy(startEnabled = false) }
        binding.csMessageDialog.handleQuizStartWithOngoingMission(
            coroutineScope = coroutineScope,
            unclosedMissionUiManager = unclosedMissionUiManager,
            onStartQuiz = { hasPushAndRespond -> createQuiz(hasPushAndRespond) },
            onCanceled = { ui.update { it.copy(startEnabled = true) } },
            onBatchCloseFailed = {
                showErrorToast(applicationContext.getString(R.string.quiz_error_msg_ongoing_quiz))
                ui.update { it.copy(startEnabled = true) }
            },
            onMaskClicked = { csWindowManager.bringWindowToTop(windowTag = tag) },
        )
    }

    private suspend fun createQuiz(isNeedToStopPushAndRespond: Boolean) = withContext(Dispatchers.Main) {
        val args = buildCreateArgs()
        val isSuccessful = quizEditWindowModel.createQuiz(args.optionType, args.quizType, args.options)
        if (isSuccessful) {
            if (isNeedToStopPushAndRespond) unclosedMissionUiManager.closeMission(MissionType.PUSH_AND_RESPOND_TASK)
            if (args.saveOptions) {
                QuizSharedUiInfo.quizOptionType = args.optionType
                QuizSharedUiInfo.quizOptionCount = args.options.size
                quizEditWindowModel.saveMultipleOptionInfos()
            }
            openStartWindow()
        } else {
            val msg = if (isNeedToStopPushAndRespond) R.string.error_msg_end_task_and_start_quiz else R.string.quiz_error_msg_start_quiz
            showErrorToast(applicationContext.getString(msg))
            ui.update { it.copy(startEnabled = true) }
        }
    }

    private fun openStartWindow() {
        csWindowManager.removeWindow(tag)
        @Suppress("UNCHECKED_CAST")
        csWindowManager.createWindow(get<Any>(startWindowClass) as IWindow<ViewBinding>, Gravity.CENTER)
    }

    private fun captureAgain() {
        screenshotManager.startCaptureScreenshot(
            screenshotSource = AmplitudeConstant.EventProperties.Value.SELECT_AGAIN,
            onSuccess = {
                csWindowManager.removeWindow(tag)
                QuizSharedUiInfo.screenshotImageUri = screenshotManager.getScreenshotImageUri()
                QuizSharedUiInfo.setQuizTypeByTag(tag)
                @Suppress("UNCHECKED_CAST")
                csWindowManager.createWindow(get<Any>(reopenSelfClass) as IWindow<ViewBinding>, Gravity.CENTER)
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
            withContext(Dispatchers.IO) { delay(1000L) }
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
        quizEditWindowModel.onCleared()
        quizCommonWindowModel.removeOpenedQuizWindowTag(tag)
        quizCommonWindowModel.onCleared()
        coroutineScope.cancel()
    }
}
