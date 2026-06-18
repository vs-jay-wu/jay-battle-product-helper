package com.viewsonic.classswift.ui.widget.quiz

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.response.DiscloseQuizResponse
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.databinding.ViewMultipleChoiceQuizResultsOverviewBinding
import com.viewsonic.classswift.utils.QuizUtils

class MultipleChoiceQuizResultsOverview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding: ViewMultipleChoiceQuizResultsOverviewBinding =
        ViewMultipleChoiceQuizResultsOverviewBinding.inflate(LayoutInflater.from(context), this)
    private val answerImageList: List<ImageView> = listOf(binding.ivCorrectAnswerPlaceholder1, binding.ivCorrectAnswerPlaceholder2, binding.ivCorrectAnswerPlaceholder3,
                                                            binding.ivCorrectAnswerPlaceholder4, binding.ivCorrectAnswerPlaceholder5, binding.ivCorrectAnswerPlaceholder6)

    init {
        binding.apply {
            setCorrectAnswerCount(0)
            setIncorrectAnswerCount(0)
            setNoAnswerCount(0)
        }
    }

    fun setCorrectAnswerImages(correctAnswers: List<Int>, isNumberOrAlphabet: QuizOptionType) {
        for (index in 0 until answerImageList.size) {
            val optionId = index + 1
            if (correctAnswers.contains(optionId)) {
                QuizUtils.getAnsweringIcon(isNumberOrAlphabet, optionId)?.let {
                    answerImageList[index].setImageResource(it)
                }
                answerImageList[index].visibility = VISIBLE
            } else {
                answerImageList[index].visibility = GONE
            }
        }
    }

    fun setAllParticipants(correctCount: Int, incorrectCount: Int, noAnswerCount: Int) {
        setCorrectAnswerCount(correctCount)
        setIncorrectAnswerCount(incorrectCount)
        setNoAnswerCount(noAnswerCount)
    }

    @SuppressLint("SetTextI18n")
    private fun setCorrectAnswerCount(count: Int) {
        binding.tvCorrectCount.text = String.format(context.getString(R.string.quiz_info_participants_count), "$count")
    }

    @SuppressLint("SetTextI18n")
    private fun setIncorrectAnswerCount(count: Int) {
        binding.tvIncorrectCount.text = String.format(context.getString(R.string.quiz_info_participants_count), "$count")
    }

    @SuppressLint("SetTextI18n")
    private fun setNoAnswerCount(count: Int) {
        binding.tvNoAnswerCount.text = String.format(context.getString(R.string.quiz_info_participants_count), "$count")
    }
}