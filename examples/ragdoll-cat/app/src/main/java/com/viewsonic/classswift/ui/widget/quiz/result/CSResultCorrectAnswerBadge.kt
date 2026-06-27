package com.viewsonic.classswift.ui.widget.quiz.result

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R

/**
 * Circle badge used in Result Overview to display a single correct-answer letter/number
 * (e.g. "T", "F", "A", "1"). Matches Figma 3215-64370 / 3215-22094 / 6668-173835 styling.
 *
 * Multi-select renders multiple of these side-by-side.
 */
class CSResultCorrectAnswerBadge @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        setBackgroundResource(R.drawable.bg_result_correct_answer_badge)
        gravity = Gravity.CENTER
        setTextColor(ContextCompat.getColor(context, R.color.neutral_900))
        textSize = DEFAULT_TEXT_SP
        typeface = Typeface.DEFAULT_BOLD
    }

    fun setLabel(label: String) {
        text = label
    }

    companion object {
        private const val DEFAULT_TEXT_SP = 24f
    }
}
