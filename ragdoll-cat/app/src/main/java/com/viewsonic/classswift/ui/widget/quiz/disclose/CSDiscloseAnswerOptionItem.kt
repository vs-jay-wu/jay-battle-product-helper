package com.viewsonic.classswift.ui.widget.quiz.disclose

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.WidgetDiscloseAnswerOptionItemBinding

class CSDiscloseAnswerOptionItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: WidgetDiscloseAnswerOptionItemBinding

    var optionId: Int = -1
        private set
    private var selectionMode: SelectionMode = SelectionMode.SINGLE
    private var isOptionChecked: Boolean = false

    init {
        binding = WidgetDiscloseAnswerOptionItemBinding.inflate(LayoutInflater.from(context), this)
        background = ContextCompat.getDrawable(context, R.drawable.bg_neutral100_radius800_line_neutral300_border400)
    }

    fun bind(data: DiscloseOptionItemData, mode: SelectionMode, checked: Boolean) {
        optionId = data.id
        binding.tvDiscloseOptionLabel.text = data.label
        selectionMode = mode
        setChecked(checked)
    }

    fun setChecked(checked: Boolean) {
        isOptionChecked = checked
        background = ContextCompat.getDrawable(
            context,
            if (checked) R.drawable.bg_disclose_answer_option_selected
            else R.drawable.bg_neutral100_radius800_line_neutral300_border400
        )
        binding.vDiscloseOptionSelector.setBackgroundResource(resolveSelectorDrawable(checked))
    }

    private fun resolveSelectorDrawable(checked: Boolean): Int = when (selectionMode) {
        SelectionMode.SINGLE ->
            if (checked) R.drawable.bg_disclose_radio_checked
            else R.drawable.bg_disclose_radio_unchecked
        SelectionMode.MULTIPLE ->
            if (checked) R.drawable.bg_disclose_checkbox_checked
            else R.drawable.bg_disclose_checkbox_unchecked
    }
}
