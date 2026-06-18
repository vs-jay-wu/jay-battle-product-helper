package com.viewsonic.classswift.ui.widget.toolbar

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewToolbarActionButtonBinding
import com.viewsonic.classswift.utils.extension.dpToPx

class CSToolbarActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {
    private var binding: ViewToolbarActionButtonBinding = ViewToolbarActionButtonBinding.inflate(LayoutInflater.from(context), this)
    private var itemState: ItemState = ItemState.ACTIVE

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CSToolbarActionButton,
            0,
            0
        ).apply {
            try {
                // Default Setting
                strokeWidth = 1.2f.dpToPx().toInt()
                radius = 4.8f.dpToPx()

                val title = getString(R.styleable.CSToolbarActionButton_cstabTitle) ?: ""
                val iconSrcResId = getResourceId(R.styleable.CSToolbarActionButton_cstabIconSrc, R.drawable.ic_toolbar_end_lesson)
                val backgroundColor = getColor(R.styleable.CSToolbarActionButton_cstabBackgroundColor, ContextCompat.getColor(context, R.color.white))
                val borderColor = getColor(R.styleable.CSToolbarActionButton_cstabBorderColor, ContextCompat.getColor(context, R.color.color_F02B2B))
                val textColor = getColor(R.styleable.CSToolbarActionButton_cstabTextColor, ContextCompat.getColor(context, R.color.color_F02B2B))
                val iconTintColor = getColor(R.styleable.CSToolbarActionButton_cstabIconTintColor, ContextCompat.getColor(context, R.color.color_F02B2B))
                val pressedBackgroundColor = getColor(R.styleable.CSToolbarActionButton_cstabPressedBackgroundColor, ContextCompat.getColor(context, R.color.color_F02B2B))
                val pressedBorderColor = getColor(R.styleable.CSToolbarActionButton_cstabPressedBorderColor, ContextCompat.getColor(context, R.color.color_F02B2B))
                val pressedTextColor = getColor(R.styleable.CSToolbarActionButton_cstabPressedTextColor, ContextCompat.getColor(context, R.color.white))
                val pressedIconTintColor = getColor(R.styleable.CSToolbarActionButton_cstabPressedIconTintColor, ContextCompat.getColor(context, R.color.white))
                val loadingProgressColor = getColor(R.styleable.CSToolbarActionButton_cstabLoadingProgressColor, ContextCompat.getColor(context, R.color.white))
                val loadingBorderColor = getColor(R.styleable.CSToolbarActionButton_cstabLoadingBorderColor, ContextCompat.getColor(context, R.color.color_F02B2B))
                val loadingBackgroundColor = getColor(R.styleable.CSToolbarActionButton_cstabLoadingBackgroundColor, ContextCompat.getColor(context, R.color.color_F02B2B))
                updateUiByItemState(ItemState.entries[getInt(R.styleable.CSToolbarActionButton_cstabItemState, 0)], true)

                val states = arrayOf(
                    intArrayOf(-android.R.attr.state_enabled), // use disabled as Loading State
                    intArrayOf(android.R.attr.state_pressed), // pressed
                    intArrayOf(android.R.attr.state_enabled), // enabled
                )
                val textColorList = intArrayOf(
                    ContextCompat.getColor(context, R.color.transparent),
                    pressedTextColor,
                    textColor
                )

                binding.tvTitle.text = title
                binding.tvTitle.setTextColor(
                    ColorStateList(states, textColorList)
                )

                val iconColorList = intArrayOf(
                    ContextCompat.getColor(context, R.color.transparent),
                    pressedIconTintColor,
                    iconTintColor
                )
                binding.ivIcon.setImageResource(iconSrcResId)
                binding.ivIcon.imageTintList = ColorStateList(states, iconColorList)

                val backgroundColorList = intArrayOf(
                    loadingBackgroundColor,
                    pressedBackgroundColor,
                    backgroundColor
                )
                setCardBackgroundColor(ColorStateList(states, backgroundColorList))

                val strokeColorList = intArrayOf(
                    loadingBorderColor,
                    pressedBorderColor,
                    borderColor
                )
                setStrokeColor(ColorStateList(states, strokeColorList))
                binding.cpiProgress.setIndicatorColor(loadingProgressColor)
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
            ItemState.ACTIVE -> {
                isEnabled = true
                isPressed = false
                binding.ivIcon.visibility = View.VISIBLE
                binding.tvTitle.visibility = View.VISIBLE
                binding.cpiProgress.isVisible = false
                isClickable = true
            }
            ItemState.LOADING -> {
                isEnabled = false
                isPressed = false
                binding.ivIcon.visibility = View.INVISIBLE
                binding.tvTitle.visibility = View.INVISIBLE
                binding.cpiProgress.isVisible = true
                isClickable = false
            }
        }
    }

    enum class ItemState {
        ACTIVE,
        LOADING
    }
}