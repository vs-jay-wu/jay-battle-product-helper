package com.viewsonic.classswift.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewTooltipBinding
import com.viewsonic.classswift.utils.SpannableStringUtils

class CSTooltipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr){
    private var binding: ViewTooltipBinding =
        ViewTooltipBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        if (!isInEditMode) {
            binding.tvNegative.text =
                SpannableStringUtils.replaceStringWithUnderline(R.string.tutorial_dialog_action_skip_all)
        }
        setArrowPosition(ArrowPosition.NONE)
        // Prevent from passing onClick event to parent.
        setOnClickListener{}
    }

    fun setTitle(title: String) {
        binding.tvTitle.text = title
    }

    fun setDescription(description: String) {
        binding.tvDescription.text = description
    }

    fun setLottieAnimation(rawResId: Int) {
        binding.lavImage.setAnimation(rawResId)
    }

    fun setLottieImage(rawResId: Int) {
        binding.lavImage.setImageResource(rawResId)
    }

    fun setNegativeButtonTitle(titleResId: Int) {
        binding.tvNegative.text = SpannableStringUtils.replaceStringWithUnderline(titleResId)
    }

    fun showNegativeButton() {
        binding.tvNegative.visibility = View.VISIBLE
    }

    fun hideNegativeButton() {
        binding.tvNegative.visibility = View.GONE
    }

    fun setPositiveButtonTitle(titleResId: Int) {
        binding.tvPositive.text = context.getString(titleResId)
    }

    fun setNegativeOnClickListener(listener: OnClickListener) {
        binding.tvNegative.setOnClickListener(listener)
    }

    fun setPositiveOnClickListener(listener: OnClickListener) {
        binding.tvPositive.setOnClickListener(listener)
    }

    fun setArrowPosition(position: ArrowPosition) {
        with(binding) {
            ivTopArrow.visibility = if (position == ArrowPosition.TOP) VISIBLE else GONE
            ivBottomArrow.visibility = if (position == ArrowPosition.BOTTOM) VISIBLE else GONE
            ivLeftArrow.visibility = if (position == ArrowPosition.LEFT) VISIBLE else GONE
            ivRightArrow.visibility = if (position == ArrowPosition.RIGHT) VISIBLE else GONE
        }
    }

    enum class ArrowPosition {
        NONE,
        TOP,
        LEFT,
        BOTTOM,
        RIGHT
    }
}