package com.viewsonic.classswift.ui.widget.task.enums

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewTaskTabBinding

class TaskTabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewTaskTabBinding = ViewTaskTabBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT)
    }

    fun setTitle(title: String) {
        binding.tvTabText.text = title
    }

    fun setSelectedStatus(isSelected: Boolean) {
        with(binding) {
            indicator.isVisible = isSelected
            tvTabText.setTextColor(getTextColor(isSelected = isSelected))
        }
    }

    private fun getTextColor(isSelected: Boolean): Int =
        ContextCompat.getColor(
            context,
            if (isSelected) R.color.push_and_respond_tab_selected_text_color
            else R.color.push_and_respond_tab_text_color
        )
}