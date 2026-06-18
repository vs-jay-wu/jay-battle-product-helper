package com.viewsonic.classswift.ui.window.quiz.start

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Outline
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.body.UpdateQuizStatusType
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.info.AnswerOptionInfo
import com.viewsonic.classswift.data.info.AudioAnswerInfo
import com.viewsonic.classswift.data.state.QuizSharedUiInfo
import com.viewsonic.classswift.databinding.WindowAudioStartQuizBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.ui.widget.fastscroll.FastScrollerBuilder
import com.viewsonic.classswift.ui.widget.fastscroll.ScrollingViewOnApplyWindowInsetsListener
import com.viewsonic.classswift.ui.widget.quiz.AnswerOptionEventListener
import com.viewsonic.classswift.ui.widget.quiz.RandomDrawWidget
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.ui.window.CSSystemDialogWindow
import com.viewsonic.classswift.ui.window.adapter.AudioAnswerAdapter
import com.viewsonic.classswift.ui.window.adapter.AudioAnswerAdapter.AudioAnswerItemEventListener
import com.viewsonic.classswift.ui.window.decoration.StudentAnswerResultItemDecoration
import com.viewsonic.classswift.ui.windowmodel.quiz.AudioStartWindowModel
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

class AudioQuizStartWindow (private val applicationContext: Context) :
    IWindow<WindowAudioStartQuizBinding>,
    RandomDrawWidget.RandomDrawViewListener,
    AudioAnswerAdapter.OnAudioAnswerItemEventListener
{
    private val csWindowManager: CSWindowManager by inject(CSWindowManager::class.java)
    private val quizCommonWindowModel: QuizCommonWindowModel by inject(QuizCommonWindowModel::class.java)
    private val quizStartWindowModel: QuizStartWindowModel by inject(QuizStartWindowModel::class.java)
    private val audioStartWindowModel: AudioStartWindowModel by inject(AudioStartWindowModel::class.java)
    private val unclosedMissionUiManager: UnclosedMissionUiManager by inject(UnclosedMissionUiManager::class.java)
    private val coroutineScope = CoroutineManager.getScope(this)
    private var closeWindowStatus = UpdateQuizStatusType.CANCEL
    private var updateQuizStatusJob: Job? = null
    private var dialogWindow: CSSystemDialogWindow? = null
    private val viewOutlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, 10.66f.dpToPx())
        }
    }

    private lateinit var audioAnswerAdapter: AudioAnswerAdapter

    override var tag: WindowTag = WindowTag.AUDIO_START_QUIZ
    override var size: SizeInPixels = SizeInPixels(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT
    )

    override val binding: WindowAudioStartQuizBinding =
        WindowAudioStartQuizBinding.inflate(
            LayoutInflater.from(
                ContextThemeWrapper(
                    applicationContext,
                    com.google.android.material.R.style.Theme_MaterialComponents
                )
            )
        )

    override fun getCurrentSize(): SizeInPixels {
        with(binding.root) {
            if (width <= 0 || height <= 0) {
                measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                return SizeInPixels(measuredWidth, measuredHeight)
            }
            return SizeInPixels(width, height)
        }
    }

    override fun onCreate() {
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
        binding.flContainer.outlineProvider = viewOutlineProvider
        binding.flContainer.clipToOutline = true
        initScreenshotImage()
        initStudentQuizRecyclerView()

        // for fastScrollView
        binding.scrollView.setOnApplyWindowInsetsListener(ScrollingViewOnApplyWindowInsetsListener())
        val trackDrawable = ContextCompat.getDrawable(
            applicationContext,
            R.drawable.selector_quiz_scroll_track
        )

        val thumbDrawable = ContextCompat.getDrawable(
            applicationContext,
            R.drawable.selector_quiz_scroll_thumb
        )

        if (trackDrawable != null && thumbDrawable != null) {
            FastScrollerBuilder(
                applicationContext,
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

    private fun initStudentQuizRecyclerView() {
        audioAnswerAdapter = AudioAnswerAdapter(this)
        binding.rvAnswerResultList.apply {
            layoutManager = GridLayoutManager(context, 5)
            addItemDecoration(
                StudentAnswerResultItemDecoration(
                    5,
                    12f.dpToPx().toInt(),
                    8f.dpToPx().toInt()
                )
            )
            adapter = audioAnswerAdapter
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

            buttonEndQuiz.setOnCustomClickListener(object : OnLoadingButtonStateListener {
                override fun onEnableClicked() {
                    endQuiz()
                }
            })

            tbEye.setOnCheckedChangeListener { _, isChecked ->
                Timber.d("[diffCallback]: tbEye.setOnCheckedChangeListener: $isChecked")
                audioAnswerAdapter.submitList(audioStartWindowModel.setPartiallyVisibleState(!isChecked))
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

    /**
     * Ends the current quiz session.
     */
    private fun endQuiz() {
        binding.buttonEndQuiz.setLoading()
        coroutineScope.launch(Dispatchers.IO) {
            if (quizStartWindowModel.updateQuizStatus(UpdateQuizStatusType.FINISH)) {
                withContext(Dispatchers.Main) {
                    audioStartWindowModel.setStop()
                    changeUiToDiscloseAnswerStage()
                }
                quizStartWindowModel.changeQuizState(QuizState.QUIZ_RESULTS)
            } else {
                withContext(Dispatchers.Main) {
                    binding.buttonEndQuiz.setEnable()
                    showErrorToast(applicationContext.getString(R.string.quiz_error_msg_end_quiz))
                }
            }
        }
    }

    /**
     *  In the Quizzing and DiscloseAnswer phases, closeStatus is set to Cancel
     */
    private fun cancelQuiz() {
        showDialog(
            applicationContext.getString(R.string.common_close_quiz),
            applicationContext.getString(R.string.dialog_message_close_quiz),
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
                            showErrorToast(applicationContext.getString(R.string.quiz_error_msg_close_quiz))
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
                viewCloseMask.visibility = View.VISIBLE
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
                            viewCloseMask.visibility = View.GONE
                            csLoadingAnimation.visibility = View.GONE
                            csLoadingAnimation.cancelAnimation()
                        }
                        showErrorToast(applicationContext.getString(R.string.quiz_error_msg_close_quiz))
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
            CSSystemDialogWindow.Builder(applicationContext)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(
                    applicationContext.getString(R.string.common_cancel),
                    applicationContext.getColor(R.color.cs_system_dialog_text_color)
                ) {
                    cancelListener?.invoke()
                    dialogWindow?.dismiss()
                }
                .setPositiveButton(
                    applicationContext.getString(R.string.common_close),
                    applicationContext.getColor(R.color.color_0A8CF0)
                ) {
                    closeListener?.invoke()
                }
                .build()
        dialogWindow?.show()
    }

    private fun showErrorToast(message: String) {
        with(binding.cstToast) {
            setText(message)
            show(coroutineScope)
        }
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

    private fun initCollection() {
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.quizzingUiState.collect { uiState ->
                withContext(Dispatchers.Main) {
                    val studentQuizAnsweringInfoList = audioStartWindowModel.getCurrentStudentQuizAudioAnsweringInfoList()
                    audioAnswerAdapter.submitList(studentQuizAnsweringInfoList)
                    setAnsweringCountTitle(uiState.answerCount, uiState.attendanceCount)
                    if (uiState.quizState == QuizState.QUIZ_RESULTS) {
                        withContext(Dispatchers.Main) {
                            closeWindowStatus = UpdateQuizStatusType.CLOSE
                            binding.rvAnswerResultList.visibility = View.VISIBLE
                            changeUiToQuizResultStage(audioStartWindowModel.setQuizResult())
                        }
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
        coroutineScope.launch(Dispatchers.IO) {
            audioStartWindowModel.audioInfosSharedFlow.collect { infos ->
                withContext(Dispatchers.Main) {
                   audioAnswerAdapter.submitList(infos)
                }
            }
        }
    }

    private fun setUpdateStudentsPointErrorHandler() {
        coroutineScope.launch(Dispatchers.IO) {
            quizStartWindowModel.updateStudentsPointErrorFlow.collectLatest { message ->
                withContext(Dispatchers.Main) {
                    Timber.d("[updateStudentsPointErrorFlow]: $message")
                    showErrorToast(
                        message = message,
                        isSpannable = true,
                        boldText = applicationContext.getString(R.string.quiz_all_correct_students)
                    )
                }
            }
        }
    }

    private fun setAnsweringCountTitle(answerCount: Int, attendanceCount: Int) {
        binding.tvResultTitle.text =
            String.format(
                applicationContext.getString(R.string.quiz_info_results),
                "$answerCount",
                "$attendanceCount"
            )
    }

    private fun changeUiToDiscloseAnswerStage() {
        with(binding) {
            csStopwatch.stopStopwatch()
            csStopwatch.visibility = View.INVISIBLE
            quizStartWindowModel.resetQuizStartTime()
            buttonEndQuiz.setEnable()
            buttonEndQuiz.visibility = View.GONE
        }
    }

    private fun changeUiToQuizResultStage(answerList: List<AudioAnswerInfo>) {
        with(binding) {
            csStopwatch.visibility = View.INVISIBLE
            tbEye.visibility = View.VISIBLE
            llRandomDraw.visibility = View.VISIBLE
            audioAnswerAdapter.submitList(answerList)
            csAnswerOptionInfo.visibility = View.VISIBLE
            val correctAnswerStudents = answerList.filter {
                it.answerResultState == AnswerResultState.ANSWERED
            }.map {
                it.studentId
            }
            coroutineScope.launch(Dispatchers.IO) {
                quizStartWindowModel.updateStudentsPoint(correctAnswerStudents, 1)
            }
            setAnswerOptionInfoView(answerList)
        }
    }

    private fun setAnswerOptionInfoView(
        answerResultInfoList: List<AudioAnswerInfo>
    ) {
        val total = answerResultInfoList.count { it.answerResultState != AnswerResultState.ABSENT }
        val answerOptionInfoList = ArrayList<AnswerOptionInfo>()

        answerOptionInfoList.add(
            AnswerOptionInfo(
                answerResultState = AnswerResultState.ANSWERED,
                answerCount = answerResultInfoList.count { it.answerResultState == AnswerResultState.ANSWERED },
                totalStudents = total
            )
        )

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
        super.onDestroy()
        quizCommonWindowModel.onCleared()
        quizStartWindowModel.onCleared()
        audioStartWindowModel.onCleared()
        quizCommonWindowModel.removeOpenedQuizWindowTag(tag)
        coroutineScope.cancel()
    }

    override fun onClickClose() {
        binding.widgetRandomDraw.visibility = View.GONE
    }

    override fun onAudioAnswerItemEvent(event: AudioAnswerItemEventListener) {
        when (event) {
            is AudioAnswerItemEventListener.ItemClick ->{
               audioStartWindowModel.setCanShowAnswer(event.info)
            }
            is AudioAnswerItemEventListener.PauseAudio -> {
                audioStartWindowModel.setPause()
            }
            is AudioAnswerItemEventListener.PlayAudio -> {
                audioStartWindowModel.setPlay(event.info)
            }
            is AudioAnswerItemEventListener.UpdateDuration -> {
                audioStartWindowModel.setDuration(event.info)
            }
        }
    }
}