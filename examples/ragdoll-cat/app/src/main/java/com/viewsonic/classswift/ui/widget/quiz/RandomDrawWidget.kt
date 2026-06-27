package com.viewsonic.classswift.ui.widget.quiz

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.viewsonic.classswift.R
import com.viewsonic.classswift.constant.AppConstants.ONE_SEC_DELAY
import com.viewsonic.classswift.data.info.QuizAnswerResultInfo
import com.viewsonic.classswift.databinding.ViewRandomDrawBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widgetmodel.RandomDrawWidgetModel
import com.viewsonic.classswift.utils.extension.show
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject

@SuppressLint("ClickableViewAccessibility")
class RandomDrawWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewRandomDrawBinding =
        ViewRandomDrawBinding.inflate(LayoutInflater.from(context), this, true)

    private val widgetModel: RandomDrawWidgetModel by inject(RandomDrawWidgetModel::class.java)
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var collectHasAttendedJob: Job? = null
    private var collectPickupStudentJob: Job? = null
    private var pickStudentJob: Job? = null

    private val conditionButtons = ArrayList<MaterialButton>()
    private var listener: RandomDrawViewListener? = null

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.RandomBuzzerView,
            0,
            0
        ).let {
            widgetModel.isCorrectType = it.getBoolean(R.styleable.RandomBuzzerView_hasCorrectAnswer, true)
            it.recycle()
        }
        setConditionsButtonUI()
        //to avoid quiz window click behavior
        binding.root.setOnTouchListener { _, _ ->
            widgetModel.bringQuizWindowToFront()
            true
        }
        initClickAction()
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        initCollect()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        widgetModel.onCleared()
        collectPickupStudentJob?.cancel()
        collectHasAttendedJob?.cancel()
        pickStudentJob?.cancel()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            widgetModel.syncStudentAnsweredInfos()
        } else {
            pickStudentJob?.cancel()
            binding.lottieDice.cancelAnimation()
        }
    }

    private fun initClickAction() {
        binding.apply {
            ivClose.setOnClickListener {
                listener?.onClickClose()
                resetStatus()
            }
            clDice.setOnClickListener {
                if (!widgetModel.isSocketConnected()) {
                    binding.cstErrorToast.show(coroutineScope)
                    return@setOnClickListener
                }
                pickStudentJob?.cancel()
                pickStudentJob = coroutineScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.Main) {
                        tvConditions.visibility = View.INVISIBLE
                        llConditions.visibility = View.INVISIBLE
                        lottieDice.visibility = View.VISIBLE
                        lottieDice.playAnimation()
                    }
                    delay(ONE_SEC_DELAY)
                    widgetModel.pickStudent()
                }
            }
            btnCorrect.setOnClickListener {
                setButtonSelectedStatus(btnCorrect)
                widgetModel.setPickAnswerType(RandomDrawWidgetModel.PickAnswerType.CORRECT)
            }
            btnIncorrect.setOnClickListener {
                setButtonSelectedStatus(btnIncorrect)
                widgetModel.setPickAnswerType(RandomDrawWidgetModel.PickAnswerType.INCORRECT)
            }
            btnAnswered.setOnClickListener {
                setButtonSelectedStatus(btnAnswered)
                widgetModel.setPickAnswerType(RandomDrawWidgetModel.PickAnswerType.ANSWERED)
            }
            btnNoAnswered.setOnClickListener {
                setButtonSelectedStatus(btnNoAnswered)
                widgetModel.setPickAnswerType(RandomDrawWidgetModel.PickAnswerType.NO_ANSWERED)
            }
            btnAll.setOnClickListener {
                setButtonSelectedStatus(btnAll)
                widgetModel.setPickAnswerType(RandomDrawWidgetModel.PickAnswerType.ALL)
            }
            llTryAgain.setOnClickListener {
                setReadyPickUpUi()
            }
        }
    }

    private fun initCollect() {
        collectHasAttendedJob = coroutineScope.launch(Dispatchers.IO) {
            widgetModel.hasAttendedUiState.collect { state ->
                when (state) {
                    is RandomDrawWidgetModel.RandomDrawUiState.HasAttendStudent -> {
                        if (!binding.lottieDice.isAnimating) {
                            withContext(Dispatchers.Main) {
                                if (state.value) {
                                    setHasAttendedStudentUi()
                                } else {
                                    setNoAttendedStudentUi()
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
        collectPickupStudentJob = coroutineScope.launch(Dispatchers.IO) {
            widgetModel.pickupStudentUiState.collect {
                pickStudentJob?.cancel()
                withContext(Dispatchers.Main) {
                    binding.lottieDice.cancelAnimation()
                    it.info?.let { info ->
                        setPickStudentInfoUi(info)
                    } ?: run {
                        setNoAttendedStudentUi()
                    }
                }

            }
        }
    }

    private fun setConditionsButtonUI() {
        if (widgetModel.isCorrectType) {
            binding.apply {
                btnCorrect.visibility = VISIBLE
                btnIncorrect.visibility = VISIBLE
                btnAnswered.visibility = GONE
                conditionButtons.add(binding.btnCorrect)
                conditionButtons.add(binding.btnIncorrect)
                conditionButtons.add(binding.btnNoAnswered)
                conditionButtons.add(binding.btnAll)
                setButtonSelectedStatus(btnCorrect)
                widgetModel.setPickAnswerType(RandomDrawWidgetModel.PickAnswerType.CORRECT)
            }
        } else {
            binding.apply {
                btnCorrect.visibility = GONE
                btnIncorrect.visibility = GONE
                btnAnswered.visibility = VISIBLE
                conditionButtons.add(binding.btnAnswered)
                conditionButtons.add(binding.btnNoAnswered)
                conditionButtons.add(binding.btnAll)
                setButtonSelectedStatus(btnAnswered)
                widgetModel.setPickAnswerType(RandomDrawWidgetModel.PickAnswerType.ANSWERED)
            }
        }
    }

    private fun setButtonSelectedStatus(button: MaterialButton) {
        conditionButtons.forEach { conditionButton ->
            conditionButton.isSelected = conditionButton == button
            conditionButton.icon = if (conditionButton == button) ContextCompat.getDrawable(context, R.drawable.ic_check_white) else null
        }
    }

    private fun setReadyPickUpUi() {
        binding.apply {
            clDice.visibility = VISIBLE
            lottieDice.visibility = GONE
            clParticipantInfo.visibility = GONE
            tvNoParticipants.visibility = GONE
            llTryAgain.visibility = GONE
            tvConditions.visibility = VISIBLE
            llConditions.visibility = VISIBLE
        }
    }

    private fun setNoAttendedStudentUi() {
        binding.apply {
            if (!lottieDice.isAnimating) {
                clDice.visibility = View.GONE
                lottieDice.visibility = View.GONE
                clParticipantInfo.visibility = View.GONE
                tvNoParticipants.visibility = View.VISIBLE
                llTryAgain.visibility = View.GONE
                tvConditions.visibility = View.VISIBLE
                llConditions.visibility = View.VISIBLE
            }
        }
    }

    private fun setHasAttendedStudentUi() {
        binding.apply {
            if (!lottieDice.isAnimating) {
                clDice.visibility = View.VISIBLE
                lottieDice.visibility = View.GONE
                clParticipantInfo.visibility = View.GONE
                tvNoParticipants.visibility = View.GONE
                llTryAgain.visibility = View.GONE
            }
        }
    }

    private fun setPickStudentInfoUi(info: QuizAnswerResultInfo) {
        binding.apply {
            if (!lottieDice.isAnimating) {
                clDice.visibility = GONE
                lottieDice.visibility = GONE
                tvNoParticipants.visibility = GONE
                llTryAgain.visibility = VISIBLE
                clParticipantInfo.visibility = VISIBLE
                tvName.text = info.displayName
                tvSeat.text = info.displaySeatNumber
            }
        }
        widgetModel.sendSocketEvent(info)
    }

    private fun resetStatus() {
        setReadyPickUpUi()
        if (widgetModel.isCorrectType) {
            setButtonSelectedStatus(binding.btnCorrect)
            widgetModel.setPickAnswerType(RandomDrawWidgetModel.PickAnswerType.CORRECT)
        } else {
            setButtonSelectedStatus(binding.btnAnswered)
            widgetModel.setPickAnswerType(RandomDrawWidgetModel.PickAnswerType.ANSWERED)
        }
    }

    fun setListener(listener: RandomDrawViewListener) {
        this.listener = listener
    }

    fun setTag(tag: WindowTag) {
        widgetModel.setTag(tag)
    }

    interface RandomDrawViewListener {
        fun onClickClose()
    }
}
