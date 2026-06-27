package com.viewsonic.classswift.ui.widget.fastscroll

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.core.math.MathUtils
import com.viewsonic.classswift.R
import kotlin.math.abs
import kotlin.math.max

class FastScroller(
    view: ViewGroup,
    viewHelper: ViewHelper,
    padding: Rect,
    trackDrawable: Drawable,
    thumbDrawable: Drawable,
    animationHelper: AnimationHelper
) {
    private val mMinTouchTargetSize: Int = view.resources.getDimensionPixelSize(
        R.dimen.fastscroll_min_touch_target_size
    )
    private val mTouchSlop: Int

    private val mView: ViewGroup
    private val mViewHelper: ViewHelper
    private var mUserPadding: Rect = Rect()
    private val mAnimationHelper: AnimationHelper

    private val mTrackWidth: Int
    private val mThumbWidth: Int
    private val mThumbHeight: Int

    private val mTrackView: View
    private val mThumbView: View

    private var mScrollbarEnabled = false
    private var mThumbOffset = 0

    private var mDownX = 0f
    private var mDownY = 0f
    private var mLastY = 0f
    private var mDragStartY = 0f
    private var mDragStartThumbOffset = 0
    private var mDragging = false

    private val mAutoHideScrollbarRunnable = Runnable { this.autoHideScrollbar() }
    private val mTempRect = Rect()

    init {
        val context = view.context
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop

        mView = view
        mViewHelper = viewHelper
        mUserPadding = padding
        mAnimationHelper = animationHelper

        mTrackWidth = requireNonNegative(
            trackDrawable.intrinsicWidth,
            "trackDrawable.getIntrinsicWidth() < 0"
        )
        mThumbWidth = requireNonNegative(
            thumbDrawable.intrinsicWidth,
            "thumbDrawable.getIntrinsicWidth() < 0"
        )
        mThumbHeight = 300

        mTrackView = View(context)
        mTrackView.background = trackDrawable
        mThumbView = View(context)
        mThumbView.background = thumbDrawable

        val overlay = mView.getOverlay()
        overlay.add(mTrackView)
        overlay.add(mThumbView)

        postAutoHideScrollbar(false)

        mViewHelper.addOnPreDrawListener(Runnable { this.onPreDraw() })
        mViewHelper.addOnScrollChangedListener(Runnable { this.onScrollChanged() })
        mViewHelper.addOnTouchEventListener(Predicate { event: MotionEvent? ->
            this.onTouchEvent(
                event!!
            )
        })
    }

    fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        if (mUserPadding.left == left && mUserPadding.top == top && mUserPadding.right == right && mUserPadding.bottom == bottom) {
            return
        }
        mUserPadding.set(left, top, right, bottom)
        mView.invalidate()
    }

    private fun getPadding(): Rect {
        mTempRect.set(
            mView.paddingLeft, mView.paddingTop, mView.paddingRight,
            mView.paddingBottom
        )
        return mTempRect
    }

    private fun onPreDraw() {
        updateScrollbarState()
        mTrackView.visibility = if (mScrollbarEnabled) View.VISIBLE else View.INVISIBLE
        mThumbView.visibility = if (mScrollbarEnabled) View.VISIBLE else View.INVISIBLE

        val layoutDirection = mView.getLayoutDirection()
        mTrackView.layoutDirection = layoutDirection
        mThumbView.layoutDirection = layoutDirection

        val isLayoutRtl = layoutDirection == View.LAYOUT_DIRECTION_RTL
        val viewWidth = mView.width
        val viewHeight = mView.height

        val padding = getPadding()
        val trackLeft = if (isLayoutRtl) padding.left else viewWidth - padding.right - mTrackWidth
        layoutView(
            mTrackView, trackLeft, padding.top, trackLeft + mTrackWidth,
            max((viewHeight - padding.bottom).toInt(), padding.top.toInt())
        )
        val thumbLeft = if (isLayoutRtl) padding.left else viewWidth - padding.right - mThumbWidth
        val thumbTop = padding.top + mThumbOffset
        layoutView(
            mThumbView, thumbLeft, thumbTop, thumbLeft + mThumbWidth,
            thumbTop + mThumbHeight
        )
    }

    private fun updateScrollbarState() {
        val scrollOffsetRange = getScrollOffsetRange()
        mScrollbarEnabled = scrollOffsetRange > 0
        mThumbOffset = if (mScrollbarEnabled) {
            (getThumbOffsetRange().toLong() * mViewHelper.getScrollOffset() / scrollOffsetRange).toInt()
        } else {
            0
        }
    }

    private fun layoutView(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        val scrollX = mView.scrollX
        val scrollY = mView.scrollY
        view.layout(scrollX + left, scrollY + top, scrollX + right, scrollY + bottom)
    }

    private fun onScrollChanged() {
        updateScrollbarState()
        if (!mScrollbarEnabled) {
            return
        }

        mAnimationHelper.showScrollbar(mTrackView, mThumbView)
        postAutoHideScrollbar(false)
    }

    private fun onTouchEvent(event: MotionEvent): Boolean {
        if (!mScrollbarEnabled) {
            return false
        }

        val eventX = event.x
        val eventY = event.y
        val padding = getPadding()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mDownX = eventX
                mDownY = eventY

                if (mThumbView.alpha > 0 && isInViewTouchTarget(mThumbView, eventX, eventY)) {
                    mDragStartY = eventY
                    mDragStartThumbOffset = mThumbOffset
                    setDragging(true)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!mDragging && isInViewTouchTarget(mTrackView, mDownX, mDownY) && abs((eventY - mDownY).toDouble()) > mTouchSlop) {
                    if (isInViewTouchTarget(mThumbView, mDownX, mDownY)) {
                        mDragStartY = mLastY
                        mDragStartThumbOffset = mThumbOffset
                    } else {
                        mDragStartY = eventY
                        mDragStartThumbOffset = (eventY - padding.top - mThumbHeight / 2f).toInt()
                        scrollToThumbOffset(mDragStartThumbOffset)
                    }
                    setDragging(true)
                }

                if (mDragging) {
                    val thumbOffset = mDragStartThumbOffset + (eventY - mDragStartY).toInt()
                    scrollToThumbOffset(thumbOffset)
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                setDragging(false)
            }
        }
        mLastY = eventY
        return mDragging
    }

    private fun isInViewTouchTarget(view: View, x: Float, y: Float): Boolean {
        val scrollX = mView.scrollX
        val scrollY = mView.scrollY

        return isInTouchTarget(x, view.left - scrollX, view.right - scrollX, 0, mView.width)
                && isInTouchTarget(y, view.top - scrollY, view.bottom - scrollY, 0, mView.height)
    }

    private fun isInTouchTarget(
        position: Float, viewStart: Int, viewEnd: Int, parentStart: Int,
        parentEnd: Int
    ): Boolean {
        val viewSize = viewEnd - viewStart
        if (viewSize >= mMinTouchTargetSize) {
            return position >= viewStart && position < viewEnd
        }

        var touchTargetStart = viewStart - (mMinTouchTargetSize - viewSize) / 2
        if (touchTargetStart < parentStart) {
            touchTargetStart = parentStart
        }

        var touchTargetEnd = touchTargetStart + mMinTouchTargetSize
        if (touchTargetEnd > parentEnd) {
            touchTargetEnd = parentEnd
            touchTargetStart = touchTargetEnd - mMinTouchTargetSize
            if (touchTargetStart < parentStart) {
                touchTargetStart = parentStart
            }
        }
        return position >= touchTargetStart && position < touchTargetEnd
    }

    private fun scrollToThumbOffset(thumbOffset: Int) {
        var thumbOffset = thumbOffset
        val thumbOffsetRange = getThumbOffsetRange()
        thumbOffset = MathUtils.clamp(thumbOffset, 0, thumbOffsetRange)
        val scrollOffset = (getScrollOffsetRange().toLong() * thumbOffset / thumbOffsetRange).toInt()
        mViewHelper.scrollTo(scrollOffset)
    }

    private fun getScrollOffsetRange(): Int {
        return mViewHelper.getScrollRange() - mView.height
    }

    private fun getThumbOffsetRange(): Int {
        val padding = getPadding()
        return mView.height - padding.top - padding.bottom - mThumbHeight
    }

    private fun setDragging(dragging: Boolean) {
        if (mDragging == dragging) {
            return
        }
        mDragging = dragging

        if (mDragging) {
            mView.getParent().requestDisallowInterceptTouchEvent(true)
        }

        mTrackView.setPressed(mDragging)
        mThumbView.setPressed(mDragging)

        if (mDragging) {
            cancelAutoHideScrollbar()
            mAnimationHelper.showScrollbar(mTrackView, mThumbView)
        } else {
            postAutoHideScrollbar(false)
        }
    }

    private fun postAutoHideScrollbar(isAutoHide: Boolean) {
        if (!isAutoHide) {
            return
        }
        cancelAutoHideScrollbar()
        if (mAnimationHelper.isScrollbarAutoHideEnabled()) {
            mView.postDelayed(
                mAutoHideScrollbarRunnable,
                mAnimationHelper.getScrollbarAutoHideDelayMillis().toLong()
            )
        }
    }

    private fun autoHideScrollbar() {
        if (mDragging) {
            return
        }
        mAnimationHelper.hideScrollbar(mTrackView, mThumbView)
    }

    private fun cancelAutoHideScrollbar() {
        mView.removeCallbacks(mAutoHideScrollbarRunnable)
    }

    interface ViewHelper {
        fun addOnPreDrawListener(onPreDraw: Runnable)
        fun addOnScrollChangedListener(onScrollChanged: Runnable)
        fun addOnTouchEventListener(onTouchEvent: Predicate<MotionEvent?>)
        fun getScrollRange(): Int
        fun getScrollOffset(): Int
        fun scrollTo(offset: Int)
    }

    interface AnimationHelper {
        fun showScrollbar(trackView: View, thumbView: View)
        fun hideScrollbar(trackView: View, thumbView: View)
        fun isScrollbarAutoHideEnabled(): Boolean
        fun getScrollbarAutoHideDelayMillis(): Int
    }

    companion object {
        private fun requireNonNegative(value: Int, message: String): Int {
            require(value >= 0) { message }
            return value
        }
    }
}
