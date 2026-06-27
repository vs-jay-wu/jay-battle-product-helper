package com.viewsonic.classswift.ui.widget.task.content

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import com.viewsonic.classswift.databinding.ViewCreateTaskBinding
import com.viewsonic.classswift.databinding.ViewCreateTaskOptionsBinding
import com.viewsonic.classswift.ui.widget.task.enums.CreateTaskOption
import com.viewsonic.classswift.utils.extension.dpToPx

class CreateTaskView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var optionSelectedListener: ((CreateTaskOption) -> Unit)? = null
    private var popupWindow: PopupWindow? = null

    //Show the popup window aligned to the center of CreateTaskView, offset by 10dp downward
    private val offset = 10f.dpToPx().toInt()

    private val binding: ViewCreateTaskBinding = ViewCreateTaskBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    private val popupWindowBinding: ViewCreateTaskOptionsBinding = ViewCreateTaskOptionsBinding.inflate(
        LayoutInflater.from(context),
        null,
        false
    )

    init {
        initClickAction()
        initPopupWindow()
    }

    fun setOnOptionSelectedListener(listener: (CreateTaskOption) -> Unit) {
        optionSelectedListener = listener
    }

    private fun initPopupWindow() {
        popupWindow = PopupWindow(
            popupWindowBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupWindowBinding.let {
            it.tvSelectContent.setOnClickListener {
                popupWindow?.dismiss()
                optionSelectedListener?.invoke((CreateTaskOption.SELECT_CONTENT))
            }

            it.tvLink.setOnClickListener {
                popupWindow?.dismiss()
                optionSelectedListener?.invoke((CreateTaskOption.LINK))
            }
        }
    }

    private fun initClickAction() {
        binding.clRoot.setOnClickListener {
            showPopupOptionWindow()
        }
    }

    private fun showPopupOptionWindow() {
        val offsetX = width / 2 + offset
        val offsetY = -height / 2 + offset
        popupWindow?.showAsDropDown(this, offsetX, offsetY)
    }
}