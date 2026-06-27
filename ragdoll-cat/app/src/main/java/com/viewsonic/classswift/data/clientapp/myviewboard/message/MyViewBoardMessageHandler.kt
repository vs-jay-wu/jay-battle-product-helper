package com.viewsonic.classswift.data.clientapp.myviewboard.message

import android.content.Context
import androidx.viewbinding.ViewBinding
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.data.clientapp.myviewboard.event.MyViewBoardEvent
import com.viewsonic.classswift.data.clientapp.myviewboard.session.MyViewBoardSessionStore
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.MvbToolbarStateManager
import com.viewsonic.classswift.manager.PendingClassEntryWindowManager
import com.viewsonic.classswift.manager.ScreenshotManager
import com.viewsonic.classswift.service.ClassSwiftService
import com.viewsonic.classswift.ui.helper.JoinClassWindowOpener
import com.viewsonic.classswift.ui.helper.MvbSpinnerWindowOpener
import com.viewsonic.classswift.ui.window.JoinClassWindow
import com.viewsonic.classswift.ui.window.quiz.mvb.MvbQuizCollectionWindow
import com.viewsonic.classswift.ui.window.ToastWindow
import com.viewsonic.classswift.ui.window.quiz.edit.MvbAudioQuizEditWindow
import com.viewsonic.classswift.ui.window.quiz.edit.MvbMultipleChoiceEditWindow
import com.viewsonic.classswift.ui.window.quiz.edit.MvbPollQuizEditWindow
import com.viewsonic.classswift.ui.window.quiz.edit.MvbShortAnswerEditWindow
import com.viewsonic.classswift.ui.window.quiz.edit.MvbTrueFalseEditWindow
import com.viewsonic.classswift.ui.window.quiz.mvb.MvbSketchResponseEditWindow
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import kotlin.getValue

class MyViewBoardMessageHandler(
    private val sessionStore: MyViewBoardSessionStore = MyViewBoardSessionStore
) {
    private val accountManager: AccountManager by inject(AccountManager::class.java)
    private val classroomManager: ClassroomManager by inject(ClassroomManager::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val screenshotManager: ScreenshotManager by inject(ScreenshotManager::class.java)
    private val mvbToolbarStateManager: MvbToolbarStateManager by inject(MvbToolbarStateManager::class.java)
    private val pendingClassEntryWindowManager: PendingClassEntryWindowManager by inject(PendingClassEntryWindowManager::class.java)
    private val context: Context by inject(Context::class.java)
    suspend fun handle(message: MyViewBoardMessage): MyViewBoardMessageResult? {
        return when (message) {
            is MyViewBoardMessage.NotHandled -> null
            is MyViewBoardMessage.Invalid -> MyViewBoardMessageResult(
                requestId = message.requestId,
                responseTo = message.responseTo,
                status = MyViewBoardEvent.STATUS_FAILED,
                reasonCode = message.reasonCode,
                reasonMessage = message.reasonMessage
            )
            is MyViewBoardMessage.MvbToken -> {
                sessionStore.updateToken(message.token)
                MyViewBoardMessageResult(
                    requestId = message.requestId,
                    responseTo = MyViewBoardResponseTo.MvbToken,
                    status = MyViewBoardEvent.STATUS_SUCCESS
                )
            }
            is MyViewBoardMessage.MvbVisibility -> {
                // Handle MyViewBoard window visibility messages by showing or hiding all ClassSwift windows.
                if (!ClassSwiftService.isServiceStarted()) {
                    createServiceNotStartedResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.MvbVisibility
                    )
                } else {
                    withContext(Dispatchers.Main) {
                        when (message.action) {
                            MyViewBoardMessage.MvbVisibility.Action.AllShow -> {
                                CSWindowManager.showAllWindows()
                            }

                            MyViewBoardMessage.MvbVisibility.Action.AllHide -> {
                                CSWindowManager.hideAllWindows(emptyList())
                            }
                        }
                    }
                    MyViewBoardMessageResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.MvbVisibility,
                        status = MyViewBoardEvent.STATUS_SUCCESS
                    )
                }
            }
            is MyViewBoardMessage.MinimizeAllWindows -> {
                if (!ClassSwiftService.isServiceStarted()) {
                    createServiceNotStartedResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.MinimizeAllWindows
                    )
                } else {
                    withContext(Dispatchers.Main) {
                        // Product decision (current scope): JOIN_CLASS is exempt from MinimizeAllWindows;
                        // other windows still minimize as default.
                        CSWindowManager.minimizeAllWindows(
                            exemptionList = listOf(WindowTag.JOIN_CLASS)
                        )
                    }
                    unclosedMissionUiManager.getUnclosedMissions().forEach {
                        unclosedMissionUiManager.notifyMissionMinimizedIfNeeded(it)
                    }
                    MyViewBoardMessageResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.MinimizeAllWindows,
                        status = MyViewBoardEvent.STATUS_SUCCESS
                    )
                }
            }
            is MyViewBoardMessage.OpenWindow -> {
                val windowTag = parseWindowTag(message.windowTag)
                    ?: return MyViewBoardMessageResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.OpenWindow,
                        status = MyViewBoardEvent.STATUS_FAILED,
                        reasonCode = MyViewBoardEvent.REASON_CODE_UNKNOWN_WINDOW_TAG,
                        reasonMessage = "window_tag is unknown."
                    )
                if (!ClassSwiftService.isServiceStarted()) {
                    createServiceNotStartedResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.OpenWindow
                    )
                } else {
                    val isOpened = withContext(Dispatchers.Main) {
                        openWindow(windowTag)
                    }
                    if (isOpened) {
                        MyViewBoardMessageResult(
                            requestId = message.requestId,
                            responseTo = MyViewBoardResponseTo.OpenWindow,
                            status = MyViewBoardEvent.STATUS_SUCCESS
                        )
                    } else {
                        MyViewBoardMessageResult(
                            requestId = message.requestId,
                            responseTo = MyViewBoardResponseTo.OpenWindow,
                            status = MyViewBoardEvent.STATUS_FAILED,
                            reasonCode = MyViewBoardEvent.REASON_CODE_OPEN_WINDOW_FAILED,
                            reasonMessage = "Failed to open window."
                        )
                    }
                }
            }
            is MyViewBoardMessage.OpenWindowAfterClassEntry -> {
                val windowTag = parseWindowTag(message.windowTag)
                    ?: return MyViewBoardMessageResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.OpenWindowAfterClassEntry,
                        status = MyViewBoardEvent.STATUS_FAILED,
                        reasonCode = MyViewBoardEvent.REASON_CODE_UNKNOWN_WINDOW_TAG,
                        reasonMessage = "window_tag is unknown."
                    )
                if (!ClassSwiftService.isServiceStarted()) {
                    createServiceNotStartedResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.OpenWindowAfterClassEntry
                    )
                } else {
                    val handled = withContext(Dispatchers.Main) {
                        // 「是否在課堂中」判斷：
                        // 1. 任何選班視窗開著 → 一定「未進班」，無視 lessonId。
                        //    switch class 走 endLesson()，但 endLesson() 不會清掉 selectedClassroomInfo.lessonId,
                        //    僅靠 lessonId 會把「switch class 後在 SelectOrg 選班 + toggle off + spinner 重啟」誤判成已在班。
                        // 2. 沒在選班 + (lessonId 非空 || JoinClass 在 windowMap) → 在班。
                        //    handleCreateLessonSuccess 進班時立即設定 lessonId，不依賴 socket 把 status 同步成 "in_class"。
                        //    JoinClass 存在作 fallback，覆蓋 on→off (all_hide)、手動關 JoinClass、spinner-launch 刻意 suppress JoinClass 等情境。
                        val lessonId = classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId
                        val joinClassExisted = CSWindowManager.isWindowExisted(WindowTag.JOIN_CLASS)
                        val isInSelectionMode = CSWindowManager.isWindowExisted(WindowTag.CS_SELECT_ORG_AND_CLASS) ||
                            CSWindowManager.isWindowExisted(WindowTag.CS_SELECT_ORG) ||
                            CSWindowManager.isWindowExisted(WindowTag.CS_SELECT_MY_CLASS)
                        val isInClass = !isInSelectionMode && (lessonId.isNotEmpty() || joinClassExisted)
                        // Gate the raw lessonId behind BuildConfig.DEBUG: lessonId is a session
                        // identifier (kotlinsecurity:S7610 sensitive in prod). Debug-only builds
                        // keep it for trace correlation; release builds emit nothing.
                        if (BuildConfig.DEBUG) {
                            Timber.d("[OpenWindowAfterClassEntry] windowTag=$windowTag isInClass=$isInClass lessonId=$lessonId joinClassExisted=$joinClassExisted isInSelectionMode=$isInSelectionMode")
                        }
                        if (isInClass) {
                            // 已在課堂中 → 立即開，JoinClass 顯示與否由該視窗自身決定
                            openWindow(windowTag)
                        } else {
                            // 尚未進班 → 暫存「進班後要做什麼」的 closure，待 SelectOrgAndSelectClassWindowModel 進班成功時 invoke。
                            // bound 狀態在 consume 當下才重判，避免 IPC 收到後到實際進班之間 mVB unbind 卻仍開 spinner 無法操作。
                            pendingClassEntryWindowManager.set {
                                if (windowTag == WindowTag.MVB_SPINNER && ClassSwiftService.isMyViewBoardBound()) {
                                    MvbSpinnerWindowOpener.open()
                                } else {
                                    JoinClassWindowOpener.open(get(JoinClassWindow::class.java))
                                }
                            }
                            true
                        }
                    }
                    MyViewBoardMessageResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.OpenWindowAfterClassEntry,
                        status = if (handled) {
                            MyViewBoardEvent.STATUS_SUCCESS
                        } else {
                            MyViewBoardEvent.STATUS_FAILED
                        }
                    )
                }
            }
            is MyViewBoardMessage.StartQuiz -> {
                if (!ClassSwiftService.isServiceStarted()) {
                    createServiceNotStartedResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.StartQuiz
                    )
                } else {
                    val isStarted = withContext(Dispatchers.Main) {
                        startQuiz(message.mvbQuizType)
                    }
                    MyViewBoardMessageResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.StartQuiz,
                        status = if (isStarted) {
                            MyViewBoardEvent.STATUS_SUCCESS
                        } else {
                            MyViewBoardEvent.STATUS_FAILED
                        }
                    )
                }
            }
            is MyViewBoardMessage.BringOngoingMissionToTop -> {
                if (!ClassSwiftService.isServiceStarted()) {
                    createServiceNotStartedResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.BringOngoingMissionToTop
                    )
                } else {
                    val isSucceeded = unclosedMissionUiManager.bringOngoingMissionToTop()
                    MyViewBoardMessageResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.BringOngoingMissionToTop,
                        status = if (isSucceeded) {
                            MyViewBoardEvent.STATUS_SUCCESS
                        } else {
                            MyViewBoardEvent.STATUS_FAILED
                        }
                    )
                }
            }
            is MyViewBoardMessage.MvbClosed -> {
                // Handle MyViewBoard App closed commands to end lesson and stop service.
                if (!ClassSwiftService.isServiceStarted()) {
                    createServiceNotStartedResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.MvbClosed
                    )
                } else {
                    pendingClassEntryWindowManager.clear()
                    classroomManager.endLesson()
                    accountManager.quitApp(false)

                    MyViewBoardMessageResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.MvbClosed,
                        status = MyViewBoardEvent.STATUS_SUCCESS,
                    )
                }
            }
            is MyViewBoardMessage.MvbUserSignOut -> {
                // Handle MyViewBoard App closed commands to end lesson and stop service.
                if (!ClassSwiftService.isServiceStarted()) {
                    createServiceNotStartedResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.MvbUserSignOut
                    )
                } else {
                    pendingClassEntryWindowManager.clear()
                    classroomManager.endLesson()
                    accountManager.quitApp(false)

                    MyViewBoardMessageResult(
                        requestId = message.requestId,
                        responseTo = MyViewBoardResponseTo.MvbUserSignOut,
                        status = MyViewBoardEvent.STATUS_SUCCESS,
                    )
                }
            }
            is MyViewBoardMessage.ToolbarPositionChanged -> {
                // Record the latest mVB main toolbar position (and, if provided,
                // the whiteboard top edge for window alignment). Listeners
                // (e.g., Join Class window) react via the manager's StateFlow.
                // No service-started gate: it's safe to record before windows exist.
                mvbToolbarStateManager.update(
                    newPosition = message.position,
                    newWhiteboardTopDp = message.whiteboardTopDp
                )
                MyViewBoardMessageResult(
                    requestId = message.requestId,
                    responseTo = MyViewBoardResponseTo.ToolbarPositionChanged,
                    status = MyViewBoardEvent.STATUS_SUCCESS
                )
            }
        }
    }

    private suspend fun openWindow(windowTag: WindowTag): Boolean {
        return when (windowTag) {
            WindowTag.JOIN_CLASS -> {
                if (CSWindowManager.isWindowExisted(windowTag)) {
                    CSWindowManager.showWindow(windowTag)
                    CSWindowManager.bringWindowToTop(windowTag)
                    true
                } else {
                    JoinClassWindowOpener.open(get(JoinClassWindow::class.java))
                }
            }

            WindowTag.WINDOW_QUIZ_COLLECTION,
            WindowTag.WINDOW_MVB_QUIZ_COLLECTION -> {
                // The MVB IPC contract dictates QC is opened from MVB only when bound; defend against
                // a future Flutter regression that sends OPEN(QC) while unbound by refusing here.
                if (!ClassSwiftService.isMyViewBoardBound()) {
                    Timber.w("[MyViewBoardMessageHandler] OPEN($windowTag) while MVB unbound — ignored")
                    return false
                }
                val activeTag = WindowTag.WINDOW_MVB_QUIZ_COLLECTION
                if (CSWindowManager.isWindowExisted(activeTag)) {
                    CSWindowManager.showWindow(activeTag)
                    CSWindowManager.bringWindowToTop(activeTag)
                    true
                } else {
                    CSWindowManager.createWindow(get(MvbQuizCollectionWindow::class.java), Gravity.CENTER)
                }
            }

            WindowTag.MVB_SPINNER -> {
                if (!ClassSwiftService.isMyViewBoardBound()) {
                    Timber.w("[MyViewBoardMessageHandler] OPEN($windowTag) while MVB unbound — ignored")
                    return false
                }
                MvbSpinnerWindowOpener.open()
            }

            else -> false
        }
    }

    private fun parseWindowTag(windowTag: String): WindowTag? {
        return runCatching {
            WindowTag.valueOf(windowTag)
        }.getOrNull()
    }

    private fun createServiceNotStartedResult(
        requestId: String,
        responseTo: String
    ) = MyViewBoardMessageResult(
        requestId = requestId,
        responseTo = responseTo,
        status = MyViewBoardEvent.STATUS_FAILED,
        reasonCode = MyViewBoardEvent.REASON_CODE_SERVICE_NOT_STARTED,
        reasonMessage = "Service is not started."
    )

    private suspend fun startQuiz(quizType: MyViewBoardMessage.StartQuiz.MvbQuizType): Boolean {
        return when (quizType) {
            MyViewBoardMessage.StartQuiz.MvbQuizType.TRUE_FALSE -> {
                startCaptureScreenshot(WindowTag.MVB_TRUE_FALSE_EDIT_QUIZ)
                true
            }

            MyViewBoardMessage.StartQuiz.MvbQuizType.MULTIPLE_SELECTION -> {
                startCaptureScreenshot(WindowTag.MVB_MULTIPLE_CHOICE_EDIT_QUIZ)
                true
            }

            MyViewBoardMessage.StartQuiz.MvbQuizType.AUDIO -> {
                startCaptureScreenshot(WindowTag.MVB_AUDIO_EDIT_QUIZ)
                true
            }

            MyViewBoardMessage.StartQuiz.MvbQuizType.SHORT_ANSWER -> {
                startCaptureScreenshot(WindowTag.MVB_SHORT_ANSWER_EDIT_QUIZ)
                true
            }

            MyViewBoardMessage.StartQuiz.MvbQuizType.POLL -> {
                startCaptureScreenshot(WindowTag.MVB_POLL_EDIT_QUIZ)
                true
            }

            MyViewBoardMessage.StartQuiz.MvbQuizType.SKETCH_RESPONSE -> {
                startCaptureScreenshot(WindowTag.MVB_SKETCH_RESPONSE_EDIT_QUIZ)
                true
            }
        }
    }

    private fun startCaptureScreenshot(destinationWindowTag: WindowTag) {
        screenshotManager.startCaptureScreenshot(
            screenshotSource = screenshotManager.getScreenShotSource(destinationWindowTag),
            onSuccess = {
                CSWindowManager.removeWindow(destinationWindowTag)
                QuizSharedUiInfo.screenshotImageUri = screenshotManager.getScreenshotImageUri()
                QuizSharedUiInfo.setQuizTypeByTag(destinationWindowTag)
                getQuizEditWindow(destinationWindowTag)?.let { window ->
                    CSWindowManager.createWindow(window, Gravity.CENTER)
                    unclosedMissionUiManager.notifyMissionOngoingIfNeeded(MissionType.QUIZ)
                }
            },
            onFailed = {
                ToastWindow.MakeText(
                    context,
                    context.getString(com.viewsonic.classswift.R.string.quiz_error_msg_screenshot),
                    3000
                ).build().show()
            },
            onCancel = {}
        )
    }

    private fun getQuizEditWindow(tag: WindowTag): IWindow<ViewBinding>? = when (tag) {
        WindowTag.MVB_TRUE_FALSE_EDIT_QUIZ -> get(MvbTrueFalseEditWindow::class.java)
        WindowTag.MVB_MULTIPLE_CHOICE_EDIT_QUIZ -> get(MvbMultipleChoiceEditWindow::class.java)
        WindowTag.MVB_POLL_EDIT_QUIZ -> get(MvbPollQuizEditWindow::class.java)
        WindowTag.MVB_SHORT_ANSWER_EDIT_QUIZ -> get(MvbShortAnswerEditWindow::class.java)
        WindowTag.MVB_SKETCH_RESPONSE_EDIT_QUIZ -> get(MvbSketchResponseEditWindow::class.java)
        WindowTag.MVB_AUDIO_EDIT_QUIZ -> get(MvbAudioQuizEditWindow::class.java)
        else -> null
    }
}
