package com.viewsonic.classswift.ui.widget.fastscroll

import android.view.View
import android.view.animation.Interpolator
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import kotlin.math.max

class DefaultAnimationHelper(view: View) : FastScroller.AnimationHelper {
    private var mView: View = view
    private var mScrollbarAutoHideEnabled = true
    private var mShowingScrollbar = true

    override fun showScrollbar(trackView: View, thumbView: View) {
        if (mShowingScrollbar) {
            return
        }
        mShowingScrollbar = true

        trackView.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(SHOW_DURATION_MILLIS.toLong())
            .setInterpolator(SHOW_SCROLLBAR_INTERPOLATOR)
            .start()
        thumbView.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(SHOW_DURATION_MILLIS.toLong())
            .setInterpolator(SHOW_SCROLLBAR_INTERPOLATOR)
            .start()
    }

    override fun hideScrollbar(trackView: View, thumbView: View) {
        if (!mShowingScrollbar) {
            return
        }
        mShowingScrollbar = false

        val isLayoutRtl = mView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL
        val width: Int = max(trackView.width.toInt(), thumbView.width.toInt())
        var translationX: Float = if (isLayoutRtl) {
            (if (trackView.left == 0) - width else 0).toFloat()
        } else {
            (if (trackView.right == mView.width) width else 0).toFloat()
        }

        trackView.animate()
            .alpha(0f)
            .translationX(translationX)
            .setDuration(HIDE_DURATION_MILLIS.toLong())
            .setInterpolator(HIDE_SCROLLBAR_INTERPOLATOR)
            .start()
        thumbView.animate()
            .alpha(0f)
            .translationX(translationX)
            .setDuration(HIDE_DURATION_MILLIS.toLong())
            .setInterpolator(HIDE_SCROLLBAR_INTERPOLATOR)
            .start()
    }

    override fun isScrollbarAutoHideEnabled(): Boolean {
        return mScrollbarAutoHideEnabled
    }

    fun setScrollbarAutoHideEnabled(enabled: Boolean) {
        mScrollbarAutoHideEnabled = enabled
    }

    override fun getScrollbarAutoHideDelayMillis(): Int {
        return AUTO_HIDE_SCROLLBAR_DELAY_MILLIS
    }

    companion object {
        private const val SHOW_DURATION_MILLIS = 150
        private const val HIDE_DURATION_MILLIS = 200
        private const val AUTO_HIDE_SCROLLBAR_DELAY_MILLIS = 1500
        private val SHOW_SCROLLBAR_INTERPOLATOR: Interpolator = LinearOutSlowInInterpolator()
        private val HIDE_SCROLLBAR_INTERPOLATOR: Interpolator = FastOutLinearInInterpolator()
    }
}
