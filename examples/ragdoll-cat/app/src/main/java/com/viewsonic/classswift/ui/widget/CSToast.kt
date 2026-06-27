package com.viewsonic.classswift.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewCsToastBinding

class CSToast @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: ViewCsToastBinding =
        ViewCsToastBinding.inflate(LayoutInflater.from(context), this, true)

    private var message: CharSequence = ""
    private var actionTitle: CharSequence = ""
    private var isSuccess = true
    private var hasAction = false

    init {
        // set Attr variable
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ClassSwiftToast,
            0,
            0
        ).apply {
            try {
                message = this.getString(R.styleable.ClassSwiftToast_cstText) ?: "Error text"
                actionTitle = this.getString(R.styleable.ClassSwiftToast_cstActionTitle) ?: "Error text"
                isSuccess = this.getBoolean(R.styleable.ClassSwiftToast_successStatus, true)
                hasAction = this.getBoolean(R.styleable.ClassSwiftToast_hasAction, false)
                setLayout()
            } finally {
                recycle()
            }
        }
    }

    private fun setLayout() {
        binding.tvMessage.text = message
        binding.tvAction.text = actionTitle
        if (isSuccess) {
            binding.ivIcon.setImageResource(R.drawable.ic_toast_success)
            binding.root.background =
                ContextCompat.getDrawable(context, R.drawable.bg_cs_toast_success)
            binding.tvAction.setTextColor(context.getColor(R.color.cs_toast_success))
        } else {
            binding.ivIcon.setImageResource(R.drawable.ic_toast_failed)
            binding.root.background =
                ContextCompat.getDrawable(context, R.drawable.bg_cs_toast_failed)
            binding.tvAction.setTextColor(context.getColor(R.color.red_400))
        }
        if (!hasAction) {
            binding.tvAction.visibility = View.GONE
        }
    }

    fun setActionOnClickListener(listener: OnClickListener) {
        binding.tvAction.setOnClickListener(listener)
    }

    fun setText(text: CharSequence) {
        message = text
        binding.tvMessage.text = text
    }

    fun setIsSuccess(isSuccess: Boolean) {
        this.isSuccess = isSuccess
        setLayout()
    }
}