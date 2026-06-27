package com.viewsonic.classswift.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewMvbLoadingAnimationBinding

class MvbLoadingAnimation @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: ViewMvbLoadingAnimationBinding =
        ViewMvbLoadingAnimationBinding.inflate(LayoutInflater.from(context), this, true)

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
        if (binding.lavLoading.isAnimating) {
            cancelAnimation()
        }
    }

    private fun setLayout() {
        binding.cvContainer.setCardBackgroundColor(backgroundColor)
    }

    fun playAnimation() {
        binding.lavLoading.playAnimation()
    }

    fun cancelAnimation() {
        binding.lavLoading.cancelAnimation()
    }
}