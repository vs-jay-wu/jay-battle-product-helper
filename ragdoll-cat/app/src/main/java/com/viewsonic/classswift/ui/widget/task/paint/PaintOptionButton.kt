package com.viewsonic.classswift.ui.widget.task.paint

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewPaintViewOptionButtonBinding
import com.viewsonic.classswift.utils.extension.dpToPx
import androidx.core.content.withStyledAttributes

class PaintOptionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val defaultSize = 32f.dpToPx().toInt()

    private val binding: ViewPaintViewOptionButtonBinding =
        ViewPaintViewOptionButtonBinding.inflate(
            LayoutInflater.from(context),
            this
        )

    init {
        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.PaintOptionButton) {
                val iconResId = getResourceId(
                    R.styleable.PaintOptionButton_iconResource,
                    0
                )

                if (iconResId != 0) {
                    setIconResId(iconResId)
                }
            }
        }
    }

    fun setIconResourceId(resId: Int) {
        setIconResId(resId = resId)
    }

    fun setOptionSelected(isSelected: Boolean) {
        val bgResId = if (isSelected) {
            R.drawable.bg_paint_option_selected
        } else {
            android.R.color.transparent
        }
        setBackgroundResource(bgResId)
    }

    private fun setIconResId(resId: Int) {
        binding.ivIcon.setImageResource(resId)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = resolveSize(defaultSize, widthMeasureSpec)
        val measuredHeight = resolveSize(defaultSize, heightMeasureSpec)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.EXACTLY)
        )
    }
}