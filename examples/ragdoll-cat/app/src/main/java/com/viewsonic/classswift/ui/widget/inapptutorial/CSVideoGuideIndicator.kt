package com.viewsonic.classswift.ui.widget.inapptutorial

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.ViewCsSpinnerBinding
import com.viewsonic.classswift.databinding.ViewSpinnerDropDownWindowBinding
import com.viewsonic.classswift.databinding.ViewToolbarActionButtonBinding
import com.viewsonic.classswift.databinding.ViewVideoGuideIndicatorBinding
import timber.log.Timber

class CSVideoGuideIndicator @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: ViewVideoGuideIndicatorBinding = ViewVideoGuideIndicatorBinding.inflate(LayoutInflater.from(context), this)
    private val indicatorViewList: List<ImageView>

    init {
        with(binding) {
            indicatorViewList = listOf(viewIndicator1, viewIndicator2, viewIndicator3, viewIndicator4, viewIndicator5)
        }
    }

    fun setState(state: State) {
        for (index in indicatorViewList.indices) {
            indicatorViewList[index].isEnabled = index + 1 <= state.ordinal
        }
    }

    enum class State {
        CLEAR,
        TO_INDICATOR_1,
        TO_INDICATOR_2,
        TO_INDICATOR_3,
        TO_INDICATOR_4,
        TO_INDICATOR_5,
    }
}

