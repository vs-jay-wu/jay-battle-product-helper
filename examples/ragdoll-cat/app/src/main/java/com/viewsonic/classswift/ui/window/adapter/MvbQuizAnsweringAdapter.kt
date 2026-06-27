package com.viewsonic.classswift.ui.window.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.QuizAnsweringInfo
import com.viewsonic.classswift.data.quiz.QuizOptionType
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.databinding.ItemMvbQuizAnsweringBinding
import com.viewsonic.classswift.ui.widget.quiz.enums.AnsweringState
import com.viewsonic.classswift.ui.windowmodel.quiz.TrueFalseWindowModel
import com.viewsonic.classswift.utils.extension.dpToPx

/**
 * MVB-specific quiz answering adapter for Quizzing phase.
 * Shows "Submitted / Not submitted / Absent" text per Figma spec (node 3215:33808).
 */
class MvbQuizAnsweringAdapter(
    private val itemClickCallback: (QuizAnsweringInfo) -> Unit,
) : ListAdapter<QuizAnsweringInfo, MvbQuizAnsweringAdapter.ViewHolder>(DIFF) {

    private var showStudentsName: Boolean = true
    private var isResult: Boolean = false
    private var highlightedOptionId: Int? = null

    fun setShowStudentsName(show: Boolean) {
        if (showStudentsName == show) return
        showStudentsName = show
        notifyItemRangeChanged(0, itemCount, Bundle().apply { putBoolean(PAYLOAD_SHOW_STUDENTS_NAME, show) })
    }

    fun setIsResult(result: Boolean) {
        if (isResult == result) return
        isResult = result
        notifyItemRangeChanged(0, itemCount, Bundle().apply { putBoolean(PAYLOAD_IS_RESULT, result) })
    }

    fun setHighlightedOptionId(optionId: Int?) {
        if (highlightedOptionId == optionId) return
        highlightedOptionId = optionId
        notifyItemRangeChanged(0, itemCount, PAYLOAD_HIGHLIGHT)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMvbQuizAnsweringBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ViewHolder(binding, itemClickCallback)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), showStudentsName, isResult, highlightedOptionId)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            holder.bind(getItem(position), showStudentsName, isResult, highlightedOptionId)
            return
        }
        if (payloads.contains(PAYLOAD_HIGHLIGHT)) {
            holder.applyHighlight(getItem(position), highlightedOptionId)
            return
        }
        payloads.forEach { payload ->
            if (payload is Bundle) {
                if (payload.containsKey(PAYLOAD_SHOW_STUDENTS_NAME)) {
                    holder.applyShowStudentsName(payload.getBoolean(PAYLOAD_SHOW_STUDENTS_NAME))
                }
                if (payload.containsKey(PAYLOAD_IS_RESULT)) {
                    holder.bind(getItem(position), showStudentsName, isResult, highlightedOptionId)
                }
            }
        }
    }

    class ViewHolder(
        private val binding: ItemMvbQuizAnsweringBinding,
        private val callback: (QuizAnsweringInfo) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(info: QuizAnsweringInfo, showStudentsName: Boolean, isResult: Boolean, highlightedOptionId: Int?) {
            val context = binding.root.context
            binding.mcvRoot.setOnClickListener {
                val isClickable = info.answeringState == AnsweringState.ANSWERED &&
                    (!isResult || info.quizType == QuizType.SHORT_ANSWER)
                if (isClickable) callback(info)
            }
            binding.tvNumberAndName.text = info.displayName
            applyShowStudentsName(showStudentsName)
            applyHighlight(info, highlightedOptionId)

            when (info.answeringState) {
                AnsweringState.ANSWERED -> applySubmitted(context, info, isResult)
                AnsweringState.NOT_ANSWER -> applyNotSubmitted(context, isResult)
                AnsweringState.ABSENT -> applyAbsent(context)
            }
        }

        fun applyShowStudentsName(show: Boolean) {
            binding.tvNumberAndName.isVisible = show
        }

        fun applyHighlight(info: QuizAnsweringInfo, highlightedOptionId: Int?) {
            binding.mcvRoot.alpha = if (matchesHighlight(info, highlightedOptionId)) FULL_ALPHA else DIM_ALPHA
        }

        private fun applySubmitted(context: Context, info: QuizAnsweringInfo, isResult: Boolean) {
            val isShortAnswer = info.quizType == QuizType.SHORT_ANSWER
            val (cardBg, headerBg) = if (isResult) {
                R.color.color_E7F7D0 to R.color.color_48720F
            } else {
                R.color.color_EDEDFD to R.color.color_4848F0
            }
            binding.mcvRoot.setCardBackgroundColor(ContextCompat.getColor(context, cardBg))
            binding.mcvRoot.strokeWidth = 0
            binding.tvNumberAndName.setBackgroundColor(ContextCompat.getColor(context, headerBg))
            binding.tvNumberAndName.setTextColor(
                ContextCompat.getColor(context, R.color.neutral_0),
            )
            binding.viewDivider.visibility = View.GONE

            val hasAnswer = if (isShortAnswer) info.answerStringData.isNotEmpty() else info.answerOption.isNotEmpty()
            if (info.canShowAnswer && hasAnswer) {
                if (isShortAnswer) {
                    binding.tvAnswerText.text = info.answerStringData
                    showOnlyStateView(StateView.ANSWER_TEXT)
                } else {
                    showAnswerChips(context, info)
                }
            } else {
                showOnlyStateView(StateView.SUBMITTED)
            }
        }

        /**
         * Both T/F and MC use 1-based optionId from the backend (matches
         * [TrueFalseWindowModel.TRUE_OPTION_INDEX] and legacy
         * `MultipleChoiceStartWindow.setAnswerOptionInfoView`).
         */
        private fun showAnswerChips(context: Context, info: QuizAnsweringInfo) {
            val container = binding.llAnswerChips
            container.removeAllViews()
            val gapPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                CHIP_GAP_DP,
                context.resources.displayMetrics,
            ).toInt()
            val inflater = LayoutInflater.from(context)
            val labels: List<String> = when (info.quizType) {
                QuizType.TRUE_FALSE -> {
                    val label = when (info.answerOption.firstOrNull()) {
                        TrueFalseWindowModel.TRUE_OPTION_INDEX -> "T"
                        TrueFalseWindowModel.FALSE_OPTION_INDEX -> "F"
                        else -> {
                            showOnlyStateView(StateView.SUBMITTED)
                            return
                        }
                    }
                    listOf(label)
                }
                else -> info.answerOption.map { optionIdx ->
                    if (info.quizOptionType == QuizOptionType.ALPHABET) {
                        ('A' + (optionIdx - 1)).toString()
                    } else {
                        optionIdx.toString()
                    }
                }
            }
            showOnlyStateView(StateView.ANSWER_CHIPS)
            labels.forEachIndexed { index, label ->
                val chip = inflater.inflate(
                    R.layout.view_mvb_result_student_answer_chip,
                    container,
                    false,
                ) as AppCompatTextView
                chip.text = label
                if (index > 0) {
                    (chip.layoutParams as LinearLayout.LayoutParams).marginStart = gapPx
                }
                container.addView(chip)
            }
        }

        private fun applyNotSubmitted(context: Context, isResult: Boolean) {
            val borderColorRes = if (isResult) {
                R.color.neutral_300
            } else {
                R.color.color_4848F0
            }
            binding.mcvRoot.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.neutral_0),
            )
            binding.mcvRoot.strokeWidth = itemView.resources.getDimensionPixelSize(R.dimen.mvb_border_100)
            binding.mcvRoot.setStrokeColor(
                ColorStateList.valueOf(ContextCompat.getColor(context, borderColorRes)),
            )
            binding.tvNumberAndName.setBackgroundColor(
                ContextCompat.getColor(context, R.color.neutral_0),
            )
            binding.tvNumberAndName.setTextColor(
                ContextCompat.getColor(context, R.color.neutral_900),
            )
            binding.viewDivider.visibility = View.VISIBLE
            binding.viewDivider.setBackgroundColor(
                ContextCompat.getColor(context, borderColorRes),
            )
            showOnlyStateView(StateView.NOT_SUBMITTED)
        }

        private fun applyAbsent(context: Context) {
            binding.mcvRoot.setCardBackgroundColor(
                ContextCompat.getColor(context, R.color.color_EBEBEB),
            )
            binding.mcvRoot.strokeWidth = itemView.resources.getDimensionPixelSize(R.dimen.mvb_border_100)
            binding.mcvRoot.setStrokeColor(
                ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.color_E5E5E5),
                ),
            )
            binding.tvNumberAndName.setBackgroundColor(
                ContextCompat.getColor(context, R.color.color_EBEBEB),
            )
            binding.tvNumberAndName.setTextColor(
                ContextCompat.getColor(context, R.color.neutral_500),
            )
            binding.viewDivider.visibility = View.VISIBLE
            binding.viewDivider.setBackgroundColor(
                ContextCompat.getColor(context, R.color.color_E5E5E5),
            )
            showOnlyStateView(StateView.ABSENT)
        }

        private fun showOnlyStateView(which: StateView) {
            binding.tvSubmitted.visibility = if (which == StateView.SUBMITTED) View.VISIBLE else View.GONE
            binding.tvAnswerText.visibility = if (which == StateView.ANSWER_TEXT) View.VISIBLE else View.GONE
            binding.llAnswerChips.visibility = if (which == StateView.ANSWER_CHIPS) View.VISIBLE else View.GONE
            binding.tvNotSubmitted.visibility = if (which == StateView.NOT_SUBMITTED) View.VISIBLE else View.GONE
            binding.tvAbsent.visibility = if (which == StateView.ABSENT) View.VISIBLE else View.GONE
        }

        private enum class StateView { SUBMITTED, ANSWER_TEXT, ANSWER_CHIPS, NOT_SUBMITTED, ABSENT }

        companion object {
            private const val FULL_ALPHA = 1.0f
            private const val DIM_ALPHA = 0.3f

            fun matchesHighlight(info: QuizAnsweringInfo, highlightedOptionId: Int?): Boolean {
                if (highlightedOptionId == null) return true
                return when (highlightedOptionId) {
                    SUBMITTED_OPTION_PSEUDO_ID -> info.answeringState == AnsweringState.ANSWERED
                    NOT_SUBMITTED_OPTION_PSEUDO_ID -> info.answeringState == AnsweringState.NOT_ANSWER
                    else -> true
                }
            }
        }
    }

    companion object {
        const val SUBMITTED_OPTION_PSEUDO_ID = -2
        const val NOT_SUBMITTED_OPTION_PSEUDO_ID = -1
        private const val CHIP_GAP_DP = 2.67f
        private const val PAYLOAD_SHOW_STUDENTS_NAME = "show_students_name"
        private const val PAYLOAD_IS_RESULT = "is_result"
        private const val PAYLOAD_HIGHLIGHT = "payload_highlight"

        private val DIFF = object : DiffUtil.ItemCallback<QuizAnsweringInfo>() {
            override fun areItemsTheSame(old: QuizAnsweringInfo, new: QuizAnsweringInfo) =
                old.studentId == new.studentId

            override fun areContentsTheSame(old: QuizAnsweringInfo, new: QuizAnsweringInfo) =
                old == new
        }
    }
}
