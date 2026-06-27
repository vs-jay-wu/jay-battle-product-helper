package com.viewsonic.classswift.ui.widget.quiz

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.constant.AppConstants
import com.viewsonic.classswift.constant.AppConstants.ONE_SEC_DELAY
import com.viewsonic.classswift.databinding.ViewOptionButtonBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionState
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionValueType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class OptionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val coroutineScope = CoroutineManager.getScope(this)
    private var onOptionButtonListener: OnOptionButtonListener? = null

    var optionState: OptionState = OptionState.OPTION
    private var optionValueType: OptionValueType = OptionValueType.NUMBER
    private var optionIndex: Int = 0

    private var binding: ViewOptionButtonBinding =
        ViewOptionButtonBinding.inflate(LayoutInflater.from(context), this)

    private var countingDownJob: Job? = null

    init {
        // set Attr variable
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MultiChoiceOptionButton,
            0,
            0
        ).let {
            val choiceValueIndex = it.getInt(R.styleable.MultiChoiceOptionButton_optionIndex, 0)
            Timber.d("choiceValueIndex: $choiceValueIndex")
            if (choiceValueIndex != 0) {
                optionIndex = choiceValueIndex
                updateOptionState(optionState)
            }
            it.recycle()
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateOptionState(state: OptionState) {
        optionState = state
        when (optionState) {
            OptionState.ADD -> {
                background = null
                foreground = null
                backgroundTintList = null
                foregroundTintList = null
                with(binding) {
                    ivAdd.isVisible = true
                    ivTrash.isVisible = false
                    tvOption.isVisible = false
                    ivAdd.setImageResource(R.drawable.ic_option_add)
                }
            }
            OptionState.ADD_PRESSED -> {
                background = null
                foreground = null
                backgroundTintList = null
                foregroundTintList = null
                with(binding) {
                    ivAdd.isVisible = true
                    ivTrash.isVisible = false
                    tvOption.isVisible = false
                    ivAdd.setImageResource(R.drawable.ic_option_add_pressed)
                }
            }
            OptionState.OPTION -> {
                background = ContextCompat.getDrawable(context, R.drawable.bg_white_radius400)
                foreground = ContextCompat.getDrawable(context, R.drawable.bg_transparent_radius400_line_white_border400)
                backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.neutral_0))
                foregroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.color_0A8CF0))
                with(binding) {
                    ivAdd.isVisible = false
                    ivTrash.isVisible = false
                    tvOption.isVisible = true
                    tvOption.setTextColor(ContextCompat.getColor(binding.root.context, R.color.color_0A8CF0))
                    updateOptionValueType(optionValueType)
                }
            }
            OptionState.DELETE -> {
                background = ContextCompat.getDrawable(context, R.drawable.bg_white_radius400)
                foreground = ContextCompat.getDrawable(context, R.drawable.bg_transparent_radius400_line_white_border400)
                backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.neutral_0))
                foregroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.red_500))
                with(binding) {
                    ivAdd.isVisible = false
                    ivTrash.isVisible = true
                    tvOption.isVisible = false
                }
            }
            OptionState.DELETE_PRESSED -> {
                background = ContextCompat.getDrawable(context, R.drawable.bg_white_radius400)
                foreground = ContextCompat.getDrawable(context, R.drawable.bg_transparent_radius400_line_white_border400)
                backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.red_50))
                foregroundTintList = ColorStateList.valueOf(ContextCompat.getColor(binding.root.context, R.color.red_500))
                with(binding) {
                    ivAdd.isVisible = false
                    ivTrash.isVisible = true
                    tvOption.isVisible = false
                }
            }
            OptionState.DELETED -> {
                background = null
                foreground = null
                backgroundTintList = null
                foregroundTintList = null
                with(binding) {
                    ivAdd.isVisible = false
                    ivTrash.isVisible = false
                    tvOption.isVisible = false
                }
            }
        }
    }

    fun setOnOptionButtonListener(onOptionButtonListener: OnOptionButtonListener) {
        this.onOptionButtonListener = onOptionButtonListener
    }

    private val touchEventListener = object : OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    countingDownJob?.cancel()
                    Timber.d("ACTION_DOWN - $optionState")
                    when (optionState) {
                        OptionState.ADD -> {
                            onOptionButtonListener?.resetDeleteStatus()
                            updateOptionState(OptionState.ADD_PRESSED)
                        }
                        OptionState.OPTION -> {
                            onOptionButtonListener?.resetDeleteStatus()
                        }
                        OptionState.DELETE -> {
                            updateOptionState(OptionState.DELETE_PRESSED)

                        }
                        else -> {}
                    }
                }

                MotionEvent.ACTION_UP -> {
                    Timber.d("ACTION_UP - $optionState")
                    when (optionState) {
                        OptionState.ADD_PRESSED -> {
                            updateOptionState(OptionState.OPTION)
                            onOptionButtonListener?.addOneForOptionNumber()
                        }
                        OptionState.OPTION -> {
                            // show delete icon
                            onOptionButtonListener?.let {
                                if (it.getOptionNumber() > MIN_OPTION_NUMBER) {
                                    updateOptionState(OptionState.DELETE)

                                    // 開始倒數 3s
                                    startCountingDown {
                                        // 時間到後，跳回原本的選項內容 & 更新 optionState = OptionState.OPTION
                                        updateOptionState(OptionState.OPTION)
                                    }
                                }
                            }
                        }
                        OptionState.DELETE_PRESSED -> {
                            // 從外部更新按鈕狀態，可能是：add or hidden
                            onOptionButtonListener?.minusOneForOptionNumber()
                        }
                        else -> {}
                    }
                }
            }
            return true
        }

    }

    init {
        binding.apply {
            setOnTouchListener(touchEventListener)
        }
    }

    private fun startCountingDown(seconds: Int = THREE_SECOND, onFinish: () -> Unit) {
        AppConstants.THREE_SEC_DELAY
        countingDownJob?.cancel()
        countingDownJob = coroutineScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                for (i in seconds downTo 1) {
                    println("Seconds remaining: $i")
                    delay(ONE_SEC_DELAY)
                }
            }
            if (optionState == OptionState.DELETED || optionState == OptionState.ADD) {
                return@launch
            }
            onFinish()
        }
    }

    /**
     * option button 的英文、數字轉換
     */
    fun updateOptionValueType(type: OptionValueType) {
        optionValueType = type
        if (optionState == OptionState.OPTION) {
            when (optionValueType) {
                OptionValueType.NUMBER -> {
                    binding.tvOption.text = optionIndex.toString()
                }
                OptionValueType.ALPHABET -> {
                    val letter = 'A' + (optionIndex - 1)
                    binding.tvOption.text = letter.toString()
                }
            }
        }
    }

    fun resetDeleteStatus() {
        countingDownJob?.cancel()
        updateOptionState(OptionState.OPTION)
    }

    private companion object {
        const val MIN_OPTION_NUMBER: Int = 2
        const val THREE_SECOND: Int = 3
    }

    interface OnOptionButtonListener {
        fun getOptionNumber(): Int
        fun minusOneForOptionNumber(): Boolean
        fun addOneForOptionNumber(): Boolean
        fun resetDeleteStatus()
    }
}

