package com.viewsonic.classswift.ui.widget.task.records

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.data.task.PopupOptionInfo
import com.viewsonic.classswift.databinding.ItemPopupOptionSelectorBinding

class PopupOptionItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var selectedListener: ((PopupOptionInfo) -> Unit)? = null
    private var optionData: PopupOptionInfo? = null

    private val binding: ItemPopupOptionSelectorBinding =
        ItemPopupOptionSelectorBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )

    init {
        initClickAction()
    }

    fun setOption(option: PopupOptionInfo) {
        optionData = option
        option.let {
            setTitle(title = it.title)
            updateSelectedStatus(isSelected = it.isSelected)
        }
    }

    fun setSelectedListener(listener: (PopupOptionInfo) -> Unit) {
        selectedListener = listener
    }

    fun setSelectedStatus(isSelected: Boolean) {
        optionData = optionData?.copy(isSelected = isSelected)
        updateSelectedStatus(isSelected = isSelected)
    }

    private fun setTitle(title: String) {
        binding.tvTitle.text = title
    }

    private fun updateSelectedStatus(isSelected: Boolean) {
        context ?: return

        with(binding) {
            acivSelected.isVisible = isSelected

            val textColorResId = if (isSelected) {
                R.color.records_sort_option_selected_text_color
            } else {
                R.color.records_sort_option_normal_text_color
            }

            tvTitle.setTextColor(
                ContextCompat.getColor(context, textColorResId)
            )
        }
    }

    private fun initClickAction() {
        binding.clSortNumberOption.setOnClickListener {
            optionData?.let {
                selectedListener?.invoke(it)
            }
        }
    }
}