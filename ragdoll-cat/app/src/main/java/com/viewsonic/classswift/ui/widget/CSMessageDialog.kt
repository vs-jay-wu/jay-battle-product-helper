package com.viewsonic.classswift.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.WidgetCsMessageDialogBinding
import org.koin.core.component.KoinComponent
import androidx.core.content.withStyledAttributes

class CSMessageDialog @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), KoinComponent {

    private var onPositiveClick: (() -> Unit)? = null
    private var onNegativeClick: (() -> Unit)? = null
    private var onMaskClick: (() -> Unit)? = null

    private var posDisableTextColor = context.getColor(R.color.cs_message_dialog_default_disable_text_color)
    private var posEnableTextColor = context.getColor(R.color.cs_message_dialog_positive_default_text_color)
    private var negDisableTextColor = context.getColor(R.color.cs_message_dialog_default_disable_text_color)
    private var negEnableTextColor = context.getColor(R.color.cs_message_dialog_negative_default_text_color)

    private val binding: WidgetCsMessageDialogBinding = WidgetCsMessageDialogBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    init {

        //Block clicks on the semi-transparent background
        setOnClickListener { onMaskClick?.invoke() }

        context.withStyledAttributes(attrs, R.styleable.CSMessageDialog) {
            val positiveColor = getColor(
                R.styleable.CSMessageDialog_positiveTextColor,
                posEnableTextColor
            )
            val negativeColor = getColor(
                R.styleable.CSMessageDialog_negativeTextColor,
                negEnableTextColor
            )

            val positiveDisableColor = getColor(
                R.styleable.CSMessageDialog_positiveDisableTextColor,
                posDisableTextColor
            )
            val negativeDisableColor = getColor(
                R.styleable.CSMessageDialog_negativeDisableTextColor,
                negDisableTextColor
            )

            val backgroundRes = getResourceId(
                R.styleable.CSMessageDialog_csmdMaskBackground,
                R.drawable.bg_mask_radius800
            )

            negEnableTextColor = negativeColor
            posEnableTextColor = positiveColor
            posDisableTextColor = positiveDisableColor
            negDisableTextColor = negativeDisableColor
            binding.acbPositive.setTextColor(posEnableTextColor)
            binding.btNegative.setTextColor(negEnableTextColor)
            setBackgroundResource(backgroundRes)
        }

        initClickedAction()
    }

    fun setTitle(title: String) {
        binding.tvTitle.text = title
    }

    fun setMessage(message: String) {
        binding.tvMessage.text = message
    }

    fun show() {
        visibility = View.VISIBLE
    }

    fun dismiss() {
        visibility = View.GONE
    }

    fun setPositiveButtonTextColor(color: Int) {
        posEnableTextColor = color
        binding.acbPositive.setTextColor(posEnableTextColor)
    }

    fun setNegativeButtonTextColor(color: Int) {
        negEnableTextColor = color
        binding.btNegative.setTextColor(negEnableTextColor)
    }

    fun setPositiveButtonText(text: String) {
        binding.acbPositive.text = text
    }

    fun setNegativeButtonText(text: String) {
        binding.btNegative.text = text
    }

    fun setPositiveButtonEnabled(enabled: Boolean) {
        binding.acbPositive.apply {
            isEnabled = enabled
            setTextColor(if (enabled) posEnableTextColor else posDisableTextColor)
        }
    }

    fun setNegativeButtonEnabled(enabled: Boolean) {
        binding.btNegative.apply {
            isEnabled = enabled
            setTextColor(if (enabled) negEnableTextColor else negDisableTextColor)
        }
    }

    fun setPositiveLoading(isLoading: Boolean) {
        with(binding) {
            acbPositive.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
            cpiPositive.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    fun setNegativeLoading(isLoading: Boolean) {
        with(binding) {
            btNegative.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
            cpiNegative.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    fun setMaskClickedListener(listener: () -> Unit) {
        onMaskClick = listener
    }

    fun setButtonClickListeners(
        onPositive: () -> Unit,
        onNegative: () -> Unit
    ) {
        onPositiveClick = onPositive
        onNegativeClick = onNegative
    }

    private fun initClickedAction() {
        with(binding) {
            btNegative.setOnClickListener {
                onNegativeClick?.invoke()
            }

            acbPositive.setOnClickListener {
                if (cpiPositive.visibility != View.VISIBLE) {
                    onPositiveClick?.invoke()
                }
            }
        }
    }
}