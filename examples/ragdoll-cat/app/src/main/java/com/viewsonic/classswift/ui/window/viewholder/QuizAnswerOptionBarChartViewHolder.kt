package com.viewsonic.classswift.ui.window.viewholder


import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.AnswerOptionInfo
import com.viewsonic.classswift.databinding.ItemQuizScoreBarChartBinding
import com.viewsonic.classswift.ui.widget.quiz.enums.AnswerResultState
import com.viewsonic.classswift.ui.window.adapter.OptionChartBarViewHolderListener

@SuppressLint("SetTextI18n")
class QuizAnswerOptionBarChartViewHolder(
    private val binding: ItemQuizScoreBarChartBinding,
    private val itemClickListener: OptionChartBarViewHolderListener
) : RecyclerView.ViewHolder(binding.root) {
    private val barChartMaxWidth = binding.root.resources.getDimension(R.dimen.quiz_max_chart_bar_width)
    fun onBind(info: AnswerOptionInfo) {
        val barChartWidth = (info.answerPercent() * barChartMaxWidth).toInt()
        if (barChartWidth <= 0) {
            binding.viewChartBar.visibility = View.GONE
            val countParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
            countParams.startToStart = LayoutParams.PARENT_ID
            countParams.marginStart = binding.root.resources.getDimension(R.dimen.quiz_option_padding).toInt()
            binding.tvCount.layoutParams = countParams
            val answeredParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
            answeredParams.startToStart = LayoutParams.PARENT_ID
            answeredParams.endToStart = binding.tvCount.id
            answeredParams.marginEnd = binding.root.resources.getDimension(R.dimen.quiz_no_answer_end_padding).toInt()
            binding.tvAnswered.layoutParams = answeredParams
            val noAnswerParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
            noAnswerParams.startToStart = LayoutParams.PARENT_ID
            noAnswerParams.endToStart = binding.tvCount.id
            noAnswerParams.marginEnd = binding.root.resources.getDimension(R.dimen.quiz_no_answer_end_padding).toInt()
            binding.tvNoAnswer.layoutParams = noAnswerParams
        } else {
            val params = LayoutParams(barChartWidth, ViewGroup.LayoutParams.MATCH_PARENT)
            params.startToStart = LayoutParams.PARENT_ID
            params.marginStart = binding.root.resources.getDimension(R.dimen.quiz_option_padding).toInt()
            binding.viewChartBar.layoutParams = params
        }

        info.getAnswerIcon()?.let { resID ->
            binding.ivAnswerIcon.background = ResourcesCompat.getDrawable(binding.root.resources, resID, null)
        }
        when (info.answerResultState) {
            AnswerResultState.CORRECT -> {
                binding.ivAnswerIcon.isVisible = true
                binding.tvAnswered.isVisible = false
                binding.tvNoAnswer.isVisible = false
                binding.viewChartBar.background = ResourcesCompat.getDrawable(binding.root.resources, R.drawable.bg_quiz_correct, null)
            }

            AnswerResultState.INCORRECT -> {
                binding.ivAnswerIcon.isVisible = true
                binding.tvAnswered.isVisible = false
                binding.tvNoAnswer.isVisible = false
                binding.viewChartBar.background = ResourcesCompat.getDrawable(binding.root.resources, R.drawable.bg_quiz_incorrect, null)
            }

            AnswerResultState.ANSWERED -> {
                binding.ivAnswerIcon.isVisible = false
                binding.tvAnswered.isVisible = true
                binding.tvNoAnswer.isVisible = false
                binding.viewChartBar.background = ResourcesCompat.getDrawable(binding.root.resources, R.drawable.bg_quiz_correct, null)
            }

            AnswerResultState.NO_ANSWER -> {
                binding.ivAnswerIcon.isVisible = false
                binding.tvAnswered.isVisible = false
                binding.tvNoAnswer.isVisible = true
                binding.viewChartBar.background = ResourcesCompat.getDrawable(binding.root.resources, R.drawable.bg_quiz_no_answer, null)
            }
            //BarChart doesn't have absent status
            AnswerResultState.ABSENT -> {}
        }
        binding.tvCount.text = info.answerCount.toString()
        binding.root.setOnClickListener {
            itemClickListener.itemClicked(info.position)
        }
    }
}
