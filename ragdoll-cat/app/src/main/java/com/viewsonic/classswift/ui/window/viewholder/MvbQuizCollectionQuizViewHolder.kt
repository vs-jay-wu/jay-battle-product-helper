package com.viewsonic.classswift.ui.window.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.databinding.ItemMvbQcQuizBinding
import com.viewsonic.classswift.ui.widget.quizcollection.mvb.MvbQuizTagChipView
import com.viewsonic.classswift.utils.extension.setDebouncedClickListener

class MvbQuizCollectionQuizViewHolder(
    private val binding: ItemMvbQcQuizBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        info: QuizInCollectionInfo,
        canUseStandards: Boolean,
        onClick: (QuizInCollectionInfo) -> Unit,
    ) {
        bindQuizType(info)
        bindResource(info)
        bindSubjectChip(info)
        bindStandardsChip(info, canUseStandards)
        binding.root.setDebouncedClickListener { onClick(info) }
    }

    private fun bindQuizType(info: QuizInCollectionInfo) {
        val context = binding.root.context
        val resId = quizTypeLabelResId(QuizType.safeValueOf(info.quizData.quizType))
        binding.tvMqcqQuizType.text = context.getString(resId)
    }

    private fun bindResource(info: QuizInCollectionInfo) {
        if (info.isTextQuiz()) {
            binding.ivMqcqThumbnail.visibility = View.GONE
            binding.tvMqcqTextContent.visibility = View.VISIBLE
            binding.tvMqcqTextContent.text = info.quizData.content
        } else {
            binding.tvMqcqTextContent.visibility = View.GONE
            binding.ivMqcqThumbnail.visibility = View.VISIBLE
            binding.ivMqcqThumbnail.load(info.quizData.imgUrl) {
                allowHardware(false)
                placeholder(R.drawable.bg_mvb_qc_quiz_thumbnail)
                error(R.drawable.bg_mvb_qc_quiz_thumbnail)
            }
        }
    }

    private fun bindSubjectChip(info: QuizInCollectionInfo) {
        val displayName = info.subjectDisplayName.ifBlank { info.quizData.subject }
        val context = binding.root.context
        binding.chipMqcqSubject.visibility = View.VISIBLE
        if (displayName.isBlank()) {
            binding.chipMqcqSubject.setVariant(MvbQuizTagChipView.Variant.SUBJECT_GENERAL)
            binding.chipMqcqSubject.setText(context.getString(R.string.mvb_qc_quiz_subject_general))
        } else {
            binding.chipMqcqSubject.setVariant(MvbQuizTagChipView.Variant.SUBJECT)
            binding.chipMqcqSubject.setText(displayName.toSubjectTitleCase())
        }
    }

    // API returns subject in lowercase snake_case (e.g. "social_studies"); Figma shows Title Case.
    private fun String.toSubjectTitleCase(): String =
        split("_", " ").filter { it.isNotEmpty() }.joinToString(" ") { word ->
            word.lowercase().replaceFirstChar(Char::titlecase)
        }

    private fun bindStandardsChip(info: QuizInCollectionInfo, canUseStandards: Boolean) {
        // Hide unless the user can use standards AND backend supplied a count.
        if (!canUseStandards || info.standardsCount <= 0) {
            binding.chipMqcqStandards.visibility = View.GONE
            return
        }
        val context = binding.root.context
        binding.chipMqcqStandards.visibility = View.VISIBLE
        binding.chipMqcqStandards.setVariant(MvbQuizTagChipView.Variant.STANDARDS)
        binding.chipMqcqStandards.setText(
            context.getString(R.string.mvb_qc_quiz_standards_count, info.standardsCount),
        )
    }

    companion object {

        private fun quizTypeLabelResId(type: QuizType): Int = when (type) {
            QuizType.SINGLE_SELECT,
            QuizType.MULTIPLE_SELECT,
            QuizType.UNSPECIFIED -> R.string.quiz_types_multiple_choice
            QuizType.TRUE_FALSE -> R.string.quiz_types_true_false
            QuizType.SHORT_ANSWER -> R.string.quiz_types_short_answer
            QuizType.SKETCH_RESPONSE -> R.string.quiz_types_sketch_response
            QuizType.RECORD -> R.string.quiz_types_audio
            QuizType.SINGLE_POLL,
            QuizType.MULTIPLE_POLL -> R.string.quiz_types_poll
        }
    }
}
