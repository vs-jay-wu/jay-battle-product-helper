package com.viewsonic.classswift.ui.widget.task.records

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isEmpty
import com.viewsonic.classswift.data.task.PopupOptionInfo
import com.viewsonic.classswift.databinding.ViewPopupOptionSelectorBinding

class PopupOptionSelectorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var popupWindow: PopupWindow? = null
    private val binding: ViewPopupOptionSelectorBinding =
        ViewPopupOptionSelectorBinding.inflate(
            LayoutInflater.from(context),
            null,
            false
        )

    private var selectedListener: ((PopupOptionInfo) -> Unit)? = null
    private var popupWidth: Int = 0
    private var parentWidth: Int = 0
    private var offsetX: Int = 0
    private var offsetY: Int = 0

    init {
        initPopupWindow()
    }

    fun setOptions(options: List<PopupOptionInfo>) {
        context ?: return

        options.forEach { option ->
            val optionItemView = PopupOptionItemView(context = context).apply {
                setOption(option = option)
                setSelectedListener { item ->
                    handleSelectedUpdate(option = item)
                    selectedListener?.invoke(item)
                    popupWindow?.dismiss()
                }
            }
            binding.llOptionContainer.addView(optionItemView)
        }
    }

    fun setSelectedListener(listener: (PopupOptionInfo) -> Unit) {
        selectedListener = listener
    }

    fun show(offsetX: Int, offsetY: Int) {
        popupWindow?.showAsDropDown(this, offsetX, offsetY)
    }

    fun show(offsetY: Int = 0) {
        binding.llOptionContainer.measure(
            MeasureSpec.UNSPECIFIED,
            MeasureSpec.UNSPECIFIED
        )

        popupWidth = binding.llOptionContainer.measuredWidth
        parentWidth = this.width
        offsetX = parentWidth - popupWidth
        popupWindow?.showAsDropDown(this, offsetX, offsetY)
    }

    private fun initPopupWindow() {
        popupWindow = PopupWindow(
            binding.llOptionContainer,
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            true
        )

        popupWidth = popupWindow?.contentView?.measuredWidth ?: popupWindow?.width ?: 0
        parentWidth = this.width
        offsetX = parentWidth - popupWidth
        offsetY = 0
    }

    private fun handleSelectedUpdate(option: PopupOptionInfo) {
        if (binding.llOptionContainer.isEmpty()) return

        val selectedIndex = option.index

        for (i in 0 until binding.llOptionContainer.childCount) {
            val itemView = binding.llOptionContainer.getChildAt(i)
            if (itemView is PopupOptionItemView) {
                itemView.setSelectedStatus(i == selectedIndex)
            }
        }
    }
}