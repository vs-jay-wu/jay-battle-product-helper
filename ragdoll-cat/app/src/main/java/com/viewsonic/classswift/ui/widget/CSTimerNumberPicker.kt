package com.viewsonic.classswift.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewTimerNumberPickerBinding
import com.viewsonic.classswift.ui.windowmodel.tool.listener.OnTimerNumberPickerListener

class CSTimerNumberPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewTimerNumberPickerBinding
    private var isBase6 = false
    private var listener: OnTimerNumberPickerListener? = null

    init {
        binding = ViewTimerNumberPickerBinding.inflate(LayoutInflater.from(context), this, true)
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CSTimerNumberPicker,
            0,
            0
        ).let {
            isBase6 = it.getBoolean(R.styleable.CSTimerNumberPicker_isBase6, false)
            setButton()
            it.recycle()
        }
    }


    private fun setButton() {
        binding.buttonNumberUp.setOnClickListener {
            binding.tvTimeValue.let {
                val value = it.text.toString().toInt()
                it.text = if (isBase6) {
                    if (value + 1 > 5) {
                        "0"
                    } else {
                        "${value + 1}"
                    }
                } else {
                    if (value + 1 > 9) {
                        "0"
                    } else {
                        "${value + 1}"
                    }
                }
                listener?.onClick(it.text.toString())
            }
        }

        binding.buttonNumberDown.setOnClickListener {
            binding.tvTimeValue.let {
                val value = it.text.toString().toInt()
                it.text = if (isBase6) {
                    if (value - 1 < 0) {
                        "5"
                    } else {
                        "${value - 1}"
                    }
                } else {
                    if (value - 1 < 0) {
                        "9"
                    } else {
                        "${value - 1}"
                    }
                }
                listener?.onClick(it.text.toString())
            }
        }
    }

    fun getValue() = binding.tvTimeValue.text.toString()

    fun setValue(value: String) {
        binding.tvTimeValue.text = value
    }

    fun setTimerNumberPickerClickListener(listener: OnTimerNumberPickerListener) {
        this.listener = listener
    }

}