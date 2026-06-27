package com.viewsonic.classswift.ui.tool

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.view.MotionEvent
import androidx.annotation.IntDef
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class NoAutoHiddenFastScroller(
    private var mRecyclerView: RecyclerView, // Final values for the vertical scroll bar
    val mVerticalThumbDrawable: StateListDrawable,
    val verticalTrackDrawable: Drawable, // Final values for the horizontal scroll bar
    val mHorizontalThumbDrawable: StateListDrawable,
    val horizontalTrackDrawable: Drawable,
    defaultWidth: Int,
    private val mScrollbarMinimumRange: Int,
    private val mMargin: Int
) : RecyclerView.ItemDecoration(),
    RecyclerView.OnItemTouchListener {
    @IntDef(STATE_HIDDEN, STATE_VISIBLE, STATE_DRAGGING)
    @Retention(
        AnnotationRetention.SOURCE
    )
    private annotation class State

    @IntDef(DRAG_X, DRAG_Y, DRAG_NONE)
    @Retention(
        AnnotationRetention.SOURCE
    )
    private annotation class DragState

    @IntDef(
        ANIMATION_STATE_OUT, ANIMATION_STATE_FADING_IN, ANIMATION_STATE_IN, ANIMATION_STATE_FADING_OUT
    )
    @Retention(
        AnnotationRetention.SOURCE
    )
    private annotation class AnimationState

    private val mVerticalThumbWidth =
        max(defaultWidth.toDouble(), mVerticalThumbDrawable.intrinsicWidth.toDouble())
            .toInt()
    private val mVerticalTrackWidth =
        max(defaultWidth.toDouble(), verticalTrackDrawable.intrinsicWidth.toDouble())
            .toInt()

    private val mHorizontalThumbHeight =
        max(defaultWidth.toDouble(), mHorizontalThumbDrawable.intrinsicWidth.toDouble())
            .toInt()
    private val mHorizontalTrackHeight =
        max(defaultWidth.toDouble(), horizontalTrackDrawable.intrinsicWidth.toDouble())
            .toInt()

    // Dynamic values for the vertical scroll bar
    var mVerticalThumbHeight: Int = 0
    var mVerticalThumbCenterY: Int = 0
    var mVerticalDragY: Float = 0f

    // Dynamic values for the horizontal scroll bar
    var mHorizontalThumbWidth: Int = 0
    var mHorizontalThumbCenterX: Int = 0
    var mHorizontalDragX: Float = 0f

    private var mRecyclerViewWidth = 0
    private var mRecyclerViewHeight = 0

    /**
     * Whether the document is long/wide enough to require scrolling. If not, we don't show the
     * relevant scroller.
     */
    private var mNeedVerticalScrollbar = false
    private var mNeedHorizontalScrollbar = false

    private val isHiddenEnabled = false
    @State
    private var mState: Int = if (isHiddenEnabled) {
        STATE_HIDDEN
    } else {
        STATE_VISIBLE
    }

    @DragState
    private var mDragState: Int = DRAG_NONE

    private val mVerticalRange = IntArray(2)
    private val mHorizontalRange = IntArray(2)
    /* synthetic access */val mShowHideAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)

    /* synthetic access */@AnimationState
    var mAnimationState: Int = ANIMATION_STATE_OUT
    private val mHideRunnable = Runnable { hide(HIDE_DURATION_MS) }
    private val mOnScrollListener: RecyclerView.OnScrollListener =
        object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateScrollPosition(
                    recyclerView.computeHorizontalScrollOffset(),
                    recyclerView.computeVerticalScrollOffset()
                )
            }
        }

    init {
        mVerticalThumbDrawable.alpha = SCROLLBAR_FULL_OPAQUE
        verticalTrackDrawable.alpha = SCROLLBAR_FULL_OPAQUE

        mShowHideAnimator.addListener(AnimatorListener())
        mShowHideAnimator.addUpdateListener(AnimatorUpdater())

        destroyCallbacks()
        setupCallbacks()

        mRecyclerView.post {
            updateScrollPosition(
                mRecyclerView.computeHorizontalScrollOffset(),
                mRecyclerView.computeVerticalScrollOffset()
            )
        }
    }

    fun attachToRecyclerView(recyclerView: RecyclerView) {
        if (mRecyclerView === recyclerView) {
            return  // nothing to do
        }
        destroyCallbacks()
        mRecyclerView = recyclerView
        setupCallbacks()
    }

    private fun setupCallbacks() {
        mRecyclerView.addItemDecoration(this)
        mRecyclerView.addOnItemTouchListener(this)
        mRecyclerView.addOnScrollListener(mOnScrollListener)
    }

    private fun destroyCallbacks() {
        mRecyclerView.removeItemDecoration(this)
        mRecyclerView.removeOnItemTouchListener(this)
        mRecyclerView.removeOnScrollListener(mOnScrollListener)
        cancelHide()
    }

    fun requestRedraw() {
        mRecyclerView.invalidate()
    }

    fun setState(@State state: Int) {
        if (state == STATE_DRAGGING && mState != STATE_DRAGGING) {
            mVerticalThumbDrawable.setState(PRESSED_STATE_SET)
            cancelHide()
        }

        if (state == STATE_HIDDEN) {
            requestRedraw()
        } else {
            show()
        }

        if (mState == STATE_DRAGGING && state != STATE_DRAGGING) {
            mVerticalThumbDrawable.setState(EMPTY_STATE_SET)
            resetHideDelay(HIDE_DELAY_AFTER_DRAGGING_MS)
        } else if (state == STATE_VISIBLE) {
            resetHideDelay(HIDE_DELAY_AFTER_VISIBLE_MS)
        }
        mState = state
    }

    private val isLayoutRTL: Boolean
        get() = ViewCompat.getLayoutDirection(mRecyclerView) == ViewCompat.LAYOUT_DIRECTION_RTL

    val isDragging: Boolean
        get() = mState == STATE_DRAGGING

    val isVisible: Boolean
        get() = mState == STATE_VISIBLE

    fun show() {
        when (mAnimationState) {
            ANIMATION_STATE_FADING_OUT -> {
                mShowHideAnimator.cancel()
                mAnimationState = ANIMATION_STATE_FADING_IN
                mShowHideAnimator.setFloatValues(mShowHideAnimator.animatedValue as Float, 1f)
                mShowHideAnimator.setDuration(SHOW_DURATION_MS.toLong())
                mShowHideAnimator.startDelay = 0
                mShowHideAnimator.start()
            }

            ANIMATION_STATE_OUT -> {
                mAnimationState = ANIMATION_STATE_FADING_IN
                mShowHideAnimator.setFloatValues(mShowHideAnimator.animatedValue as Float, 1f)
                mShowHideAnimator.setDuration(SHOW_DURATION_MS.toLong())
                mShowHideAnimator.startDelay = 0
                mShowHideAnimator.start()
            }
        }
    }

    
    fun hide(duration: Int) {
        when (mAnimationState) {
            ANIMATION_STATE_FADING_IN -> {
                mShowHideAnimator.cancel()
                mAnimationState = ANIMATION_STATE_FADING_OUT
                mShowHideAnimator.setFloatValues(mShowHideAnimator.animatedValue as Float, 0f)
                mShowHideAnimator.setDuration(duration.toLong())
                mShowHideAnimator.start()
            }

            ANIMATION_STATE_IN -> {
                mAnimationState = ANIMATION_STATE_FADING_OUT
                mShowHideAnimator.setFloatValues(mShowHideAnimator.animatedValue as Float, 0f)
                mShowHideAnimator.setDuration(duration.toLong())
                mShowHideAnimator.start()
            }
        }
    }

    private fun cancelHide() {
        if (isHiddenEnabled) {
            mRecyclerView.removeCallbacks(mHideRunnable)
        }
    }

    private fun resetHideDelay(delay: Int) {
        if (isHiddenEnabled) {
            cancelHide()
            mRecyclerView.postDelayed(mHideRunnable, delay.toLong())
        }
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (mRecyclerViewWidth != mRecyclerView.width
            || mRecyclerViewHeight != mRecyclerView.height
        ) {
            mRecyclerViewWidth = mRecyclerView.width
            mRecyclerViewHeight = mRecyclerView.height
            // This is due to the different events ordering when keyboard is opened or
            // retracted vs rotate. Hence to avoid corner cases we just disable the
            // scroller when size changed, and wait until the scroll position is recomputed
            // before showing it back.
            setState(STATE_HIDDEN)
            return
        }

        if (mAnimationState != ANIMATION_STATE_OUT) {
            if (mNeedVerticalScrollbar) {
                drawVerticalScrollbar(canvas)
            }
            if (mNeedHorizontalScrollbar) {
                drawHorizontalScrollbar(canvas)
            }
        }
    }

    private fun drawVerticalScrollbar(canvas: Canvas) {
        val viewWidth = mRecyclerViewWidth

        val left = viewWidth - mVerticalThumbWidth
        val top = mVerticalThumbCenterY - mVerticalThumbHeight / 2
        mVerticalThumbDrawable.setBounds(0, 0, mVerticalThumbWidth, mVerticalThumbHeight)
        verticalTrackDrawable
            .setBounds(0, 0, mVerticalTrackWidth, mRecyclerViewHeight)

        if (isLayoutRTL) {
            verticalTrackDrawable.draw(canvas)
            canvas.translate(mVerticalThumbWidth.toFloat(), top.toFloat())
            canvas.scale(-1f, 1f)
            mVerticalThumbDrawable.draw(canvas)
            canvas.scale(-1f, 1f)
            canvas.translate(-mVerticalThumbWidth.toFloat(), -top.toFloat())
        } else {
            canvas.translate(left.toFloat(), 0f)
            verticalTrackDrawable.draw(canvas)
            canvas.translate(0f, top.toFloat())
            mVerticalThumbDrawable.draw(canvas)
            canvas.translate(-left.toFloat(), -top.toFloat())
        }
    }

    private fun drawHorizontalScrollbar(canvas: Canvas) {
        val viewHeight = mRecyclerViewHeight

        val top = viewHeight - mHorizontalThumbHeight
        val left = mHorizontalThumbCenterX - mHorizontalThumbWidth / 2
        mHorizontalThumbDrawable.setBounds(0, 0, mHorizontalThumbWidth, mHorizontalThumbHeight)
        horizontalTrackDrawable
            .setBounds(0, 0, mRecyclerViewWidth, mHorizontalTrackHeight)

        canvas.translate(0f, top.toFloat())
        horizontalTrackDrawable.draw(canvas)
        canvas.translate(left.toFloat(), 0f)
        mHorizontalThumbDrawable.draw(canvas)
        canvas.translate(-left.toFloat(), -top.toFloat())
    }

    /**
     * Notify the scroller of external change of the scroll, e.g. through dragging or flinging on
     * the view itself.
     *
     * @param offsetX The new scroll X offset.
     * @param offsetY The new scroll Y offset.
     */
    fun updateScrollPosition(offsetX: Int, offsetY: Int) {
        val verticalContentLength = mRecyclerView.computeVerticalScrollRange()
        val verticalVisibleLength = mRecyclerViewHeight
        mNeedVerticalScrollbar = verticalContentLength - verticalVisibleLength > 0
                && mRecyclerViewHeight >= mScrollbarMinimumRange

        val horizontalContentLength = mRecyclerView.computeHorizontalScrollRange()
        val horizontalVisibleLength = mRecyclerViewWidth
        mNeedHorizontalScrollbar = horizontalContentLength - horizontalVisibleLength > 0
                && mRecyclerViewWidth >= mScrollbarMinimumRange

        if (!mNeedVerticalScrollbar && !mNeedHorizontalScrollbar) {
            if (mState != STATE_HIDDEN) {
                setState(STATE_HIDDEN)
            }
            return
        }

        if (mNeedVerticalScrollbar) {
            val middleScreenPos = offsetY + verticalVisibleLength / 2.0f
            mVerticalThumbCenterY =
                ((verticalVisibleLength * middleScreenPos) / verticalContentLength.toFloat()).toInt()
            mVerticalThumbHeight = min(
                verticalVisibleLength.toDouble(),
                ((verticalVisibleLength * verticalVisibleLength) / verticalContentLength.toFloat()).toDouble()
            )
                .toInt()
        }

        if (mNeedHorizontalScrollbar) {
            val middleScreenPos = offsetX + horizontalVisibleLength / 2.0f
            mHorizontalThumbCenterX =
                ((horizontalVisibleLength * middleScreenPos) / horizontalContentLength).toInt()
            mHorizontalThumbWidth = min(
                horizontalVisibleLength.toDouble(),
                ((horizontalVisibleLength * horizontalVisibleLength) / horizontalContentLength).toDouble()
            )
                .toInt()
        }

        if (mState == STATE_HIDDEN || mState == STATE_VISIBLE) {
            setState(STATE_VISIBLE)
        }
    }

    override fun onInterceptTouchEvent(
        recyclerView: RecyclerView,
        ev: MotionEvent
    ): Boolean {
        val handled: Boolean
        if (mState == STATE_VISIBLE) {
            val insideVerticalThumb = isPointInsideVerticalThumb(ev.x, ev.y)
            val insideHorizontalThumb = isPointInsideHorizontalThumb(ev.x, ev.y)
            if (ev.action == MotionEvent.ACTION_DOWN
                && (insideVerticalThumb || insideHorizontalThumb)
            ) {
                if (insideHorizontalThumb) {
                    mDragState = DRAG_X
                    mHorizontalDragX = ev.x.toInt().toFloat()
                } else if (insideVerticalThumb) {
                    mDragState = DRAG_Y
                    mVerticalDragY = ev.y.toInt().toFloat()
                }

                setState(STATE_DRAGGING)
                handled = true
            } else {
                handled = false
            }
        } else if (mState == STATE_DRAGGING) {
            handled = true
        } else {
            handled = false
        }
        return handled
    }

    override fun onTouchEvent(recyclerView: RecyclerView, me: MotionEvent) {
        if (mState == STATE_HIDDEN) {
            return
        }

        if (me.action == MotionEvent.ACTION_DOWN) {
            val insideVerticalThumb = isPointInsideVerticalThumb(me.x, me.y)
            val insideHorizontalThumb = isPointInsideHorizontalThumb(me.x, me.y)
            if (insideVerticalThumb || insideHorizontalThumb) {
                if (insideHorizontalThumb) {
                    mDragState = DRAG_X
                    mHorizontalDragX = me.x.toInt().toFloat()
                } else if (insideVerticalThumb) {
                    mDragState = DRAG_Y
                    mVerticalDragY = me.y.toInt().toFloat()
                }
                setState(STATE_DRAGGING)
            }
        } else if (me.action == MotionEvent.ACTION_UP && mState == STATE_DRAGGING) {
            mVerticalDragY = 0f
            mHorizontalDragX = 0f
            setState(STATE_VISIBLE)
            mDragState = DRAG_NONE
        } else if (me.action == MotionEvent.ACTION_MOVE && mState == STATE_DRAGGING) {
            show()
            if (mDragState == DRAG_X) {
                horizontalScrollTo(me.x)
            }
            if (mDragState == DRAG_Y) {
                verticalScrollTo(me.y)
            }
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}

    private fun verticalScrollTo(y: Float) {
        var y = y
        val scrollbarRange = verticalRange
        y = max(scrollbarRange[0].toDouble(), min(scrollbarRange[1].toDouble(), y.toDouble()))
            .toFloat()
        if (abs((mVerticalThumbCenterY - y).toDouble()) < 2) {
            return
        }
        val scrollingBy = scrollTo(
            mVerticalDragY, y, scrollbarRange,
            mRecyclerView.computeVerticalScrollRange(),
            mRecyclerView.computeVerticalScrollOffset(), mRecyclerViewHeight
        )
        if (scrollingBy != 0) {
            mRecyclerView.scrollBy(0, scrollingBy)
        }
        mVerticalDragY = y
    }

    private fun horizontalScrollTo(x: Float) {
        var x = x
        val scrollbarRange = horizontalRange
        x = max(scrollbarRange[0].toDouble(), min(scrollbarRange[1].toDouble(), x.toDouble()))
            .toFloat()
        if (abs((mHorizontalThumbCenterX - x).toDouble()) < 2) {
            return
        }

        val scrollingBy = scrollTo(
            mHorizontalDragX, x, scrollbarRange,
            mRecyclerView.computeHorizontalScrollRange(),
            mRecyclerView.computeHorizontalScrollOffset(), mRecyclerViewWidth
        )
        if (scrollingBy != 0) {
            mRecyclerView.scrollBy(scrollingBy, 0)
        }

        mHorizontalDragX = x
    }

    private fun scrollTo(
        oldDragPos: Float, newDragPos: Float, scrollbarRange: IntArray, scrollRange: Int,
        scrollOffset: Int, viewLength: Int
    ): Int {
        val scrollbarLength = scrollbarRange[1] - scrollbarRange[0]
        if (scrollbarLength == 0) {
            return 0
        }
        val percentage = ((newDragPos - oldDragPos) / scrollbarLength.toFloat())
        val totalPossibleOffset = scrollRange - viewLength
        val scrollingBy = (percentage * totalPossibleOffset).toInt()
        val absoluteOffset = scrollOffset + scrollingBy
        return if (absoluteOffset < totalPossibleOffset && absoluteOffset >= 0) {
            scrollingBy
        } else {
            0
        }
    }

    
    fun isPointInsideVerticalThumb(x: Float, y: Float): Boolean {
        return (if (isLayoutRTL)
            x <= mVerticalThumbWidth
        else
            x >= mRecyclerViewWidth - mVerticalThumbWidth)
                && y >= mVerticalThumbCenterY - mVerticalThumbHeight / 2 && y <= mVerticalThumbCenterY + mVerticalThumbHeight / 2
    }

    
    fun isPointInsideHorizontalThumb(x: Float, y: Float): Boolean {
        return (y >= mRecyclerViewHeight - mHorizontalThumbHeight)
                && x >= mHorizontalThumbCenterX - mHorizontalThumbWidth / 2 && x <= mHorizontalThumbCenterX + mHorizontalThumbWidth / 2
    }

    val horizontalThumbDrawable: Drawable
        get() = mHorizontalThumbDrawable
    val verticalThumbDrawable: Drawable
        get() = mVerticalThumbDrawable

    private val verticalRange: IntArray
        /**
         * Gets the (min, max) vertical positions of the vertical scroll bar.
         */
        get() {
            mVerticalRange[0] = mMargin
            mVerticalRange[1] = mRecyclerViewHeight - mMargin
            return mVerticalRange
        }

    private val horizontalRange: IntArray
        /**
         * Gets the (min, max) horizontal positions of the horizontal scroll bar.
         */
        get() {
            mHorizontalRange[0] = mMargin
            mHorizontalRange[1] = mRecyclerViewWidth - mMargin
            return mHorizontalRange
        }

    private inner class AnimatorListener : AnimatorListenerAdapter() {
        private var mCanceled = false

        override fun onAnimationEnd(animation: Animator) {
            // Cancel is always followed by a new directive, so don't update state.
            if (mCanceled) {
                mCanceled = false
                return
            }
            if (mShowHideAnimator.animatedValue as Float == 0f) {
                mAnimationState = ANIMATION_STATE_OUT
                setState(STATE_HIDDEN)
            } else {
                mAnimationState = ANIMATION_STATE_IN
                requestRedraw()
            }
        }

        override fun onAnimationCancel(animation: Animator) {
            mCanceled = true
        }
    }

    private inner class AnimatorUpdater : ValueAnimator.AnimatorUpdateListener {
        override fun onAnimationUpdate(valueAnimator: ValueAnimator) {
            val alpha: Int =
                (SCROLLBAR_FULL_OPAQUE * (valueAnimator.animatedValue as Float)).toInt()
            mVerticalThumbDrawable.alpha = alpha
            verticalTrackDrawable.alpha = alpha
            requestRedraw()
        }
    }

    companion object {
        // Scroll thumb not showing
        private const val STATE_HIDDEN = 0

        // Scroll thumb visible and moving along with the scrollbar
        private const val STATE_VISIBLE = 1

        // Scroll thumb being dragged by user
        private const val STATE_DRAGGING = 2

        private const val DRAG_NONE = 0
        private const val DRAG_X = 1
        private const val DRAG_Y = 2

        private const val ANIMATION_STATE_OUT = 0
        private const val ANIMATION_STATE_FADING_IN = 1
        private const val ANIMATION_STATE_IN = 2
        private const val ANIMATION_STATE_FADING_OUT = 3

        private const val SHOW_DURATION_MS = 500
        private const val HIDE_DELAY_AFTER_VISIBLE_MS = 1500
        private const val HIDE_DELAY_AFTER_DRAGGING_MS = 1200
        private const val HIDE_DURATION_MS = 500
        private const val SCROLLBAR_FULL_OPAQUE = 255

        private val PRESSED_STATE_SET = intArrayOf(android.R.attr.state_pressed)
        private val EMPTY_STATE_SET = intArrayOf()
    }
}