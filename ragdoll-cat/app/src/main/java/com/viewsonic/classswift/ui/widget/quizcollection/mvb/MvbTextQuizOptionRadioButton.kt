package com.viewsonic.classswift.ui.widget.quizcollection.mvb

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Checkable
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R

class MvbTextQuizOptionRadioButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr), Checkable {

    private var checked: Boolean = false

    init {
        isClickable = true
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MvbTextQuizOptionRadioButton,
            defStyleAttr,
            0,
        ).apply {
            try {
                checked = getBoolean(
                    R.styleable.MvbTextQuizOptionRadioButton_android_checked,
                    false,
                )
                isSelected = checked
            } finally {
                recycle()
            }
        }
        updateBackground()
    }

    override fun isChecked(): Boolean = checked

    override fun setChecked(checked: Boolean) {
        if (this.checked == checked) return
        this.checked = checked
        isSelected = checked
        refreshDrawableState()
        updateBackground()
    }

    override fun toggle() {
        isChecked = !checked
    }

    private fun updateBackground() {
        background = ContextCompat.getDrawable(
            context,
            if (checked) {
                R.drawable.bg_mvb_text_quiz_option_radio_checked
            } else {
                R.drawable.bg_mvb_text_quiz_option_radio_unchecked
            },
        )
    }
}
