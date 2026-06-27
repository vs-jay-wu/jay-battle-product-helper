package com.viewsonic.classswift.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewMvbToastBinding

class MvbToast @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewMvbToastBinding =
        ViewMvbToastBinding.inflate(LayoutInflater.from(context), this)

    enum class ToastLevel { DEFAULT, NEUTRAL, INFO, SUCCESS, WARNING, DANGER }

    private data class LevelStyle(
        @ColorInt val rootBgColor: Int,
        @ColorInt val rootBorderColor: Int,
        @ColorInt val contentColor: Int,
        @ColorInt val accentColor: Int
    )

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.MvbToast, 0, 0).apply {
            try {
                val text = getString(R.styleable.MvbToast_mtText) ?: ""
                val iconRes = getResourceId(R.styleable.MvbToast_mtIconSrc, R.drawable.ic_circle_letter_i)
                val actionTitle = getString(R.styleable.MvbToast_mtActionTitle) ?: ""
                val hasActionButton = getBoolean(R.styleable.MvbToast_mtHasActionButton, false)
                val hasCloseButton = getBoolean(R.styleable.MvbToast_mtHasCloseButton, true)
                val level = ToastLevel.entries[getInt(R.styleable.MvbToast_mtToastLevel, 0)]

                binding.tvMessage.text = text
                binding.ivIcon.setImageResource(iconRes)
                binding.cslbAction.isVisible = hasActionButton
                binding.ivClose.isVisible = hasCloseButton
                if (hasActionButton) binding.cslbAction.setEnableText(actionTitle)

                applyLevel(level)
            } finally {
                recycle()
            }
        }
    }

    private fun applyLevel(level: ToastLevel) {
        val style = levelStyle(level)
        background = createRootBackground(style.rootBgColor, style.rootBorderColor)
        binding.tvMessage.setTextColor(style.contentColor)
        val tint = ColorStateList.valueOf(style.contentColor)
        binding.ivIcon.imageTintList = tint
        binding.ivClose.imageTintList = tint
        binding.cslbAction.setButtonStyle(
            defaultBgColor = context.getColor(R.color.neutral_0),
            defaultTextColor = style.accentColor,
            defaultBorderColor = style.accentColor,
            borderWidthPx = resources.getDimensionPixelSize(R.dimen.border_100),
            clickedBgColor = style.accentColor,
            clickedTextColor = context.getColor(R.color.neutral_0),
            clickedBorderColor = style.accentColor
        )
    }

    private fun levelStyle(level: ToastLevel): LevelStyle = when (level) {
        ToastLevel.DEFAULT -> LevelStyle(
            rootBgColor = context.getColor(R.color.neutral_100),
            rootBorderColor = context.getColor(R.color.neutral_100),
            contentColor = context.getColor(R.color.neutral_900),
            accentColor = context.getColor(R.color.violet_500)
        )
        ToastLevel.NEUTRAL -> LevelStyle(
            rootBgColor = context.getColor(R.color.neutral_1000),
            rootBorderColor = context.getColor(R.color.neutral_1000),
            contentColor = context.getColor(R.color.neutral_0),
            accentColor = context.getColor(R.color.neutral_1000)
        )
        ToastLevel.INFO -> LevelStyle(
            rootBgColor = context.getColor(R.color.sky_100),
            rootBorderColor = context.getColor(R.color.sky_600),
            contentColor = context.getColor(R.color.neutral_900),
            accentColor = context.getColor(R.color.sky_600)
        )
        ToastLevel.SUCCESS -> LevelStyle(
            rootBgColor = context.getColor(R.color.green_100),
            rootBorderColor = context.getColor(R.color.green_700),
            contentColor = context.getColor(R.color.neutral_900),
            accentColor = context.getColor(R.color.green_700)
        )
        ToastLevel.WARNING -> LevelStyle(
            rootBgColor = context.getColor(R.color.yellow_100),
            rootBorderColor = context.getColor(R.color.yellow_900),
            contentColor = context.getColor(R.color.neutral_900),
            accentColor = context.getColor(R.color.yellow_900)
        )
        ToastLevel.DANGER -> LevelStyle(
            rootBgColor = context.getColor(R.color.red_100),
            rootBorderColor = context.getColor(R.color.red_600),
            contentColor = context.getColor(R.color.neutral_900),
            accentColor = context.getColor(R.color.red_600)
        )
    }

    private fun createRootBackground(@ColorInt bgColor: Int, @ColorInt borderColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = resources.getDimension(R.dimen.radius_200)
            setColor(bgColor)
            setStroke(resources.getDimensionPixelSize(R.dimen.border_100), borderColor)
        }
    }

    fun setText(text: CharSequence) {
        binding.tvMessage.text = text
    }

    fun setIcon(@DrawableRes iconRes: Int) {
        binding.ivIcon.setImageResource(iconRes)
    }

    fun setActionTitle(title: CharSequence) {
        binding.cslbAction.setEnableText(title.toString())
    }

    fun setActionOnClickListener(listener: OnClickListener) {
        binding.cslbAction.setOnClickListener(listener)
    }

    fun setCloseOnClickListener(listener: OnClickListener) {
        binding.ivClose.setOnClickListener(listener)
    }
}
