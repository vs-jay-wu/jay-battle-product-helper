package com.viewsonic.classswift.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewLoadingAnimationBinding

class LoadingAnimation @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: ViewLoadingAnimationBinding =
        ViewLoadingAnimationBinding.inflate(LayoutInflater.from(context), this, true)

    @ColorInt
    private val backgroundColor: Int

    init {
        // set Attr variable
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.LoadingAnimation,
            0,
            0
        ).apply {
            try {
                backgroundColor = this.getColor(R.styleable.LoadingAnimation_animationBackgroundColor, context.getColor(R.color.white))
            } finally {
                recycle()
            }
        }
        setLayout()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (binding.lottieLoading.isAnimating) {
            cancelAnimation()
        }
    }

    private fun setLayout() {
        binding.cardView.setCardBackgroundColor(backgroundColor)
    }

    fun playAnimation() {
        binding.lottieLoading.playAnimation()
    }

    fun cancelAnimation() {
        binding.lottieLoading.cancelAnimation()
    }
}