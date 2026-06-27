package com.viewsonic.classswift.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R

@SuppressLint("AppCompatCustomView")
class CSUserInfoCheckBox @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Button(context, attrs, defStyleAttr) {

    private var isChecked = false

    private var externalClickListener: OnClickListener? = null

    init {
        setOnClickListener {}
        updateBackground()
    }

    override fun setOnClickListener(l: OnClickListener?) {
        // 儲存外部的 Listener
        externalClickListener = l
        super.setOnClickListener {
            // 執行自定義邏輯
            toggle()
            // 執行外部的 Listener
            externalClickListener?.onClick(it)
        }
    }

    private fun toggle() {
        isChecked = !isChecked
        updateBackground()
    }

    private fun updateBackground() {
        isSelected = isChecked
        background = if (isChecked) {
            ContextCompat.getDrawable(context, R.drawable.ic_checked_blue)
        } else {
            ContextCompat.getDrawable(context, R.drawable.ic_uncheck_gray)
        }
    }

    fun setChecked(checked: Boolean) {
        isChecked = checked
        updateBackground()
    }

    fun isChecked(): Boolean = isChecked
}