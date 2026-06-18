package com.viewsonic.classswift.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewCsSnackbarBinding

class CSSnackbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding = ViewCsSnackbarBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = ContextCompat.getDrawable(context, R.drawable.bg_danger_radius600_line_dangervariant_border100)
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true
        elevation = resources.getDimension(R.dimen.mvb_spacing_100)
        val padV = resources.getDimensionPixelSize(R.dimen.mvb_spacing_200)
        val padH = resources.getDimensionPixelSize(R.dimen.mvb_spacing_400)
        setPadding(padH, padV, padH, padV)
    }

    fun setText(text: CharSequence) {
        binding.tvMessage.text = text
    }
}
