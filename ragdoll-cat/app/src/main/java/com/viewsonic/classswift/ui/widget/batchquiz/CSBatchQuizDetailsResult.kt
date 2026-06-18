package com.viewsonic.classswift.ui.widget.batchquiz

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.BatchQuizSummaryInfo
import com.viewsonic.classswift.databinding.WidgetBatchQuizDetailResultBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.quiz.AnswerOptionEventListener
import com.viewsonic.classswift.ui.widget.quiz.RandomDrawWidget
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerType
import com.viewsonic.classswift.ui.widgetmodel.batchquiz.CSBatchQuizDetailsResultWidgetModel
import com.viewsonic.classswift.ui.window.adapter.QuizAnswerResultAdapter
import com.viewsonic.classswift.ui.window.decoration.StudentAnswerResultItemDecoration
import com.viewsonic.classswift.ui.windowmodel.quiz.TrueFalseWindowModel.Companion.FALSE_OPTION_INDEX
import com.viewsonic.classswift.ui.windowmodel.quiz.TrueFalseWindowModel.Companion.TRUE_OPTION_INDEX
import com.viewsonic.classswift.ui.windowmodel.quiz.enums.EyeState
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.isLatexContent
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import kotlin.getValue

class CSBatchQuizDetailsResult @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), RandomDrawWidget.RandomDrawViewListener {
    private var binding: WidgetBatchQuizDetailResultBinding = WidgetBatchQuizDetailResultBinding.inflate(LayoutInflater.from(context), this)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val widgetModel: CSBatchQuizDetailsResultWidgetModel by inject(CSBatchQuizDetailsResultWidgetModel::class.java)
    private var collectDataJob: Job? = null
    private var collectUiEventJob: Job? = null
    private lateinit var studentQuizAnswerResultAdapter: QuizAnswerResultAdapter
    private var eyeStatus: EyeState = EyeState.CLOSED
    var onWidgetClose: (() -> Unit)? = null

    init {
        binding.root.setBackgroundResource(R.drawable.bg_cs_window)
        binding.cswAnswerStateBarchartView.setIncreaseTitle(R.string.quiz_status_answered)
        binding.cswAnswerStateBarchartView.setAnswerTitle(R.string.quiz_title_answer)
        initOnClickEvent()
        initRandomDrawWidget()
        initStudentQuizAnswerResultRecyclerView()
    }

    fun setSummaryInfo(info: BatchQuizSummaryInfo) {
        showLoadingAnimation()
        initCollection()
        widgetModel.getQuizDetailInfo(info)
    }

    private fun initCollection() {
        collectDataJob?.cancel()
        collectUiEventJob?.cancel()
        collectUiEventJob = coroutineScope.launch(Dispatchers.Main) {
            //todo error handling ui
            widgetModel.updateUiEventFlow.collect { uiEvent ->
                binding.apply {
                    when (uiEvent) {
                        CSBatchQuizDetailsResultWidgetModel.BatchQuizDetailResultUiEvent.GetDetailResultError -> {
                            cancelLoadingAnimation()
                        }
                        CSBatchQuizDetailsResultWidgetModel.BatchQuizDetailResultUiEvent.AddPointFailed -> {
                            cstToast.setText(context.getString(R.string.student_list_error_msg_failed_to_add_point))
                            cstToast.show()
                        }
                    }
                }
            }
        }

        collectDataJob = coroutineScope.launch(Dispatchers.Main) {
            widgetModel.detailInfoFlow.collect { detailInfo ->
                detailInfo?.let {
                    binding.apply {
                        tvResultTitle.text = String.format(
                            context.getString(R.string.quiz_info_results), "${detailInfo.correctStudentCount}", "${detailInfo.submittedStudentCount}"
                        )
                        cswAnswerStateBarchartView.setScoreInfos(
                            detailInfo.getAnswerOptionInfoList()
                        )
                        studentQuizAnswerResultAdapter.submitList(detailInfo.studentAnswerResultList)
                        if (detailInfo.quizContent.isLatexContent()) {
                            cswKatexView.isVisible = true
                            tvQuizContent.isVisible = false
                            cswKatexView.setText(detailInfo.quizContent)
                        } else {
                            cswKatexView.isVisible = false
                            tvQuizContent.isVisible = true
                            tvQuizContent.text = detailInfo.quizContent
                        }
                        if (detailInfo.answerType == AnswerType.TRUE_FALSE) {
                            // set data for csTrueFalseQuizResultsOverview components
                            cswTrueFalseQuizResultsOverview.isVisible = true
                            cswMultipleChoiceQuizResultsOverview.isVisible = false
                            cswTrueFalseQuizResultsOverview.apply {
                                if (detailInfo.correctAnswerList[0] == TRUE_OPTION_INDEX) {
                                    setCorrectAnswer(TRUE_OPTION_INDEX)
                                } else {
                                    setCorrectAnswer(FALSE_OPTION_INDEX)
                                }
                                setCorrectAnswerCount(detailInfo.correctStudentCount)
                                setIncorrectAnswerCount(detailInfo.inCorrectStudentCount)
                                setNoAnswerCount(detailInfo.noAnswerStudentCount)
                            }
                            tvTitle.text = context.getString(R.string.quiz_types_true_false)


                        } else {
                            cswTrueFalseQuizResultsOverview.isVisible = false
                            cswMultipleChoiceQuizResultsOverview.isVisible = true
                            tvTitle.text = context.getString(R.string.quiz_types_multiple_selection)
                            cswMultipleChoiceQuizResultsOverview.apply {
                                setAllParticipants(
                                    correctCount = detailInfo.correctStudentCount,
                                    incorrectCount = detailInfo.inCorrectStudentCount,
                                    noAnswerCount = detailInfo.noAnswerStudentCount
                                )
                                setCorrectAnswerImages(
                                    correctAnswers = detailInfo.correctAnswerList, isNumberOrAlphabet = detailInfo.optionType
                                )
                            }
                        }
                        cancelLoadingAnimation()
                        clSummaryInfoArea.isVisible = true
                    }
                } ?: return@collect
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initOnClickEvent() {
        binding.apply {
            ivClose.setOnClickListener {
                onWidgetClose?.invoke()
                collectDataJob?.cancel()
                collectUiEventJob?.cancel()
                widgetModel.onCleared()
                initState()
            }
            llRandomDraw.setOnClickListener {
                widgetRandomDraw.isVisible = true
            }
            cswAnswerStateBarchartView.apply {
                setEventListener(object : AnswerOptionEventListener {
                    override fun addPoint() {
                        coroutineScope.launch(Dispatchers.IO) {
                            widgetModel.addCorrectStudentsPoints()
                        }
                    }
                    override fun clickOptionItem(position: Int) {}
                })
            }

            ivEye.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (eyeStatus == EyeState.CLOSED) {
                            ivEye.setImageResource(R.drawable.ic_eye_closed_pressed)
                        }
                        if (eyeStatus == EyeState.OPENED) {
                            ivEye.setImageResource(R.drawable.ic_eye_opened_pressed)
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        if (eyeStatus == EyeState.CLOSED) {
                            ivEye.setImageResource(R.drawable.ic_eye_opened)
                            rvStudentQuizResultsList.isVisible = true
                            setQuizResultsOverviewVisibility(false)
                        }
                        if (eyeStatus == EyeState.OPENED) {
                            ivEye.setImageResource(R.drawable.ic_eye_closed)
                            rvStudentQuizResultsList.isVisible = false
                            setQuizResultsOverviewVisibility(true)
                        }

                        eyeStatus = if (eyeStatus == EyeState.CLOSED) {
                            EyeState.OPENED
                        } else {
                            EyeState.CLOSED
                        }
                    }
                }
                true
            }
        }
    }

    private fun initRandomDrawWidget() {
        binding.widgetRandomDraw.apply {
            setListener(this@CSBatchQuizDetailsResult)
            setTag(WindowTag.BATCH_QUIZ_RESULT)
        }
    }
    
    private fun initStudentQuizAnswerResultRecyclerView() {
        studentQuizAnswerResultAdapter = QuizAnswerResultAdapter()

        binding.rvStudentQuizResultsList.apply {
            layoutManager = GridLayoutManager(context, 5)
            addItemDecoration(StudentAnswerResultItemDecoration(5, 12f.dpToPx().toInt(), 8f.dpToPx().toInt()))
            adapter = studentQuizAnswerResultAdapter
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
    }

    private fun setQuizResultsOverviewVisibility(isVisible: Boolean) {
        binding.apply {
            if (widgetModel.detailInfoFlow.value?.answerType == AnswerType.TRUE_FALSE) {
                cswTrueFalseQuizResultsOverview.isVisible = isVisible
            }
            if (widgetModel.detailInfoFlow.value?.answerType == AnswerType.MULTIPLE_CHOICE) {
                cswMultipleChoiceQuizResultsOverview.isVisible = isVisible
            }
        }
    }

    private fun showLoadingAnimation() {
        binding.apply {
            windowCloseMask.visibility = VISIBLE
            csLoadingAnimation.visibility = VISIBLE
            csLoadingAnimation.playAnimation()
        }
    }

    private fun cancelLoadingAnimation() {
        binding.apply {
            windowCloseMask.visibility = GONE
            csLoadingAnimation.visibility = GONE
            csLoadingAnimation.cancelAnimation()
        }
    }

    private fun initState() {
        binding.apply {
            cswKatexView.release()
            // todo: this is workaround to solve chart bar view different data display issue
            cswAnswerStateBarchartView.resetAdapter()
            clSummaryInfoArea.isVisible = false
            ivEye.setImageResource(R.drawable.ic_eye_closed)
            eyeStatus = EyeState.CLOSED
            rvStudentQuizResultsList.isVisible = false
            setQuizResultsOverviewVisibility(true)
        }
    }

    override fun onClickClose() {
        binding.widgetRandomDraw.isVisible = false
    }
}