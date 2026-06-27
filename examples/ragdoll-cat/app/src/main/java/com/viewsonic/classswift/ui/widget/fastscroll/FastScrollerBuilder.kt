package com.viewsonic.classswift.ui.widget.fastscroll

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.ViewGroup

class FastScrollerBuilder(
    val context: Context,
    val view: ViewGroup,
    val trackDrawable: Drawable,
    val thumbDrawable: Drawable
) {
    private val mView: ViewGroup = view
    private val mViewHelper: FastScroller.ViewHelper = getViewHelper()
    private var mPadding: Rect = Rect()
    private var mAnimationHelper: FastScroller.AnimationHelper = DefaultAnimationHelper(mView)

    fun build(): FastScroller {
        return FastScroller(
            mView,
            mViewHelper,
            mPadding,
            trackDrawable,
            thumbDrawable,
            mAnimationHelper
        )
    }

    private fun getViewHelper(): FastScroller.ViewHelper {
        return (mView as ViewHelperProvider).getViewHelper()
    }
}
