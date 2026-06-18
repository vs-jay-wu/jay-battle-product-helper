package com.viewsonic.classswift.ui.widget

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewLoadingButtonBinding
import com.viewsonic.classswift.utils.extension.dpToPx

class ClassSwiftLoadingButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var binding: ViewLoadingButtonBinding =
        ViewLoadingButtonBinding.inflate(LayoutInflater.from(context), this)
    private val defaultRadius = 8f
    private var currentRadius = defaultRadius
    private val defaultIconSize = 16f
    private val defaultIconMarginEnd = 2.66f.dpToPx()
    private val defaultTextSize = 24f
    private val defaultResourceId = -1
    private val defaultColor = context.getColor(R.color.brand_blue)
    private val defaultBorderLineColor = context.getColor(R.color.transparent)

    @ColorInt
    private var textColor = defaultColor

    @ColorInt
    private var disabledTextColor = defaultColor

    @ColorInt
    private var alertTextColor = defaultColor

    @ColorInt
    private var pressedTextColor = defaultColor

    private var iconTintColor: ColorStateList? = null
    private var clickedIconTintColor: ColorStateList? = null
    private var disableIconTintColor: ColorStateList? = null
    private var alertIconTintColor: ColorStateList? = null

    private var enableString = ""

    private var alertString = ""

    private lateinit var enableBackground: GradientDrawable
    private lateinit var loadingBackground: GradientDrawable
    private lateinit var disableBackground: GradientDrawable
    private lateinit var clickedBackground: GradientDrawable
    private lateinit var alertBackground: GradientDrawable
    private var buttonState = LoadingButtonState.ENABLE
    private var stateListener: OnLoadingButtonStateListener? = null


    init {
        // set Attr variable
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.LoadingButton,
            0,
            0
        ).apply {
            try {
                generateBackgroundResource(this)
                binding.root.background = enableBackground
                setTextStyle(this)
                setIcon(this)
                setButtonState(this)
                isClickable = true
            } finally {
                recycle()
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isEnableState()) {
                    binding.root.background = clickedBackground
                    binding.tvText.setTextColor(pressedTextColor)
                    binding.ivIcon.imageTintList = clickedIconTintColor ?: iconTintColor
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isEnableState()) {
                    stateListener?.onEnableClicked()
                    if (isEnableState()) {
                        binding.root.background = enableBackground
                        binding.tvText.setTextColor(textColor)
                        binding.ivIcon.imageTintList = iconTintColor
                    }
                }
                stateListener?.onClickedWithState(buttonState)
            }

            MotionEvent.ACTION_CANCEL -> {
                if (isEnableState()) {
                    binding.root.background = enableBackground
                    binding.tvText.setTextColor(textColor)
                    binding.ivIcon.imageTintList = iconTintColor
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun generateBackgroundResource(attr: TypedArray) {
        val radius = attr.getDimension(R.styleable.LoadingButton_lbRadius, defaultRadius)
        currentRadius = radius
        val borderWidth = attr.getDimensionPixelSize(R.styleable.LoadingButton_lbBorderWidth, 2)
        // 狀態 - Enable
        @ColorInt val backgroundColor =
            attr.getColor(R.styleable.LoadingButton_lbBackgroundColor, defaultColor)
        @ColorInt val borderLineColor =
            attr.getColor(R.styleable.LoadingButton_lbBorderColor, defaultBorderLineColor)
        enableBackground = createGradientDrawable(backgroundColor, borderLineColor, radius, borderWidth)

        // 狀態 - Pressed
        @ColorInt val pressedBackgroundColor =
            attr.getColor(R.styleable.LoadingButton_lbClickedBackgroundColor, backgroundColor)
        @ColorInt val pressOrderLineColor =
            attr.getColor(R.styleable.LoadingButton_lbClickedBorderColor, defaultBorderLineColor)
        clickedBackground =
            createGradientDrawable(
                pressedBackgroundColor,
                pressOrderLineColor,
                radius,
                borderWidth
            )

        // 狀態 - Disabled
        @ColorInt val disableBackgroundColor =
            attr.getColor(R.styleable.LoadingButton_lbDisableBackgroundColor, backgroundColor)
        @ColorInt val disableBorderLineColor =
            attr.getColor(R.styleable.LoadingButton_lbDisableBorderColor, defaultBorderLineColor)
        disableBackground =
            createGradientDrawable(disableBackgroundColor, disableBorderLineColor, radius, borderWidth)

        // 狀態 - Loading
        @ColorInt val defaultLoadingColor = context.getColor(R.color.neutral_200)
        @ColorInt val loadingBackgroundColor =
            attr.getColor(R.styleable.LoadingButton_lbLoadingBackground, defaultLoadingColor)
        @ColorInt val loadingBorderLineColor =
            attr.getColor(R.styleable.LoadingButton_lbLoadingBorderColor, defaultLoadingColor)
        loadingBackground =
            createGradientDrawable(loadingBackgroundColor, loadingBorderLineColor, radius, borderWidth)

        // 狀態 - Alert
        @ColorInt val alertBackgroundColor =
            attr.getColor(R.styleable.LoadingButton_lbAlertBackgroundColor, backgroundColor)
        @ColorInt val alertBorderLineColor =
            attr.getColor(R.styleable.LoadingButton_lbAlertBorderColor, defaultBorderLineColor)
        alertBackground = createGradientDrawable(alertBackgroundColor, alertBorderLineColor, radius, borderWidth)
    }

    private fun createGradientDrawable(
        @ColorInt backgroundColor: Int,
        @ColorInt borderLineColor: Int,
        radius: Float,
        borderWidth: Int = 2
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius // 圓角半徑
            setColor(backgroundColor) // 背景顏色
            setStroke(borderWidth, borderLineColor) // 邊框寬度和顏色
        }
    }

    private fun setTextStyle(attr: TypedArray) {
        enableString = attr.getString(R.styleable.LoadingButton_lbText) ?: ""
        alertString = attr.getString(R.styleable.LoadingButton_lbAlertText) ?: ""

        //no set attr, hide textView
        if (enableString.isEmpty()) {
            binding.tvText.visibility = INVISIBLE
            return
        }

        if (alertString.isNotEmpty()) {
            alertTextColor =  attr.getInt(R.styleable.LoadingButton_lbAlertTextColor, defaultColor)
        }

        binding.tvText.isVisible = true
        binding.tvText.text = enableString
        val textSize =
            attr.getDimension(R.styleable.LoadingButton_lbTextSize, defaultTextSize)
        textColor =
            attr.getInt(R.styleable.LoadingButton_lbTextColor, defaultColor)
        pressedTextColor = attr.getColor(R.styleable.LoadingButton_lbClickedTextColor, textColor)
        disabledTextColor = attr.getInt(R.styleable.LoadingButton_lbDisableTextColor, textColor)
        binding.tvText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        binding.tvText.setTextColor(textColor)
        attr.getString(R.styleable.LoadingButton_lbFontFamily)?.let { family ->
            binding.tvText.typeface = Typeface.create(family, Typeface.NORMAL)
        }
    }

    private fun setIcon(attr: TypedArray) {
        @IdRes val iconSrc =
            attr.getResourceId(R.styleable.LoadingButton_lbImgSrc, defaultResourceId)
        //no set attr, hide icon
        if (iconSrc == defaultResourceId) {
            binding.ivIcon.isVisible = false
            iconTintColor = null
            clickedIconTintColor = null
            disableIconTintColor = null
            alertIconTintColor = null
            return
        }
        binding.ivIcon.isVisible = true
        val iconWidth =
            attr.getDimension(R.styleable.LoadingButton_lbIconWidth, defaultIconSize)
        val iconHeight =
            attr.getDimension(R.styleable.LoadingButton_lbIconHeight, defaultIconSize)
        val iconMarginEnd =
            attr.getDimension(R.styleable.LoadingButton_lbIconMarginEnd, defaultIconMarginEnd)
        val params = LayoutParams(iconWidth.toInt(), iconHeight.toInt())
        params.topToTop = LayoutParams.PARENT_ID
        params.bottomToBottom = LayoutParams.PARENT_ID
        params.startToStart = LayoutParams.PARENT_ID
        params.endToStart = binding.tvText.id
        params.horizontalChainStyle = LayoutParams.CHAIN_PACKED
        params.marginEnd = iconMarginEnd.toInt()
        binding.ivIcon.layoutParams = params
        binding.ivIcon.setImageResource(iconSrc)
        iconTintColor = if (attr.hasValue(R.styleable.LoadingButton_lbIconTint)) {
            ColorStateList.valueOf(attr.getColor(R.styleable.LoadingButton_lbIconTint, defaultColor))
        } else {
            null
        }
        binding.ivIcon.imageTintList = iconTintColor
        clickedIconTintColor = if (attr.hasValue(R.styleable.LoadingButton_lbClickedIconTint)) {
            ColorStateList.valueOf(
                attr.getColor(R.styleable.LoadingButton_lbClickedIconTint, defaultColor)
            )
        } else {
            null
        }
        disableIconTintColor = if (attr.hasValue(R.styleable.LoadingButton_lbDisableIconTint)) {
            ColorStateList.valueOf(
                attr.getColor(R.styleable.LoadingButton_lbDisableIconTint, defaultColor)
            )
        } else {
            null
        }
        alertIconTintColor = if (attr.hasValue(R.styleable.LoadingButton_lbAlertIconTint)) {
            ColorStateList.valueOf(
                attr.getColor(R.styleable.LoadingButton_lbAlertIconTint, defaultColor)
            )
        } else {
            null
        }
    }

    private fun setButtonState(attr: TypedArray) {
        val stateValue =
            attr.getInt(R.styleable.LoadingButton_lbButtonState, LoadingButtonState.ENABLE.ordinal)
        val state = when (stateValue) {
            0 -> LoadingButtonState.LOADING
            1 -> LoadingButtonState.DISABLE
            2 -> LoadingButtonState.ENABLE
            3 -> LoadingButtonState.ALERT
            else -> LoadingButtonState.ENABLE
        }
        setState(state)
    }

    fun setOnCustomClickListener(listener: OnLoadingButtonStateListener) {
        this.stateListener = listener
    }

    fun setEnableText(text: String) {
        enableString = text
        if (buttonState == LoadingButtonState.ENABLE) {
            binding.tvText.text = enableString
        }
    }

    fun setEnable() {
        if (buttonState != LoadingButtonState.ENABLE) {
            notLoadingWidgetState()
            buttonState = LoadingButtonState.ENABLE
            binding.tvText.text = enableString
            binding.tvText.setTextColor(textColor)
            binding.ivIcon.imageTintList = iconTintColor
            binding.root.background = enableBackground
            stateListener?.onStateChange(buttonState)
        }
    }

    fun setLoading() {
        if (buttonState != LoadingButtonState.LOADING) {
            buttonState = LoadingButtonState.LOADING
            binding.lavLoading.isVisible = true
            binding.lavLoading.playAnimation()
            binding.tvText.isInvisible = true
            binding.ivIcon.isInvisible = true
            binding.root.background = loadingBackground
            stateListener?.onStateChange(buttonState)
        }
    }

    fun setDisable() {
        if (buttonState != LoadingButtonState.DISABLE) {
            notLoadingWidgetState()
            buttonState = LoadingButtonState.DISABLE
            binding.tvText.text = enableString
            binding.tvText.setTextColor(disabledTextColor)
            binding.ivIcon.imageTintList = disableIconTintColor ?: iconTintColor
            binding.root.background = disableBackground
            stateListener?.onStateChange(buttonState)
        }
    }

    private fun notLoadingWidgetState() {
        if (buttonState == LoadingButtonState.LOADING) {
            binding.lavLoading.cancelAnimation()
            binding.lavLoading.isVisible = false
            binding.tvText.isVisible = true
            binding.ivIcon.isVisible = true
            binding.ivIcon.imageTintList = iconTintColor
        }
    }

    fun setState(state: LoadingButtonState = buttonState) {
        when (state) {
            LoadingButtonState.LOADING -> setLoading()
            LoadingButtonState.ENABLE -> setEnable()
            LoadingButtonState.DISABLE -> setDisable()
            LoadingButtonState.ALERT -> {
                if (buttonState != LoadingButtonState.ALERT && alertString.isNotEmpty()) {
                    notLoadingWidgetState()
                    buttonState = LoadingButtonState.ALERT
                    binding.tvText.setTextColor(alertTextColor)
                    binding.tvText.text = alertString
                    binding.ivIcon.imageTintList = alertIconTintColor ?: iconTintColor
                    binding.root.background = alertBackground
                    stateListener?.onStateChange(buttonState)
                }
            }
        }
    }

    fun clearTextTransformation() {
        binding.tvText.transformationMethod = null
    }

    fun isEnableState(): Boolean {
        return buttonState == LoadingButtonState.ENABLE
    }

    fun setButtonStyle(
        @ColorInt defaultBgColor: Int,
        @ColorInt defaultTextColor: Int,
        @ColorInt defaultBorderColor: Int,
        borderWidthPx: Int,
        @ColorInt clickedBgColor: Int,
        @ColorInt clickedTextColor: Int,
        @ColorInt clickedBorderColor: Int
    ) {
        enableBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = currentRadius
            setColor(defaultBgColor)
            setStroke(borderWidthPx, defaultBorderColor)
        }
        clickedBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = currentRadius
            setColor(clickedBgColor)
            setStroke(borderWidthPx, clickedBorderColor)
        }
        textColor = defaultTextColor
        pressedTextColor = clickedTextColor
        if (isEnableState()) {
            binding.root.background = enableBackground
            binding.tvText.setTextColor(textColor)
        }
    }
}

enum class LoadingButtonState {
    LOADING, DISABLE, ENABLE, ALERT
}

interface OnLoadingButtonStateListener {
    fun onStateChange(state: LoadingButtonState) = Unit
    fun onClickedWithState(state: LoadingButtonState) = Unit
    fun onEnableClicked()
}
