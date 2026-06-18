package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import coil.load
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.databinding.WidgetMvbQcDetailBaseBinding
import com.viewsonic.classswift.ui.widget.OnLoadingButtonStateListener
import com.viewsonic.classswift.utils.extension.dpToPx
import com.viewsonic.classswift.utils.extension.setDebouncedClickListener

/**
 * Base detail view for the Mvb Quiz Collection detail page (VSFT-7268).
 *
 * Layout structure: Back button (top) + Question card (chip header + preview + options) + Start button (bottom).
 *
 * Subclasses override [bindOptions] (and optionally [bindPreview]) to inject variant-specific content.
 * The default [bindPreview] shows the question's image; the default [bindOptions] hides the options section
 * (used by SA / Audio variants which have no answer options).
 */
open class MvbCollectionQuizDetailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    protected val binding: WidgetMvbQcDetailBaseBinding =
        WidgetMvbQcDetailBaseBinding.inflate(
            // Use Material theme so `?selectableItemBackground` resolves on the Back button.
            LayoutInflater.from(
                ContextThemeWrapper(context, com.google.android.material.R.style.Theme_MaterialComponents),
            ),
            this,
            true,
        )

    fun bind(info: QuizInCollectionInfo) {
        bindChip(info)
        bindPreview(info)
        bindOptions(info)
    }

    fun setOnBackClicked(onClick: () -> Unit) {
        binding.llMqcdBack.setDebouncedClickListener { onClick() }
    }

    fun setOnStartClicked(onClick: () -> Unit) {
        binding.cslbMqcdStart.setOnCustomClickListener(object : OnLoadingButtonStateListener {
            override fun onEnableClicked() = onClick()
        })
    }

    fun setStartButtonLoading() {
        binding.cslbMqcdStart.setLoading()
    }

    fun setStartButtonEnabled(enabled: Boolean) {
        if (enabled) binding.cslbMqcdStart.setEnable() else binding.cslbMqcdStart.setDisable()
    }

    protected val previewContainer: FrameLayout get() = binding.flMqcdPreview
    protected val optionsSection: View get() = binding.llMqcdOptionsSection
    protected val optionsContent: FrameLayout get() = binding.flMqcdOptionsContent

    private fun bindChip(info: QuizInCollectionInfo) {
        val quizType = QuizType.safeValueOf(info.quizData.quizType)
        val iconRes = resolveDetailChipDrawable(quizType)
        if (iconRes != null) {
            binding.ivMqcdChip.setImageResource(iconRes)
            binding.ivMqcdChip.visibility = View.VISIBLE
        } else {
            binding.ivMqcdChip.visibility = View.GONE
        }
        binding.tvMqcdChipLabel.text =
            context.getString(resolveDetailChipText(quizType, info.isTextQuiz()))
        val badgeRes = resolveDetailChipBadgeText(quizType)
        if (badgeRes == null) {
            binding.tvMqcdChipBadge.visibility = View.GONE
        } else {
            binding.tvMqcdChipBadge.text = context.getString(badgeRes)
            binding.tvMqcdChipBadge.visibility = View.VISIBLE
        }
    }

    /**
     * Default preview: dispatches between text-only and image-based question rendering.
     *
     * NOTE: VSFT-7268 AC only defines image-based detail page styling. Text-quiz detail UI is
     * deferred to the next story — current text rendering is an interim plain-text fallback so the
     * flow is functional. When the proper Mvb text-quiz detail design ships, [renderTextPreview] (or
     * variant overrides) should be replaced with the designed component.
     */
    protected open fun bindPreview(info: QuizInCollectionInfo) {
        previewContainer.removeAllViews()
        if (info.isTextQuiz()) {
            renderTextPreview(info)
        } else {
            renderImagePreview(info)
        }
    }

    protected fun renderImagePreview(info: QuizInCollectionInfo) {
        val imageView = ImageView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }
        if (info.quizData.imgUrl.isNotBlank()) {
            imageView.load(info.quizData.imgUrl) {
                allowHardware(false)
            }
        }
        previewContainer.addView(imageView)
    }

    @Deprecated(
        message = "Use renderTextInsetCard for proper Figma-aligned text preview (mvb-text-quiz-detail). " +
            "This was a VSFT-7268 transition implementation — plain text on gray bg, no inset card.",
        replaceWith = ReplaceWith("renderTextInsetCard(info)"),
    )
    protected fun renderTextPreview(info: QuizInCollectionInfo) {
        val padding = TEXT_PREVIEW_PADDING_DP.dpToPx().toInt()
        val textView = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            setPadding(padding, padding, padding, padding)
            gravity = Gravity.TOP or Gravity.START
            text = info.quizData.content
            setTextColor(context.getColor(R.color.neutral_900))
            textSize = TEXT_PREVIEW_SIZE_SP
            includeFontPadding = false
        }
        previewContainer.addView(textView)
    }

    /**
     * Renders the text question stem inside a white inset card (border + radius + padding).
     * Used by `MvbCollection*TextQuizDetailView` subclasses (mvb-text-quiz-detail).
     *
     * Figma (per node `3333-40325` / `3333-39414` / `3754-184747`):
     *  - card 736×417 fill #FFFFFF / stroke #E5E5E5 1px / radius 12 / pad 24
     *  - text Inter w500 16px lh=25.6 color #333333 (Inter w500 ~10.67sp / lineSpacingMultiplier 1.6)
     *
     * @param contentGravity gravity for the text inside the inset card. Defaults to TOP|START
     *   matching TF / MC / SA layouts (verified against Figma screenshots).
     * @param cardWidthPx fixed pixel width for the card (use [LayoutParams.MATCH_PARENT] to fill).
     *   SA-text passes a fixed width (491dp Figma) + [cardLayoutGravity]=CENTER_HORIZONTAL so the
     *   card visually centers within the body when options section is GONE.
     * @param cardLayoutGravity Gravity flags for the card within its parent FrameLayout.
     */
    protected fun renderTextInsetCard(
        info: QuizInCollectionInfo,
        contentGravity: Int = Gravity.TOP or Gravity.START,
        cardWidthPx: Int = LayoutParams.MATCH_PARENT,
        cardLayoutGravity: Int = Gravity.NO_GRAVITY,
    ) {
        val card = FrameLayout(context).apply {
            layoutParams = LayoutParams(cardWidthPx, LayoutParams.MATCH_PARENT).apply {
                gravity = cardLayoutGravity
            }
            background = ContextCompat.getDrawable(
                context,
                R.drawable.bg_neutral0_radius600_line_neutral300_border200,
            )
        }
        val padding = resources.getDimensionPixelSize(R.dimen.mvb_spacing_600)
        val scrollView = ScrollView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            setPadding(padding, padding, padding, padding)
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val textView = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )
            gravity = contentGravity
            text = info.quizData.content
            setTextColor(context.getColor(R.color.neutral_900))
            textSize = TEXT_PREVIEW_SIZE_SP
            setLineSpacing(0f, TEXT_LINE_SPACING_MULT)
            includeFontPadding = false
        }
        scrollView.addView(textView)
        card.addView(scrollView)
        previewContainer.addView(card)
    }

    /**
     * Renders a vertical scrollable list of text rows in the options content area.
     * Used by `MvbCollectionTrueFalseTextQuizDetailView` (T/F) and `MvbCollectionMultipleChoiceTextQuizDetailView` ((A) text...).
     *
     * Figma (per node `3333-39414`): each row is `quiz answer option_text` 399×110 — fill #F6F6F6 /
     * stroke #E5E5E5 1px / radius 16 / pad 16. Rows stack vertically with gap 16px → mvb_spacing_400.
     */
    protected fun renderTextOptionList(rowLabels: List<String>) {
        optionsContent.removeAllViews()
        val scrollView = ScrollView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        val container = LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )
            orientation = LinearLayout.VERTICAL
        }
        val rowPadding = resources.getDimensionPixelSize(R.dimen.mvb_spacing_400)
        val rowGap = resources.getDimensionPixelSize(R.dimen.mvb_spacing_400)
        rowLabels.forEachIndexed { index, label ->
            val row = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { if (index > 0) topMargin = rowGap }
                background = ContextCompat.getDrawable(
                    context,
                    R.drawable.bg_neutral100_radius800_line_neutral300_border200,
                )
                setPadding(rowPadding, rowPadding, rowPadding, rowPadding)
                text = label
                setTextColor(context.getColor(R.color.neutral_900))
                textSize = OPTION_ROW_TEXT_SIZE_SP
                setLineSpacing(0f, TEXT_LINE_SPACING_MULT)
                includeFontPadding = false
            }
            container.addView(row)
        }
        scrollView.addView(container)
        optionsContent.addView(scrollView)
    }

    /**
     * Default options: section invisible but keeps layout space. Per Figma, even SA / Audio
     * variants reserve the right-column width so the preview stays at ~60% of the card width
     * (matches the MC/Poll/TF visual proportion).
     */
    protected open fun bindOptions(info: QuizInCollectionInfo) {
        optionsSection.visibility = View.INVISIBLE
    }

    /**
     * Renders an A-F label grid into the options content. Shared between MC + Poll variants.
     * Caller is expected to have already shown [optionsSection]; this clears + repopulates the inner content.
     */
    protected fun renderOptionGrid(info: QuizInCollectionInfo, columns: Int = GRID_DEFAULT_COLUMNS) {
        optionsSection.visibility = View.VISIBLE
        optionsContent.removeAllViews()
        val grid = GridLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )
            columnCount = columns
        }
        val gap = OPTION_GAP_DP.dpToPx().toInt()
        val cardSize = OPTION_SIZE_DP.dpToPx().toInt()
        val optionCount = info.quizData.optionList.size.coerceIn(MIN_OPTIONS, MAX_OPTIONS)
        repeat(optionCount) { index ->
            val label = ('A' + index).toString()
            val card = createOptionCard(label)
            val params = GridLayout.LayoutParams().apply {
                width = cardSize
                height = cardSize
                rowSpec = GridLayout.spec(index / columns)
                columnSpec = GridLayout.spec(index % columns)
                if (index % columns != 0) leftMargin = gap
                if (index >= columns) topMargin = gap
            }
            grid.addView(card, params)
        }
        optionsContent.addView(grid)
    }

    /**
     * Builds a single option card (T/F or A-F label) used by TF / MC / Poll variants.
     * Shared here so the three variants don't duplicate the styling code (S4144).
     */
    protected fun createOptionCard(label: String): TextView = TextView(context).apply {
        background = context.getDrawable(R.drawable.bg_mvb_qc_detail_option_card)
        gravity = Gravity.CENTER
        text = label
        setTextColor(context.getColor(R.color.neutral_900))
        textSize = OPTION_TEXT_SIZE_SP
        includeFontPadding = false
        setTypeface(null, Typeface.BOLD)
    }

    companion object {
        private const val TEXT_PREVIEW_PADDING_DP = 24f
        private const val TEXT_PREVIEW_SIZE_SP = 10.67f
        private const val OPTION_ROW_TEXT_SIZE_SP = 10.67f

        // Figma 16px font / 25.6 line-height → multiplier 1.6 (TF/MC/SA text variants).
        private const val TEXT_LINE_SPACING_MULT = 1.6f

        const val OPTION_SIZE_DP = 67.33f
        const val OPTION_GAP_DP = 10.66f
        const val OPTION_TEXT_SIZE_SP = 17.78f

        private const val GRID_DEFAULT_COLUMNS = 3
        private const val MIN_OPTIONS = 2
        private const val MAX_OPTIONS = 6

        /**
         * Detail chip primary label — re-uses the existing localized `quiz_types_*` strings so the
         * label stays consistent with the quiz card (`MvbQuizCollectionQuizViewHolder.quizTypeLabelResId`)
         * across zh-tw / en locales (T44 fix). MC/Poll single/multi distinction is rendered as a
         * separate badge in Figma — handled by [resolveDetailChipBadgeText] (TODO T38).
         */
        @StringRes
        fun resolveDetailChipText(quizType: QuizType, @Suppress("UNUSED_PARAMETER") isTextQuiz: Boolean): Int =
            when (quizType) {
                QuizType.TRUE_FALSE -> R.string.quiz_types_true_false
                QuizType.SINGLE_SELECT,
                QuizType.MULTIPLE_SELECT -> R.string.quiz_types_multiple_choice
                QuizType.SINGLE_POLL,
                QuizType.MULTIPLE_POLL -> R.string.quiz_types_poll
                QuizType.RECORD -> R.string.quiz_types_audio
                QuizType.SHORT_ANSWER,
                QuizType.UNSPECIFIED -> R.string.quiz_types_short_answer
                QuizType.SKETCH_RESPONSE -> R.string.quiz_types_sketch_response
            }

        /**
         * Single/Multiple answer pill badge label, shown next to the chip text in the header.
         * Figma `3333-39416` shows "Single answer" / "Multiple answer" pills for MC; Poll variants
         * use the existing localized "Single vote" / "Multiple votes" strings. Returns null for
         * variants that don't display the badge (TF / SA / Audio).
         */
        @StringRes
        fun resolveDetailChipBadgeText(quizType: QuizType): Int? = when (quizType) {
            QuizType.SINGLE_SELECT -> R.string.quiz_types_multiple_choice_single_answer
            QuizType.MULTIPLE_SELECT -> R.string.quiz_types_multiple_choice_multiple_answers
            QuizType.SINGLE_POLL -> R.string.quiz_types_poll_single_vote
            QuizType.MULTIPLE_POLL -> R.string.quiz_types_poll_multiple_votes
            QuizType.TRUE_FALSE,
            QuizType.RECORD,
            QuizType.SHORT_ANSWER,
            QuizType.SKETCH_RESPONSE,
            QuizType.UNSPECIFIED -> null
        }

        /**
         * Per-type combined chip drawable (Figma 'tools 48*48' export — blue square + white
         * border + per-type icon, baked into a single vector).
         */
        @androidx.annotation.DrawableRes
        fun resolveDetailChipDrawable(quizType: QuizType): Int? = when (quizType) {
            QuizType.TRUE_FALSE -> R.drawable.ic_mvb_qc_chip_true_false
            QuizType.SINGLE_SELECT,
            QuizType.MULTIPLE_SELECT -> R.drawable.ic_mvb_qc_chip_multiple_choice
            QuizType.SINGLE_POLL,
            QuizType.MULTIPLE_POLL -> R.drawable.ic_mvb_qc_chip_poll
            QuizType.SHORT_ANSWER -> R.drawable.ic_mvb_qc_chip_short_answer
            QuizType.RECORD -> R.drawable.ic_mvb_qc_chip_audio
            QuizType.SKETCH_RESPONSE,
            QuizType.UNSPECIFIED -> null
        }
    }
}
