package com.viewsonic.classswift.ui.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.core.view.isVisible
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewSigninLoadingButtonBinding

class SignInLoadingButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var binding: ViewSigninLoadingButtonBinding =
        ViewSigninLoadingButtonBinding.inflate(LayoutInflater.from(context), this, true)

    private val defaultResourceId = -1
    private var titleString = ""
    private var isLoading = false
    private var onClickListener: OnSingInLoadingClickListener? = null

    init {
        // set Attr variable
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.SignInLoadingButton,
            0,
            0
        ).apply {
            try {
                setText(this)
                setIcon(this)
            } finally {
                recycle()
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_UP -> {
                if (!isLoading) {
                    onClickListener?.onClicked()
                }
                binding.mcvRoot.isPressed = false
            }
        }
        return super.dispatchTouchEvent(event)
    }

    private fun setText(attr: TypedArray) {
        binding.apply {
            titleString = attr.getString(R.styleable.SignInLoadingButton_ilbText) ?: ""
            //no set attr, hide textView
            if (titleString.isEmpty()) {
                tvTitle.visibility = INVISIBLE
                return
            }
            tvTitle.isVisible = true
            tvTitle.text = titleString
        }
    }

    private fun setIcon(attr: TypedArray) {
        binding.apply {
            @IdRes val iconSrc =
                attr.getResourceId(R.styleable.SignInLoadingButton_ilbIconSrc, defaultResourceId)
            //no set attr, hide icon
            if (iconSrc == defaultResourceId) {
                ivIcon.visibility = INVISIBLE
                return
            }
            ivIcon.visibility = VISIBLE
            ivIcon.setImageResource(iconSrc)
        }
    }

    fun setLoadingStatus(value: Boolean = false) {
        if (isLoading == value) {
            return
        }
        isLoading = value
        binding.apply {
            mcvRoot.isClickable = !isLoading
            circleProgress.isVisible = isLoading
            ivIcon.isVisible = !isLoading
            tvTitle.isVisible = !isLoading
        }
    }

    fun isInLoading(): Boolean {
        return isLoading
    }

    fun setOnClickListener(listener: OnSingInLoadingClickListener) {
        this.onClickListener = listener
    }
}

interface OnSingInLoadingClickListener {
    fun onClicked()
}
