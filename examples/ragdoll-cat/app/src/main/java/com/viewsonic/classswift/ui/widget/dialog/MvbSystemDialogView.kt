package com.viewsonic.classswift.ui.widget.dialog

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewMvbSystemDialogBinding

class MvbSystemDialogView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewMvbSystemDialogBinding.inflate(LayoutInflater.from(context), this)

    init {
        visibility = GONE
        background = ResourcesCompat.getDrawable(resources, R.drawable.bg_mvb_system_dialog_mask, null)
    }

    fun setup(
        title: String,
        message: String,
        positiveText: String,
        onPositive: () -> Unit,
        negativeText: String,
        onNegative: () -> Unit = {},
    ): MvbSystemDialogView {
        binding.tvTitle.text = title
        binding.tvMessage.text = message
        binding.btnPositive.text = positiveText
        binding.btnNegative.text = negativeText
        binding.btnPositive.setOnClickListener { onPositive() }
        binding.btnNegative.setOnClickListener { onNegative(); dismiss() }
        binding.ivBtnClose.setOnClickListener { onNegative(); dismiss() }
        return this
    }

    fun show() {
        visibility = VISIBLE
    }

    fun dismiss() {
        visibility = GONE
    }
}
