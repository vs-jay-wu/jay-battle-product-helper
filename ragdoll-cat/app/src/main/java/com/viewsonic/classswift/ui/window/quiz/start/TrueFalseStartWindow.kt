package com.viewsonic.classswift.ui.window.quiz.start

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.body.UpdateQuizStatusType
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.info.QuizAnsweringInfo
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.databinding.WindowTrueFalseStartQuizBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.widget.quiz.AnswerOptionEventListener
import com.viewsonic.classswift.ui.widget.quiz.RandomDrawWidget
import com.viewsonic.classswift.ui.widget.quiz.SingleMultipleAnswerButtonGroup.OnChangeDiscloseAnswerButtonListener
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState
import com.viewsonic.classswift.ui.window.CSSystemDialogWindow
import com.viewsonic.classswift.ui.window.adapter.QuizAnswerResultAdapter
import com.viewsonic.classswift.ui.window.adapter.QuizAnsweringAdapter
import com.viewsonic.classswift.ui.window.decoration.StudentAnswerResultItemDecoration
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizStartWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.TrueFalseWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.TrueFalseWindowModel.Companion.FALSE_OPTION_INDEX
import com.viewsonic.classswift.ui.windowmodel.quiz.TrueFalseWindowModel.Companion.TRUE_OPTION_INDEX
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.EyeState
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.QuizState
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.SpannableStringUtils
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import kotlin.getValue

class TrueFalseStartWindow(
    private val context: Context
) : IWindow<WindowTrueFalseStartQuizBinding>, RandomDrawWidget.RandomDrawViewListener {
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    private val quizStartWindowModel: QuizStartWindowModel by inject(QuizStartWindowModel::class.java)
    private val trueFalseWindowModel: TrueFalseWindowModel by inject(TrueFalseWindowModel::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var quizCloseJob: Job? = null
    private var quizCancelJob: Job? = null
    private var dialogWindow: CSSystemDialogWindow? = null
    private var closeStatus = UpdateQuizStatusType.CANCEL
    private var eyeStatus: EyeState = EyeState.CLOSED

    private lateinit var studentQuizzingListAdapter: QuizAnsweringAdapter
    private lateinit var studentQuizAnswerResultAdapter: QuizAnswerResultAdapter

    override var tag: WindowTag = WindowTag.TRUE_FALSE_START_QUIZ
    override var size: SizeInPixels = SizeInPixels(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT
    )

    override val binding: WindowTrueFalseStartQuizBinding = WindowTrueFalseStartQuizBinding.inflate(
        LayoutInflater.from(
            ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Theme_MaterialComponents
            )
        )
    )

    override fun getCurrentSize(): SizeInPixels {
        if (binding.root.width <= 0 || binding.root.height <= 0) {
            binding.root.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            return SizeInPixels(binding.root.measuredWidth, binding.root.measuredHeight)
        }
        return SizeInPixels(binding.root.width, binding.root.height)
    }

    override fun onCreate() {
        Timber.d("[onCreate]")
        super.onCreate()
        if (!QuizSharedUiInfo.isOngoing) {
            quizStartWindowModel.changeQuizState(QuizState.QUIZZING)
        }
        quizStartWindowModel.setStudentQuizzingList()
    }

    override fun onViewCreated() {
        super.onViewCreated()
        unclosedMissionUiManager.notifyMissionOngoingIfNeeded(MissionType.QUIZ)
        quizCommonWindowModel.addOpenedQuizWindowTag(tag)
        initUI()
        initOnClick()
        initCollection()
        setDisClosedErrorHandler()
        setUpdateStudentsPointErrorHandler()
        // Ongoing feature
        changeUiStatusForOngoing()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initUI() {
        if (!QuizSharedUiInfo.isOngoing) {
            setResultTitle(0, 0)
        }
        initScreenshotImage()
        initStudentQuizzingRecyclerView()
        initTrueFalseAnswerButtonGroup()
        initStudentQuizAnswerResultRecyclerView()
        binding.widgetRandomDraw.setTag(tag)
        binding.widgetRandomDraw.setListener(this)
    }

    private fun changeUiStatusForOngoing() {
        if (QuizSharedUiInfo.isOngoing) {
            Timber.d("[changeUiStatusForOngoing] isOngoing: true")
            // UI 階段會從 QUIZZING -> DISCLOSE_ANSWER -> QUIZ_RESULTS
            // 視窗一打開會是 QUIZZING 階段，所以無需處理
            // DISCLOSE_ANSWER 階段會呼叫 changeUiToDiscloseAnswerStage()
            // QUIZ_RESULTS 階段也會先走到 DISCLOSE_ANSWER 階段, 然後再由
            // initCollection() 中的 changeUiToQuizResultStage() 變更狀態
            when (quizStartWindowModel.quizzingUiState.value.quizState) {
                QuizState.DISCLOSE_ANSWER,
                QuizState.QUIZ_RESULTS-> {
                    changeUiToDiscloseAnswerStage()
                }
                else -> {}
            }
        }
    }

    private fun setResultTitle(answerCount: Int, attendanceCount: Int) {
        val resultTitle = context.getString(R.string.quiz_info_results)
        binding.tvResultTitle.text = String.format(resultTitle, answerCount.toString(), attendanceCount.toString())
    }

    private fun initScreenshotImage() {
        binding.apply {
            csScreenshotImage.setImage(quizCommonWindowModel.getScreenImageUri())
            csScreenshotImage.setCircleProgressbarVisibility(false)
            csScreenshotImage.setMaskVisibility(false)
        }
    }

    private fun initStudentQuizzingRecyclerView() {
        studentQuizzingListAdapter = QuizAnsweringAdapter {
            // show/hide student answer
            quizStartWindowModel.updateStudentQuizAnsweringVisibility(it)
        }

        binding.rvStudentQuizzingList.apply {
            layoutManager = GridLayoutManager(context, 5)
            addItemDecoration(StudentAnswerResultItemDecoration(5, 12f.dpToPx().toInt(), 8f.dpToPx().toInt()))
            adapter = studentQuizzingListAdapter
        }
    }

    private fun initStudentQuizAnswerResultRecyclerView() {
        studentQuizAnswerResultAdapter = QuizAnswerResultAdapter()

        binding.rvStudentQuizResultsList.apply {
            layoutManager = GridLayoutManager(context, 5)
            addItemDecoration(StudentAnswerResultItemDecoration(5, 12f.dpToPx().toInt(), 8f.dpToPx().toInt()))
            adapter = studentQuizAnswerResultAdapter
        }
    }

    private fun initTrueFalseAnswerButtonGroup() {
        val onChangeDiscloseAnswerButtonListener = object : OnChangeDiscloseAnswerButtonListener {
            override fun enableDiscloseAnswerButton() {
                binding.buttonDiscloseAnswer.setEnable()
            }
        }
        binding.csTrueFalseAnswerButtonGroup.setOnChangeDiscloseAnswerButtonListener(onChangeDiscloseAnswerButtonListener)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initOnClick() {
        binding.apply {
            viewNetworkDisconnect.bindCloseAction(ivClose)
            WindowControlButtonsUiHelper.setup(
                ivClose = ivClose,
                ivMinimizeWindow = ivMinimizeWindow,
                ivToolbarBringToFront = ivToolbarBringToFront,
                windowTag = tag,
                isMvbBound = quizCommonWindowModel.isMyViewBoardBound(),
                csWindowManager = csWindowManager,
                coroutineScope = coroutineScope,
                onCloseClick = {
                    if (quizStartWindowModel.quizzingUiState.value.quizState == QuizState.QUIZ_RESULTS) {
                        // QuizResult stage, CLOSE quiz
                        closeQuiz()
                    } else {
                        // From Quizzing to DiscloseAnswer stage, show dialog to CANCEL quiz
                        showDialog(context.getString(R.string.common_close_quiz), context.getString(R.string.dialog_message_close_quiz),
                            cancelListener = {
                                quizCancelJob?.cancel()
                                dialogWindow?.dismiss()
                            },
                            closeListener = {
                                dialogWindow?.startPositiveButtonLoading()
                                cancelQuiz()
                            }
                        )
                    }
                },
                onAfterMinimize = { unclosedMissionUiManager.notifyMissionMinimizedIfNeeded(MissionType.QUIZ) }
            )

            buttonEndQuiz.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    endQuiz()
                }
            })

            buttonDiscloseAnswer.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    buttonDiscloseAnswer.setLoading()
                    sendDiscloseAnswer()
                }
            })

            ivEye.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            Timber.d("ACTION_DOWN - $eyeStatus")
                            if (eyeStatus == EyeState.CLOSED) {
                                ivEye.setImageResource(R.drawable.ic_eye_closed_pressed)
                            }
                            if (eyeStatus == EyeState.OPENED) {
                                ivEye.setImageResource(R.drawable.ic_eye_opened_pressed)
                            }
                        }
                        MotionEvent.ACTION_UP -> {
                            Timber.d("ACTION_UP - $eyeStatus")
                            if (eyeStatus == EyeState.CLOSED) {
                                ivEye.setImageResource(R.drawable.ic_eye_opened)
                                setStudentQuizResultsListVisibility(true)
                                setTrueFalseQuizResultsOverviewVisibility(false)
                            }
                            if (eyeStatus == EyeState.OPENED) {
                                ivEye.setImageResource(R.drawable.ic_eye_closed)
                                setStudentQuizResultsListVisibility(false)
                                setTrueFalseQuizResultsOverviewVisibility(true)
                            }

                            eyeStatus = if (eyeStatus == EyeState.CLOSED)  {
                                EyeState.OPENED
                            } else {
                                EyeState.CLOSED
                            }
                        }
                    }
                    return true
                }
            })

            csAnswerOptionInfoBarchartView.setEventListener(object : AnswerOptionEventListener {
                override fun addPoint() {
                    quizStartWindowModel.updateStudentsPoint(points = 1, isManually = true)
                }
                override fun clickOptionItem(position: Int) {}
            })

            llRandomDraw.setOnClickListener {
                binding.widgetRandomDraw.visibility = View.VISIBLE
            }

            viewNetworkDisconnect.setOnClickListener {
                coroutineScope.launch(Dispatchers.Main) {
                    csWindowManager.bringWindowToTop(tag)
                    csWindowManager.bringWindowToTop(WindowTag.TOOLBAR)
                }
            }
        }
    }

    /**
     * 結束學生作答
     */
    private fun endQuiz() {
        binding.buttonEndQuiz.setLoading()
        coroutineScope.launch(Dispatchers.IO) {
            // 透過 API 更新 Quiz status to Finish
            if (quizStartWindowModel.updateQuizStatus(UpdateQuizStatusType.FINISH)) {
                // after API response
                withContext(Dispatchers.Main) {
                    changeUiToDiscloseAnswerStage()
                }
                quizStartWindowModel.changeQuizState(QuizState.DISCLOSE_ANSWER)
            } else {
                withContext(Dispatchers.Main) {
                    binding.buttonEndQuiz.setEnable()
                    showErrorToast(context.getString(R.string.quiz_error_msg_end_quiz))
                }
            }
        }
    }

    /**
     *  Quizzing & DiscloseAnswer 階段: closeStatus = Cancel
     */
    private fun cancelQuiz() {
        closeStatus = UpdateQuizStatusType.CANCEL
        quizCancelJob?.cancel()
        quizCancelJob = coroutineScope.launch(Dispatchers.IO) {
            if (quizStartWindowModel.updateQuizStatus(closeStatus)) {
                withContext(Dispatchers.Main) {
                    csWindowManager.removeWindow(tag)
                    unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
                    dialogWindow?.dismiss()
                }
            } else {
                withContext(Dispatchers.Main) {
                    showErrorToast(context.getString(R.string.quiz_error_msg_close_quiz))
                    dialogWindow?.setEnable(true)
                    dialogWindow?.dismiss()
                }
            }
        }
    }

    /**
     *  QuizResults 階段: closeStatus = Close
     */
    private fun closeQuiz() {
        closeStatus = UpdateQuizStatusType.CLOSE
        quizCloseJob?.cancel()
        quizCloseJob = coroutineScope.launch(Dispatchers.Main) {
            binding.apply {
                // show window mask and lottie animation
                windowCloseMask.visibility = View.VISIBLE
                csLoadingAnimation.visibility = View.VISIBLE
                csLoadingAnimation.playAnimation()
            }

            if (quizStartWindowModel.updateQuizStatus(closeStatus)) {
                withContext(Dispatchers.Main) {
                    csWindowManager.removeWindow(tag)
                    unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.apply {
                        windowCloseMask.visibility = View.GONE
                        csLoadingAnimation.visibility = View.GONE
                        csLoadingAnimation.cancelAnimation()
                    }
                    showErrorToast(context.getString(R.string.quiz_error_msg_close_quiz))
                }
            }
        }
    }

    private fun showDialog(title: String, message: String, cancelListener: (() -> Unit)? = null, closeListener: (() -> Unit)? = null) {
        dialogWindow =
            CSSystemDialogWindow.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(
                    context.getString(R.string.common_cancel),
                    context.getColor(R.color.cs_system_dialog_text_color)
                ) {
                    cancelListener?.invoke()
                    dialogWindow?.dismiss()
                }
                .setPositiveButton(
                    context.getString(R.string.common_close),
                    context.getColor(R.color.color_0A8CF0)
                ) {
                    closeListener?.invoke()
                }
                .build()
        dialogWindow?.show()
    }

    private fun showErrorToast(message: String, isSpannable: Boolean = false, boldText: String = "") {
        coroutineScope.launch(Dispatchers.Main) {
            if (isSpannable) {
                val spannableString = SpannableStringUtils.replaceStringFirstArgAsBoldStyle(message, boldText)
                binding.cstToast.setText(spannableString)
            } else {
                binding.cstToast.setText(message)
            }

            binding.cstToast.visibility = View.VISIBLE
            withContext(Dispatchers.IO) {
                delay(3000)
            }
            binding.cstToast.visibility = View.GONE
        }
    }

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.quizzingUiState.collect { uiState ->
                Timber.d("[initCollection]: collect studentTrueFalseQuizzingInfoList")
                Timber.d("[initCollection]: QuizState - ${uiState.quizState.name}")

                val studentQuizAnsweringInfoList = uiState.studentQuizzingInfoList.map {
                    var answeringState = AnsweringState.ABSENT
                    if (it.status == StudentInfo.Status.ACTIVE) {
                        answeringState = AnsweringState.NOT_ANSWER
                    }
                    if (it.answerDataList.isNotEmpty()) {
                        answeringState = AnsweringState.ANSWERED
                    }
                    QuizAnsweringInfo.fromStudentTrueFalseQuizzingInfo(it, answeringState, it.answerDataList)
                }

                withContext(Dispatchers.Main) {
                    // update studentQuizAnsweringInfo data of recyclerView
                    studentQuizzingListAdapter.submitList(studentQuizAnsweringInfoList)
                    // update answer/attendance count
                    setResultTitle(uiState.answerCount, uiState.attendanceCount)
                }

                if (uiState.quizState == QuizState.QUIZ_RESULTS) {
                    val correctOptionId = uiState.discloseAnswerData[0].optionId
                    val studentAnswerResultInfoList = quizStartWindowModel.getTrueFalseResultInfos()
                    val (trueNumber, falseNumber) = quizStartWindowModel.getTrueFalseNumberPair()
                    val noAnswerNumber = quizStartWindowModel.getNoAnswerNumber()
                    withContext(Dispatchers.Main) {
                        studentQuizAnswerResultAdapter.submitList(studentAnswerResultInfoList)
                        changeUiToQuizResultStage(trueNumber, falseNumber, noAnswerNumber, correctOptionId)
                    }
                }
            }
        }
        coroutineScope.launch(Dispatchers.IO) {
            quizCommonWindowModel.networkAvailabilityState.collect { hasNetwork ->
                withContext(Dispatchers.Main) {
                    binding.viewNetworkDisconnect.isVisible = !hasNetwork
                    if (!hasNetwork) {
                        dialogWindow?.dismiss()
                    }
                }
            }
        }
    }

    private fun setDisClosedErrorHandler() {
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.discloseAnswerErrorFlow.collectLatest { message ->
                withContext(Dispatchers.Main) {
                    binding.buttonDiscloseAnswer.setEnable()
                    Timber.d("[discloseAnswerErrorFlow]: $message")
                    showErrorToast(message)
                }
            }
        }
    }

    private fun setUpdateStudentsPointErrorHandler() {
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.updateStudentsPointErrorFlow.collectLatest { message ->
                withContext(Dispatchers.Main) {
                    binding.buttonDiscloseAnswer.setEnable()
                    Timber.d("[updateStudentsPointErrorFlow]: $message")
                    showErrorToast(message, isSpannable = true, boldText = context.getString(R.string.quiz_all_correct_students))
                }
            }
        }
    }

    private fun sendDiscloseAnswer() {
        val correctOptionId = binding.csTrueFalseAnswerButtonGroup.getTrueFalseAnswer()
        quizStartWindowModel.discloseAnswer(listOf(correctOptionId))
    }

    private fun changeUiToDiscloseAnswerStage() {
        binding.csStopwatch.stopStopwatch()
        binding.csStopwatch.visibility = View.GONE
        binding.buttonEndQuiz.setEnable()
        binding.buttonEndQuiz.visibility = View.GONE
        // show DiscloseAnswer button
        binding.buttonDiscloseAnswer.setDisable()
        binding.buttonDiscloseAnswer.visibility = View.VISIBLE
        binding.csTrueFalseAnswerButtonGroup.visibility = View.VISIBLE
    }

    private fun changeUiToQuizResultStage(trueNumber: Int, falseNumber: Int, noAnswerNumber: Int, correctOptionId: Int) {
        binding.apply {
            // hide UI components
            quizStartWindowModel.resetQuizStartTime()
            csTrueFalseAnswerButtonGroup.visibility = View.GONE
            buttonDiscloseAnswer.visibility = View.GONE
            rvStudentQuizzingList.visibility = View.GONE
            csStopwatch.visibility = View.GONE

            // set data for csTrueFalseQuizResultsOverview components
            if (correctOptionId == TRUE_OPTION_INDEX) {
                csTrueFalseQuizResultsOverview.setCorrectAnswer(TRUE_OPTION_INDEX)
                csTrueFalseQuizResultsOverview.setCorrectAnswerCount(trueNumber)
                csTrueFalseQuizResultsOverview.setIncorrectAnswerCount(falseNumber)
                csTrueFalseQuizResultsOverview.setNoAnswerCount(noAnswerNumber)
            }
            if (correctOptionId == FALSE_OPTION_INDEX) {
                csTrueFalseQuizResultsOverview.setCorrectAnswer(FALSE_OPTION_INDEX)
                csTrueFalseQuizResultsOverview.setCorrectAnswerCount(falseNumber)
                csTrueFalseQuizResultsOverview.setIncorrectAnswerCount(trueNumber)
                csTrueFalseQuizResultsOverview.setNoAnswerCount(noAnswerNumber)
            }

            // set data for csAnswerOptionInfoBarchartView components
            csAnswerOptionInfoBarchartView.setScoreInfos(trueFalseWindowModel.generateAnswerOptionInfoList(trueNumber, falseNumber, noAnswerNumber, correctOptionId))

            // show UI components
            csAnswerOptionInfoBarchartView.visibility = View.VISIBLE
            quizStartWindowModel.updateStudentsPoint(points = 1)
            eyeStatus = EyeState.CLOSED
            ivEye.setImageResource(R.drawable.ic_eye_closed)
            ivEye.visibility = View.VISIBLE
            llRandomDraw.visibility = View.VISIBLE
            setTrueFalseQuizResultsOverviewVisibility(true)
        }
    }

    private fun setStudentQuizResultsListVisibility(isShow: Boolean) {
        binding.apply {
            rvStudentQuizResultsList.visibility = if (isShow) View.VISIBLE else View.GONE
        }
    }

    private fun setTrueFalseQuizResultsOverviewVisibility(isShow: Boolean) {
        binding.apply {
            csTrueFalseQuizResultsOverview.visibility = if (isShow) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroy() {
        Timber.d("[onDestroy]")
        super.onDestroy()
        quizCommonWindowModel.removeOpenedQuizWindowTag(tag)
        quizStartWindowModel.onCleared()
        coroutineScope.cancel()
    }

    override fun onClickClose() {
        binding.widgetRandomDraw.visibility = View.GONE
    }
}