package com.viewsonic.classswift.ui.widget.batchquiz

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.BatchQuizSummaryInfo
import com.viewsonic.classswift.data.quiz.QuizType.Companion.getString
import com.viewsonic.classswift.databinding.WidgetBatchQuizReultTitleBinding
import com.viewsonic.classswift.ui.widget.batchquiz.adapter.BatchQuizResultAdapter


@SuppressLint("SetTextI18n")
class CSBatchQuizItemTitle @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: WidgetBatchQuizReultTitleBinding =
        WidgetBatchQuizReultTitleBinding.inflate(LayoutInflater.from(context), this)

    private var listener: BatchQuizResultAdapter.OnItemTitleClickedListener? = null
    private var summaryInfo: BatchQuizSummaryInfo = BatchQuizSummaryInfo()

    init {
        binding.root.setBackgroundResource(R.drawable.selector_batch_quiz_item_title)
        binding.root.isClickable = true
        // fake data
        binding.tvQuizNo.text = "1"
        binding.tvQuizCategory.text = "Multiple Choice"
        initOnClick()
    }

    private fun initOnClick() {
        binding.root.setOnClickListener {
            listener?.onShowDetailsResult(summaryInfo)
        }
    }

    fun setOnClickListener(listener: BatchQuizResultAdapter.OnItemTitleClickedListener) {
        this.listener = listener
    }

    fun setInfo(info: BatchQuizSummaryInfo) {
        summaryInfo = info
        binding.apply {
            tvQuizNo.text = info.sequence.toString()
            tvQuizCategory.text = info.quizType.getString(context)
        }
    }
}