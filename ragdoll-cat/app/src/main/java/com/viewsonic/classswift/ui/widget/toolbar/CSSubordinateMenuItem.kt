package com.viewsonic.classswift.ui.widget.toolbar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ItemSubordinateMenuBinding

class CSSubordinateMenuItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: ItemSubordinateMenuBinding = ItemSubordinateMenuBinding.inflate(LayoutInflater.from(context), this, true)
    private var itemState: ItemState = ItemState.NORMAL

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CSSubordinateMenuItem,
            0,
            0
        ).apply {
            try {
                val iconSrcResId = getResourceId(R.styleable.CSSubordinateMenuItem_iconSrc, -1)
                val title = getString(R.styleable.CSSubordinateMenuItem_title) ?: ""
                binding.tvTitle.text = title
                if (iconSrcResId != -1) {
                    binding.ivIcon.setImageResource(iconSrcResId)
                    binding.ivIcon.imageTintList =
                        ContextCompat.getColorStateList(context, R.color.selector_toolbar_icon_button_tint)
                }
                updateUiByItemState(ItemState.entries[getInt(R.styleable.CSSubordinateMenuItem_itemState, 0)], true)
            } finally {
                recycle()
            }
        }
    }

    fun setItemState(state: ItemState) {
        updateUiByItemState(state)
    }

    private fun updateUiByItemState(newState: ItemState, isForced: Boolean = false) {
        if (itemState == newState && !isForced) {
            return
        }
        itemState = newState
        when (itemState) {
            ItemState.NORMAL -> {
                binding.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.color_2E3133))
                binding.tvSoon.isVisible = false
                binding.ivPremiumIcon.isVisible = false
                binding.ivIcon.isEnabled = true
                binding.ivIcon.isSelected = false
            }
            ItemState.SELECTED -> {
                binding.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.color_0A8CF0))
                binding.tvSoon.isVisible = false
                binding.ivPremiumIcon.isVisible = false
                binding.ivIcon.isEnabled = true
                binding.ivIcon.isSelected = true
            }
            ItemState.NEED_TO_UPGRADE -> {
                binding.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.color_C3C7C7))
                binding.tvSoon.isVisible = false
                binding.ivPremiumIcon.isVisible = true
                binding.ivIcon.isEnabled = false
                binding.ivIcon.isSelected = false
            }
            ItemState.COMING_SOON -> {
                binding.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.color_C3C7C7))
                binding.tvSoon.isVisible = true
                binding.ivPremiumIcon.isVisible = false
                binding.ivIcon.isEnabled = false
                binding.ivIcon.isSelected = false
            }
            ItemState.DISABLED -> {
                binding.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.color_C3C7C7))
                binding.tvSoon.isVisible = false
                binding.ivPremiumIcon.isVisible = false
                binding.ivIcon.isEnabled = false
                binding.ivIcon.isSelected = false
            }
        }
    }

    enum class ItemState {
        NORMAL,
        SELECTED,
        NEED_TO_UPGRADE,
        COMING_SOON,
        DISABLED
    }
}