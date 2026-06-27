package com.viewsonic.classswift.ui.widget.quiz

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.viewsonic.classswift.data.info.AnswerOptionInfo
import com.viewsonic.classswift.databinding.ViewAnswerOptionInfoBinding
import com.viewsonic.classswift.ui.window.adapter.OptionChartBarViewHolderListener
import com.viewsonic.classswift.ui.window.adapter.QuizAnswerOptionAdapter


class ClassSwiftAnswerOptionInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), OptionChartBarViewHolderListener {

    private var eventListener: AnswerOptionEventListener? = null
    private var answerOptionListAdapter: QuizAnswerOptionAdapter = QuizAnswerOptionAdapter(this)
    private val binding: ViewAnswerOptionInfoBinding =
        ViewAnswerOptionInfoBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        binding.rvOptions.layoutManager = LinearLayoutManager(context)
        binding.rvOptions.adapter = answerOptionListAdapter
        setClickAction()
    }

    private fun setClickAction() {
        binding.tvIncreasePoint.setOnClickListener {
            eventListener?.addPoint()
        }
    }

    fun setAnswerTitle(stringResId: Int) {
        binding.tvTitle.text = context.getString(stringResId)
    }

    fun setIncreaseTitle(stringResId: Int) {
        binding.tvIncreaseTitle.text = context.getString(stringResId)
    }

    fun setEventListener(listener: AnswerOptionEventListener) {
        eventListener = listener
    }

    fun setScoreInfos(infos: ArrayList<AnswerOptionInfo>) {
        answerOptionListAdapter.submitList(infos)
    }

    fun resetAdapter() {
        answerOptionListAdapter = QuizAnswerOptionAdapter(this)
        binding.rvOptions.adapter = answerOptionListAdapter
    }

    override fun itemClicked(position: Int) {
        eventListener?.clickOptionItem(position)
    }
}

interface AnswerOptionEventListener {
    fun addPoint()
    fun clickOptionItem(position: Int)
}