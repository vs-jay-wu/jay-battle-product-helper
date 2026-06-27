package com.viewsonic.classswift.ui.window.adapter

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.QuizAnsweringInfo
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.data.quiz.QuizType.Companion.isMultipleChoice
import com.viewsonic.classswift.data.quiz.QuizType.Companion.isPoll
import com.viewsonic.classswift.databinding.ItemMvbQuizResultStudentBinding
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState
import com.viewsonic.classswift.ui.windowmodel.quiz.TrueFalseWindowModel

/**
 * Result-stage student adapter for VSFT-7265.
 *
 * Shows each student's answer result (CORRECT / INCORRECT / NOT_SUBMITTED / ABSENT)
 * with colored header + answer label, and supports highlight dimming when a bar is selected.
 *
 * Data model: [QuizAnsweringInfo] (reused; [AnsweringState] + `answerOption`).
 * External state: [correctDiscloseId] drives correct/incorrect classification;
 * [highlightedOptionId] drives dim-others effect.
 */
class MvbQuizResultAdapter :
    ListAdapter<QuizAnsweringInfo, MvbQuizResultAdapter.ViewHolder>(DIFF) {

    private var correctDiscloseIds: List<Int> = emptyList()
    private var highlightedOptionId: Int? = null

    fun setCorrectDiscloseIds(ids: List<Int>) {
        if (correctDiscloseIds == ids) return
        correctDiscloseIds = ids
        notifyItemRangeChanged(0, itemCount, PAYLOAD_REBIND_ALL)
    }

    /** Backwards-compatible single-id setter (T/F / single-select). */
    fun setCorrectDiscloseId(id: Int?) {
        setCorrectDiscloseIds(if (id == null) emptyList() else listOf(id))
    }

    fun setHighlightedOptionId(optionId: Int?) {
        if (highlightedOptionId == optionId) return
        highlightedOptionId = optionId
        notifyItemRangeChanged(0, itemCount, PAYLOAD_HIGHLIGHT)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMvbQuizResultStudentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), correctDiscloseIds, highlightedOptionId)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }
        val item = getItem(position)
        if (payloads.contains(PAYLOAD_HIGHLIGHT)) {
            holder.applyHighlight(item, highlightedOptionId)
        }
        if (payloads.contains(PAYLOAD_REBIND_ALL)) {
            holder.bind(item, correctDiscloseIds, highlightedOptionId)
        }
    }

    class ViewHolder(
        private val binding: ItemMvbQuizResultStudentBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(info: QuizAnsweringInfo, correctDiscloseIds: List<Int>, highlightedOptionId: Int?) {
            val context = binding.root.context
            val state = classify(info, correctDiscloseIds)
            applyState(context, state, info)
            binding.tvResultStudentName.text = info.displayName
            applyHighlight(info, highlightedOptionId)
        }

        fun applyHighlight(info: QuizAnsweringInfo, highlightedOptionId: Int?) {
            val match = matchesHighlight(info, highlightedOptionId)
            binding.mcvResultStudentRoot.alpha = if (match) FULL_ALPHA else DIM_ALPHA
        }

        private fun applyState(
            context: Context,
            state: ResultState,
            info: QuizAnsweringInfo,
        ) {
            when (state) {
                ResultState.CORRECT -> applyCorrect(context, info)
                ResultState.INCORRECT -> applyIncorrect(context, info)
                ResultState.NOT_SUBMITTED -> applyNotSubmitted(context)
                ResultState.ABSENT -> applyAbsent(context)
            }
        }

        private fun applyCorrect(context: Context, info: QuizAnsweringInfo) {
            binding.mcvResultStudentRoot.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.color_E7F7D0),
            )
            binding.tvResultStudentName.setBackgroundResource(R.drawable.bg_result_student_header_correct)
            binding.tvResultStudentName.setTextColor(ContextCompat.getColor(context, R.color.neutral_0))
            renderAnswer(context, info)
        }

        private fun applyIncorrect(context: Context, info: QuizAnsweringInfo) {
            binding.mcvResultStudentRoot.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.color_FFECEF),
            )
            binding.tvResultStudentName.setBackgroundResource(R.drawable.bg_result_student_header_incorrect)
            binding.tvResultStudentName.setTextColor(ContextCompat.getColor(context, R.color.neutral_0))
            renderAnswer(context, info)
        }

        private fun applyNotSubmitted(context: Context) {
            binding.mcvResultStudentRoot.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.neutral_0),
            )
            binding.tvResultStudentName.setBackgroundResource(R.drawable.bg_result_student_header_not_submitted)
            binding.tvResultStudentName.setTextColor(ContextCompat.getColor(context, R.color.neutral_900))
            showFallbackText(
                context,
                text = context.getString(R.string.quiz_mvb_cell_not_submitted),
                textColorRes = R.color.neutral_500,
                sizeSp = ANSWER_SMALL_SP,
                bold = false,
            )
        }

        private fun applyAbsent(context: Context) {
            binding.mcvResultStudentRoot.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.color_EBEBEB),
            )
            binding.tvResultStudentName.setBackgroundResource(R.drawable.bg_result_student_header_absent)
            binding.tvResultStudentName.setTextColor(ContextCompat.getColor(context, R.color.color_999999))
            showFallbackText(
                context,
                text = context.getString(R.string.quiz_mvb_cell_absent),
                textColorRes = R.color.color_999999,
                sizeSp = ANSWER_SMALL_SP,
                bold = false,
            )
        }

        /** CORRECT / INCORRECT answer rendering — chips for MC/Poll, big bold label for T/F. */
        private fun renderAnswer(context: Context, info: QuizAnsweringInfo) {
            if (info.quizType.isMultipleChoice() || info.quizType.isPoll()) {
                showAnswerChips(context, info)
            } else {
                showFallbackText(
                    context,
                    text = formatAnswerLabel(info),
                    textColorRes = R.color.neutral_900,
                    sizeSp = ANSWER_BIG_SP,
                    bold = true,
                )
            }
        }

        private fun showAnswerChips(context: Context, info: QuizAnsweringInfo) {
            binding.tvResultStudentAnswer.visibility = View.GONE
            val container = binding.llResultStudentAnswerChips
            container.removeAllViews()
            container.visibility = View.VISIBLE
            val gapPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                CHIP_GAP_DP,
                context.resources.displayMetrics,
            ).toInt()
            val inflater = LayoutInflater.from(context)
            info.answerOption.forEachIndexed { index, optionIdx ->
                val chip = inflater.inflate(
                    R.layout.view_mvb_result_student_answer_chip,
                    container,
                    false,
                ) as AppCompatTextView
                chip.text = formatSingleOptionLabel(info, optionIdx)
                if (index > 0) {
                    (chip.layoutParams as LinearLayout.LayoutParams).marginStart = gapPx
                }
                container.addView(chip)
            }
        }

        private fun showFallbackText(
            context: Context,
            text: String,
            textColorRes: Int,
            sizeSp: Float,
            bold: Boolean,
        ) {
            binding.llResultStudentAnswerChips.visibility = View.GONE
            binding.tvResultStudentAnswer.visibility = View.VISIBLE
            binding.tvResultStudentAnswer.text = text
            binding.tvResultStudentAnswer.setTextColor(ContextCompat.getColor(context, textColorRes))
            binding.tvResultStudentAnswer.textSize = sizeSp
            binding.tvResultStudentAnswer.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        private fun classify(info: QuizAnsweringInfo, correctDiscloseIds: List<Int>): ResultState {
            return when (info.answeringState) {
                AnsweringState.ABSENT -> ResultState.ABSENT
                AnsweringState.NOT_ANSWER -> ResultState.NOT_SUBMITTED
                AnsweringState.ANSWERED -> {
                    if (info.quizType.isPoll()) ResultState.CORRECT
                    else if (correctDiscloseIds.isEmpty()) ResultState.INCORRECT
                    else if (studentChoseCorrect(info, correctDiscloseIds)) ResultState.CORRECT
                    else ResultState.INCORRECT
                }
            }
        }

        companion object {
            private const val ANSWER_BIG_SP = 16f
            private const val ANSWER_SMALL_SP = 9.33f
            private const val FULL_ALPHA = 1.0f
            private const val DIM_ALPHA = 0.3f
            private const val CHIP_GAP_DP = 2.67f

            fun formatAnswerLabel(info: QuizAnsweringInfo): String {
                if (info.quizType == QuizType.TRUE_FALSE) {
                    return when (info.answerOption.firstOrNull()) {
                        TrueFalseWindowModel.TRUE_OPTION_INDEX -> "T"
                        TrueFalseWindowModel.FALSE_OPTION_INDEX -> "F"
                        else -> ""
                    }
                }
                return info.answerOption.joinToString("") { formatSingleOptionLabel(info, it) }
            }

            /**
             * Single option label (A/B/C... or 1/2/3...) for MC chip rendering.
             * [optionIdx] is the 1-based option id from the backend (matches legacy
             * `MultipleChoiceStartWindow.setAnswerOptionInfoView` convention).
             */
            fun formatSingleOptionLabel(info: QuizAnsweringInfo, optionIdx: Int): String {
                return if (info.quizOptionType == QuizOptionType.ALPHABET) {
                    ('A' + (optionIdx - 1)).toString()
                } else {
                    optionIdx.toString()
                }
            }

            /**
             * Student is correct when their answer SET equals the correct answer SET.
             * Both student [answerOption] and [correctDiscloseIds] use the same
             * 1-based convention (T=1/F=2 for true/false; option id 1..N for MC).
             */
            fun studentChoseCorrect(info: QuizAnsweringInfo, correctDiscloseIds: List<Int>): Boolean {
                if (info.quizType == QuizType.TRUE_FALSE) {
                    return info.answerOption.firstOrNull() == correctDiscloseIds.firstOrNull()
                }
                return info.answerOption.toSet() == correctDiscloseIds.toSet()
            }

            fun matchesHighlight(info: QuizAnsweringInfo, highlightedOptionId: Int?): Boolean {
                if (highlightedOptionId == null) return true
                if (highlightedOptionId == NOT_SUBMITTED_OPTION_PSEUDO_ID) {
                    return info.answeringState == AnsweringState.NOT_ANSWER
                }
                // Both highlightedOptionId and student answerOption use the same
                // 1-based convention (T/F + MC).
                return info.answerOption.contains(highlightedOptionId)
            }

            const val NOT_SUBMITTED_OPTION_PSEUDO_ID = -1
        }
    }

    private enum class ResultState { CORRECT, INCORRECT, NOT_SUBMITTED, ABSENT }

    companion object {
        private const val PAYLOAD_HIGHLIGHT = "payload_highlight"
        private const val PAYLOAD_REBIND_ALL = "payload_rebind_all"

        private val DIFF = object : DiffUtil.ItemCallback<QuizAnsweringInfo>() {
            override fun areItemsTheSame(old: QuizAnsweringInfo, new: QuizAnsweringInfo) =
                old.studentId == new.studentId

            override fun areContentsTheSame(old: QuizAnsweringInfo, new: QuizAnsweringInfo) =
                old == new
        }
    }
}
