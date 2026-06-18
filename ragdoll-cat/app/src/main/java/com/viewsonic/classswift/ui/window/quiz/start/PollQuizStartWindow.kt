package com.viewsonic.classswift.ui.window.quiz.start

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.api.body.UpdateQuizStatusType
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.info.AnswerOptionInfo
import com.viewsonic.classswift.data.info.QuizAnswerResultInfo
import com.viewsonic.classswift.data.info.QuizAnsweringInfo
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.databinding.WindowPollStartQuizBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.widget.fastscroll.FastScrollerBuilder
import com.viewsonic.classswift.ui.widget.fastscroll.ScrollingViewOnApplyWindowInsetsListener
import com.viewsonic.classswift.ui.widget.quiz.AnswerOptionEventListener
import com.viewsonic.classswift.ui.widget.quiz.RandomDrawWidget
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerType
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState
import com.viewsonic.classswift.ui.window.CSSystemDialogWindow
import com.viewsonic.classswift.ui.window.adapter.QuizAnswerResultAdapter
import com.viewsonic.classswift.ui.window.adapter.QuizAnsweringAdapter
import com.viewsonic.classswift.ui.window.decoration.StudentAnswerResultItemDecoration
import com.viewsonic.classswift.ui.windowmodel.quiz.PollAnswerWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizCommonWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.QuizStartWindowModel
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.QuizState
import com.viewsonic.classswift.uimanager.UnclosedMissionUiManager
import com.viewsonic.classswift.utils.SpannableStringUtils
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.ui.helper.WindowControlButtonsUiHelper
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import kotlin.getValue

class PollQuizStartWindow(private val context: Context) : IWindow<WindowPollStartQuizBinding>, RandomDrawWidget.RandomDrawViewListener,
    QuizAnswerResultAdapter.OnItemInteractionListener {

    override var tag: WindowTag = WindowTag.POLL_START_QUIZ
    override var size: SizeInPixels = SizeInPixels(955f.dpToPx().toInt(), 515f.dpToPx().toInt())

    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    private val quizStartWindowModel: QuizStartWindowModel by inject(QuizStartWindowModel::class.java)
    private val pollAnswerWindowModel: PollAnswerWindowModel by inject(
        PollAnswerWindowModel::class.java
    )
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val coroutineScope = CoroutineManager.getScope(this)

    private lateinit var studentQuizzingListAdapter: QuizAnsweringAdapter
    private val studentQuizAnswerResultAdapter: QuizAnswerResultAdapter = QuizAnswerResultAdapter(this@PollQuizStartWindow)

    private var closeWindowStatus = UpdateQuizStatusType.CANCEL
    private var updateQuizStatusJob: Job? = null
    private var dialogWindow: CSSystemDialogWindow? = null

    override val binding: WindowPollStartQuizBinding =
        WindowPollStartQuizBinding.inflate(
            LayoutInflater.from(
                ContextThemeWrapper(
                    context,
                    com.google.android.material.R.style.Theme_MaterialComponents
                )
            )
        )

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
        setUpdateStudentsPointErrorHandler()
        // Ongoing feature
        changeUiStatusForOngoing()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initUI() {
        initTitleFlag()
        initScreenshotImage()
        initStudentQuizRecyclerView()

        // for fastScrollView
        binding.scrollView.setOnApplyWindowInsetsListener(ScrollingViewOnApplyWindowInsetsListener())
        val trackDrawable = ContextCompat.getDrawable(
            context,
            R.drawable.selector_quiz_scroll_track
        )

        val thumbDrawable = ContextCompat.getDrawable(
            context,
            R.drawable.selector_quiz_scroll_thumb
        )

        if (trackDrawable != null && thumbDrawable != null) {
            FastScrollerBuilder(
                context,
                binding.scrollView,
                trackDrawable,
                thumbDrawable
            ).build()
        }
        setAnsweringCountTitle(answerCount = 0, attendanceCount = 0)
        binding.widgetRandomDraw.setTag(tag)
        binding.widgetRandomDraw.setListener(this)
        binding.csAnswerOptionInfo.setIncreaseTitle(R.string.quiz_status_answered)
        binding.csAnswerOptionInfo.setAnswerTitle(R.string.quiz_title_answer)
    }

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
                    if (closeWindowStatus == UpdateQuizStatusType.CANCEL) {
                        cancelQuiz()
                    } else if (closeWindowStatus == UpdateQuizStatusType.CLOSE) {
                        closeQuiz()
                    }
                },
                onAfterMinimize = { unclosedMissionUiManager.notifyMissionMinimizedIfNeeded(MissionType.QUIZ) }
            )

            cslbEndQuiz.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    endQuiz()
                }
            })

            tbEye.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    rvAnswerResultList.visibility = View.VISIBLE
                    llVotedSummary.visibility = View.INVISIBLE
                } else {
                    rvAnswerResultList.visibility = View.INVISIBLE
                    llVotedSummary.visibility = View.VISIBLE
                }
            }

            llRandomDraw.setOnClickListener {
                widgetRandomDraw.visibility = View.VISIBLE
            }

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
            quizStartWindowModel.quizzingUiState.collect { uiState ->
                Timber.d("[initCollect] : collect studentChoiceQuizzingInfoList")
                Timber.d("[initCollection]: QuizState - ${uiState.quizState.name}")
                val studentQuizAnsweringInfoList = uiState.studentQuizzingInfoList.map {
                    var answeringState = AnsweringState.ABSENT
                    if (it.status == StudentInfo.Status.ACTIVE) {
                        answeringState = AnsweringState.NOT_ANSWER
                    }
                    if (it.answerDataList.isNotEmpty()) {
                        answeringState = AnsweringState.ANSWERED
                    }
                    QuizAnsweringInfo.fromStudentPollQuizzingInfo(
                        it,
                        QuizSharedUiInfo.quizOptionType,
                        QuizSharedUiInfo.quizType,
                        answeringState,
                        it.answerDataList
                    )
                }
                withContext(Dispatchers.Main) {
                    studentQuizzingListAdapter.submitList(studentQuizAnsweringInfoList)
                    setAnsweringCountTitle(uiState.answerCount, uiState.attendanceCount)
                }

                if (uiState.quizState == QuizState.QUIZ_RESULTS) {
                    withContext(Dispatchers.Main) {
                        closeWindowStatus = UpdateQuizStatusType.CLOSE
                        binding.rvAnswerResultList.visibility = View.VISIBLE
                        changeUiToQuizResultStage()
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

    private fun initTitleFlag() {
        binding.apply {
            if (QuizSharedUiInfo.quizType == QuizType.SINGLE_POLL) {
                val singleAnswer = ContextCompat.getString(context, R.string.quiz_types_poll_single_vote)
                tvPollTypeQuizzing.text = singleAnswer
                tvPollTypeDiscloseAnswer.text = singleAnswer
            }
            if (QuizSharedUiInfo.quizType == QuizType.MULTIPLE_POLL) {
                val multiAnswer = ContextCompat.getString(context, R.string.quiz_types_poll_multiple_votes)
                tvPollTypeQuizzing.text = multiAnswer
                tvPollTypeDiscloseAnswer.text = multiAnswer
            }
            tvPollTypeQuizzing.visibility = View.VISIBLE
            tvPollTypeDiscloseAnswer.visibility = View.GONE
        }
    }

    private fun changeUiStatusForOngoing() {
        if (QuizSharedUiInfo.isOngoing) {
            Timber.d("[changeUiStatusForOngoing] isOngoing: true")
            when (quizStartWindowModel.quizzingUiState.value.quizState) {
                QuizState.DISCLOSE_ANSWER,
                QuizState.QUIZ_RESULTS -> changeUiToDiscloseAnswerStage()
                else -> Unit
            }
        }
    }

    private fun initScreenshotImage() {
        binding.csScreenshotImage.apply {
            setImage(uri = quizCommonWindowModel.getScreenImageUri())
            setCircleProgressbarVisibility(isShown = false)
            setMaskVisibility(isShown = false)
        }
    }

    private fun initStudentQuizRecyclerView() {
        studentQuizzingListAdapter = QuizAnsweringAdapter { info ->
            // show/hide student answer
            quizStartWindowModel.updateStudentQuizAnsweringVisibility(info)
        }

        binding.rvAnswerResultList.apply {
            layoutManager = GridLayoutManager(context, 5)
            addItemDecoration(
                StudentAnswerResultItemDecoration(
                    5,
                    12f.dpToPx().toInt(),
                    8f.dpToPx().toInt()
                )
            )
            adapter = studentQuizzingListAdapter
        }
    }

    private fun setAnsweringCountTitle(answerCount: Int, attendanceCount: Int) {
        binding.tvResultTitle.text =
            String.format(
                context.getString(R.string.quiz_info_results),
                "$answerCount",
                "$attendanceCount"
            )
    }

    private fun setUpdateStudentsPointErrorHandler() {
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.updateStudentsPointErrorFlow.collectLatest { message ->
                withContext(Dispatchers.Main) {
                    Timber.d("[updateStudentsPointErrorFlow]: $message")
                    showErrorToast(
                        message = message,
                        isSpannable = true,
                        boldText = context.getString(R.string.quiz_all_correct_students)
                    )
                }
            }
        }
    }

    /**
     * Ends the current quiz session.
     */
    private fun endQuiz() {
        binding.cslbEndQuiz.setLoading()
        coroutineScope.launch(Dispatchers.IO) {
            if (quizStartWindowModel.updateQuizStatus(UpdateQuizStatusType.FINISH)) {
                withContext(Dispatchers.Main) {
                    changeUiToDiscloseAnswerStage()
                }
                quizStartWindowModel.changeQuizState(QuizState.QUIZ_RESULTS)
            } else {
                withContext(Dispatchers.Main) {
                    binding.cslbEndQuiz.setEnable()
                    showErrorToast(context.getString(R.string.quiz_error_msg_end_quiz))
                }
            }
        }
    }

    /**
     *  In the Quizzing and DiscloseAnswer phases, closeStatus is set to Cancel
     */
    private fun cancelQuiz() {
        showDialog(
            context.getString(R.string.common_close_quiz),
            context.getString(R.string.dialog_message_close_quiz),
            cancelListener = {
                updateQuizStatusJob?.cancel()
                dialogWindow?.dismiss()
            },
            closeListener = {
                dialogWindow?.startPositiveButtonLoading()
                updateQuizStatusJob?.cancel()
                updateQuizStatusJob = coroutineScope.launch(Dispatchers.IO) {
                    if (quizStartWindowModel.updateQuizStatus(closeWindowStatus)) {
                        withContext(Dispatchers.Main) {
                            csWindowManager.removeWindow(tag)
                            unclosedMissionUiManager.notifyMissionClosedIfNeeded(MissionType.QUIZ)
                            dialogWindow?.dismiss()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showErrorToast(context.getString(R.string.quiz_error_msg_close_quiz))
                            dialogWindow?.let {
                                it.setEnable(true)
                                it.dismiss()
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     *  In the QuizResults phase, closeStatus is set to Close
     */
    private fun closeQuiz() {
        updateQuizStatusJob?.cancel()
        updateQuizStatusJob = coroutineScope.launch(Dispatchers.Main) {
            binding.apply {
                // show window mask and lottie animation
                windowCloseMask.visibility = View.VISIBLE
                csLoadingAnimation.visibility = View.VISIBLE
                csLoadingAnimation.playAnimation()
            }
            withContext(Dispatchers.IO) {
                if (quizStartWindowModel.updateQuizStatus(closeWindowStatus)) {
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
    }

    private fun showDialog(
        title: String,
        message: String,
        cancelListener: (() -> Unit)? = null,
        closeListener: (() -> Unit)? = null
    ) {
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

    private fun showErrorToast(
        message: String,
        isSpannable: Boolean = false,
        boldText: String = ""
    ) {
        with(binding.cstToast) {
            if (isSpannable) {
                val spannableString =
                    SpannableStringUtils.replaceStringFirstArgAsBoldStyle(message, boldText)
                setText(spannableString)
            } else {
                setText(message)
            }
            show(coroutineScope)
        }
    }

    private fun changeUiToDiscloseAnswerStage() {
        with(binding) {
            csStopwatch.stopStopwatch()
            csStopwatch.visibility = View.INVISIBLE
            quizStartWindowModel.resetQuizStartTime()
            cslbEndQuiz.setEnable()
            cslbEndQuiz.visibility = View.GONE
        }
    }

    private fun changeUiToQuizResultStage() {
        with(binding) {
            csStopwatch.visibility = View.INVISIBLE
            tbEye.visibility = View.VISIBLE
            llRandomDraw.visibility = View.VISIBLE
            tvPollTypeDiscloseAnswer.visibility = View.VISIBLE
            tvPollTypeQuizzing.visibility = View.GONE


            val studentAnswerResultInfoList = pollAnswerWindowModel.getPollAnswerResultInfos()

            studentQuizAnswerResultAdapter.submitList(studentAnswerResultInfoList)
            rvAnswerResultList.adapter = studentQuizAnswerResultAdapter

            csAnswerOptionInfo.visibility = View.VISIBLE

            val correctAnswerStudents = studentAnswerResultInfoList.filter {
                it.answerResultState == AnswerResultState.ANSWERED
            }.map {
                it.studentId
            }
            tvVotedCount.text = String.format(context.getString(R.string.quiz_info_participants_count), "${correctAnswerStudents.size}")
            val noAnswerParticipants = studentAnswerResultInfoList.count {
                it.answerResultState == AnswerResultState.NO_ANSWER
            }
            tvNoAnswerCount.text = String.format(context.getString(R.string.quiz_info_participants_count), "$noAnswerParticipants")
            llVotedSummary.visibility = View.VISIBLE
            rvAnswerResultList.visibility = View.GONE
            coroutineScope.launch(Dispatchers.IO) {
                quizStartWindowModel.updateStudentsPoint(correctAnswerStudents, 1)
            }
            setAnswerOptionInfoView(studentAnswerResultInfoList)
        }
    }

    private fun setAnswerOptionInfoView(answerResultInfoList: List<QuizAnswerResultInfo>) {
        val total = answerResultInfoList.count { it.answerResultState != AnswerResultState.ABSENT }
        val answerOptionInfoList = ArrayList<AnswerOptionInfo>()

        // For Option data
        for (option in 0..<QuizSharedUiInfo.quizOptionCount) {
            val answerResultState = AnswerResultState.CORRECT
            answerOptionInfoList.add(
                AnswerOptionInfo(
                    position = option,
                    answerType = AnswerType.MULTIPLE_CHOICE,
                    answerResultState = answerResultState,
                    multipleType = QuizSharedUiInfo.quizOptionType,
                    answerCount = answerResultInfoList.map { it.answerOption }
                        .count { it.contains(option + 1) },
                    totalStudents = total
                )
            )
        }
        // For Unsubmitted Answers
        answerOptionInfoList.add(
            AnswerOptionInfo(
                answerResultState = AnswerResultState.NO_ANSWER,
                answerCount = answerResultInfoList.count { it.answerResultState == AnswerResultState.NO_ANSWER },
                totalStudents = total
            )
        )

        binding.csAnswerOptionInfo.apply {
            setScoreInfos(answerOptionInfoList)
            setEventListener(object : AnswerOptionEventListener {
                override fun addPoint() {
                    val correctAnswerStudents =
                        answerResultInfoList.filter { it.answerResultState == AnswerResultState.ANSWERED }
                            .map { it.studentId }
                    coroutineScope.launch(Dispatchers.IO) {
                        quizStartWindowModel.updateStudentsPoint(correctAnswerStudents, 1, true)
                    }
                }

                override fun clickOptionItem(position: Int) {}
            })
        }
    }

    override fun onDestroy() {
        Timber.d("[onDestroy]")
        super.onDestroy()
        quizCommonWindowModel.onCleared()
        quizStartWindowModel.onCleared()
        quizCommonWindowModel.removeOpenedQuizWindowTag(tag)
        coroutineScope.cancel()
    }

    override fun onClickClose() {
        binding.widgetRandomDraw.visibility = View.GONE
    }
}