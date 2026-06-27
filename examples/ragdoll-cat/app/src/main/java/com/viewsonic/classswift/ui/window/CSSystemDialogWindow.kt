package com.viewsonic.classswift.ui.window

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.WindowSystemDialogBinding
import com.viewsonic.classswift.utils.DisplayUtils
import com.viewsonic.classswift.utils.extension.isActuallyVisible
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CSSystemDialogWindow(context: Context, override var size: SizeInPixels, override var tag: WindowTag = WindowTag.CS_NORMAL_DIALOG) :
    IWindow<WindowSystemDialogBinding>, KoinComponent {

    override val binding: WindowSystemDialogBinding =
        WindowSystemDialogBinding.inflate(
            LayoutInflater.from(
                ContextThemeWrapper(
                    context,
                    com.google.android.material.R.style.Theme_MaterialComponents
                )
            )
        )

    private val csWindowManager: CSWindowManager by inject()

    private val disableTextColor = context.getColor(R.color.cs_system_dialog_disable_text_color)
    private var enableTextColor = context.getColor(R.color.neutral_900)

    private var isNetworkDisconnect = false

    var isPositiveButtonLoading = false
        private set
    var isNegativeButtonLoading = false
        private set

    fun setEnableTextColor(@ColorInt color: Int) {
        enableTextColor = color
    }

    fun startPositiveButtonLoading() {
        with(binding) {
            btPositive.isVisible = false
            cpiPositive.isVisible = true
        }
        isPositiveButtonLoading = true
    }

    fun startNegativeButtonLoading() {
        with(binding) {
            btNegative.isVisible = false
            cpiNegative.isVisible = true
        }
        isNegativeButtonLoading = true
    }

    fun stopPositiveButtonLoading() {
        with(binding) {
            btPositive.isVisible = true
            cpiPositive.isVisible = false
        }
        isPositiveButtonLoading = false
    }

    fun stopNegativeButtonLoading() {
        with(binding) {
            btNegative.isVisible = true
            cpiNegative.isVisible = false
        }
        isNegativeButtonLoading = false
    }

    fun setEnable(flag: Boolean) {
        with(binding) {
            btPositive.isVisible = true
            val color = if (flag) enableTextColor else disableTextColor
            btPositive.setTextColor(color)
            btPositive.isEnabled = flag
            cpiPositive.isVisible = false
            isPositiveButtonLoading = false
            cpiNegative.isVisible = false
            isNegativeButtonLoading = false
        }
    }

    fun isShowing(): Boolean {
        return binding.root.isActuallyVisible()
    }

    fun setNegativeButtonEnable(flag: Boolean) {
        with(binding) {
            val color = if (flag) enableTextColor else disableTextColor
            btNegative.setTextColor(color)
            btNegative.isEnabled = flag
            cpiNegative.isVisible = false
            isNegativeButtonLoading = false
        }
    }

    fun setOneButtonLayout() {
        val params = LayoutParams(0, 0)
        params.topToBottom = binding.viewBottomDividerLine.id
        params.startToStart = LayoutParams.PARENT_ID
        params.endToEnd = LayoutParams.PARENT_ID
        params.bottomToBottom = LayoutParams.PARENT_ID
        with(binding) {
            btPositive.layoutParams = params
            btNegative.isVisible = false
            cpiNegative.isVisible = false
            viewButtonDividerLine.isVisible = false
            cpiPositive.isVisible = false
        }
    }

    fun show() {
        csWindowManager.createWindow(this, Gravity.CENTER, isOutOfScreen = false)
    }

    fun dismiss() {
        csWindowManager.removeWindow(tag)
    }

    // only system dialog need to set
    fun setIsNetworkDisconnect(value: Boolean) {
        isNetworkDisconnect = value
    }

    // only system dialog need to call
    fun isNetworkDisconnect(): Boolean {
        return isNetworkDisconnect
    }

    // check dialog is system dialog or not
    fun isSystemDialog(): Boolean {
        return tag == WindowTag.CS_SYSTEM_DIALOG || tag == WindowTag.FORCE_LOGOUT_DIALOG
    }


    class Builder(private val context: Context, private var tag: WindowTag = WindowTag.CS_NORMAL_DIALOG) {
        private var title = ""
        private var message: CharSequence = ""
        private var negativeButtonString: String? = null
        private var positiveButtonString = ""

        @ColorInt
        private var negativeButtonTextColor = context.getColor(R.color.permission_positive_color)

        @ColorInt
        private var positiveButtonTextColor = context.getColor(R.color.brand_blue)
        private var positiveButtonClickListener: (() -> Unit)? = null
        private var negativeButtonClickListener: (() -> Unit)? = null

        fun setTitle(title: String) = apply { this.title = title }

        fun setMessage(message: CharSequence) = apply { this.message = message }

        fun setPositiveButton(text: String, @ColorInt color: Int, listener: () -> Unit) = apply {
            this.positiveButtonString = text
            this.positiveButtonTextColor = color
            this.positiveButtonClickListener = listener
        }

        fun setNegativeButton(text: String, @ColorInt color: Int, listener: () -> Unit) = apply {
            this.negativeButtonString = text
            this.negativeButtonTextColor = color
            this.negativeButtonClickListener = listener
        }

        fun build(): CSSystemDialogWindow {
            val screenWidth = DisplayUtils.getScreenSize().first
            val screenHeight = DisplayUtils.getScreenSize().second
            val window =
                CSSystemDialogWindow(context, SizeInPixels(screenWidth, screenHeight), tag)
            window.binding.tvTitle.text = title
            window.binding.tvMessage.text = message
            window.binding.btPositive.text = positiveButtonString
            window.binding.btPositive.setTextColor(this.positiveButtonTextColor)
            window.setEnableTextColor(this.positiveButtonTextColor)
            window.binding.cpiPositive.setIndicatorColor(this.positiveButtonTextColor)
            window.binding.btPositive.setOnClickListener {
                if (!window.isPositiveButtonLoading) positiveButtonClickListener?.invoke()
            }
            this.negativeButtonString?.let {
                window.binding.btNegative.text = it
                window.binding.btNegative.setTextColor(this.negativeButtonTextColor)
                window.binding.cpiNegative.setIndicatorColor(this.negativeButtonTextColor)
                window.binding.btNegative.setOnClickListener {
                    if (!window.isNegativeButtonLoading) negativeButtonClickListener?.invoke()
                }
            } ?: run {
                window.setOneButtonLayout()
            }
            return window
        }
    }
}