package com.viewsonic.classswift.ui.widget.fastscroll

import android.graphics.Rect
import android.view.View
import android.view.WindowInsets

class ScrollingViewOnApplyWindowInsetsListener @JvmOverloads constructor(
    view: View? = null,
    fastScroller: FastScroller? = null
) : View.OnApplyWindowInsetsListener {
    private val mPadding = Rect()
    private val mFastScroller: FastScroller?

    init {
        if (view != null) {
            mPadding.set(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                view.paddingBottom
            )
        }
        mFastScroller = fastScroller
    }

    override fun onApplyWindowInsets(view: View, insets: WindowInsets): WindowInsets {
        view.setPadding(
            mPadding.left + insets.getSystemWindowInsetLeft(),
            mPadding.top,
            mPadding.right + insets.getSystemWindowInsetRight(),
            mPadding.bottom + insets.getSystemWindowInsetBottom()
        )
        mFastScroller?.setPadding(
            insets.getSystemWindowInsetLeft(),
            0,
            insets.getSystemWindowInsetRight(),
            insets.getSystemWindowInsetBottom()
        )
        return insets
    }
}
