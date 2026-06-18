package com.viewsonic.classswift.ui.widget.batchquiz

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.WidgetBatchQuizItemBarChartBinding

class CSBatchQuizBarChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: WidgetBatchQuizItemBarChartBinding =
        WidgetBatchQuizItemBarChartBinding.inflate(LayoutInflater.from(context), this)

    private val defaultColor = context.getColor(R.color.transparent)

    private var barChartMaxHeight: Float

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.BatchQuizBarChart,
            0,
            0
        ).apply {
            try {
                @ColorInt val barCharColor =
                    this.getColor(R.styleable.BatchQuizBarChart_barChartColor, defaultColor)
                binding.viewBarChart.setBackgroundColor(barCharColor)
                barChartMaxHeight =
                    this.getDimension(R.styleable.BatchQuizBarChart_barChartMaxHeight, context.resources.getDimension(R.dimen.batch_quiz_chart_bar_max_height))
            } finally {
                recycle()
            }
        }
    }

    fun setAnswerStatusCount(statusCount: Int, joinedStudentCount: Int) {
        binding.apply {
            // Calculate bar chart height based on status count and joined student count, should < 1
            if (statusCount < 1 || joinedStudentCount < 1) {
                viewBarChart.visibility = GONE
            } else {
                viewBarChart.visibility = VISIBLE
                val barChartHeight = (statusCount * barChartMaxHeight / joinedStudentCount).toInt()
                val params = LayoutParams(LayoutParams.MATCH_PARENT, barChartHeight)
                params.startToStart = LayoutParams.PARENT_ID
                params.endToEnd = LayoutParams.PARENT_ID
                params.bottomToTop = tvCount.id
                params.bottomMargin = binding.root.resources.getDimension(R.dimen.batch_quiz_chart_bar_bottom_padding).toInt()
                viewBarChart.layoutParams = params
                viewBarChart.requestLayout()
            }
            tvCount.text = statusCount.toString()
        }
    }
}