package com.viewsonic.classswift.ui.widget.quiz.result

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import com.viewsonic.classswift.databinding.WidgetResultAnalyticChipBinding

/**
 * Overview tab analytic chip: outlined badge + icon + label + big count + "students".
 */
class CSResultAnalyticChip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    data class Data(
        @DrawableRes val iconRes: Int?,
        val label: String,
        val count: Int,
    )

    private val binding: WidgetResultAnalyticChipBinding

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        binding = WidgetResultAnalyticChipBinding.inflate(LayoutInflater.from(context), this, true)
    }

    fun setData(data: Data) {
        val iconRes = data.iconRes
        if (iconRes != null) {
            binding.ivResultAnalyticIcon.setImageResource(iconRes)
            binding.ivResultAnalyticIcon.visibility = View.VISIBLE
        } else {
            binding.ivResultAnalyticIcon.visibility = View.GONE
        }
        binding.tvResultAnalyticLabel.text = data.label
        binding.tvResultAnalyticCount.text = data.count.toString()
    }
}
