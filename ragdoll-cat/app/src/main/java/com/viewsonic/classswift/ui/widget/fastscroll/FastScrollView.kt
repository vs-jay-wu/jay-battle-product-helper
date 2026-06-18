package com.viewsonic.classswift.ui.widget.fastscroll

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ScrollView
import androidx.annotation.AttrRes

@SuppressLint("MissingSuperCall")
class FastScrollView : ScrollView, ViewHelperProvider {
    private val mViewHelper: ViewHelper = ViewHelper()

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(
        context: Context, attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        isVerticalScrollBarEnabled = false
        isScrollContainer = true
    }

    override fun getViewHelper(): ViewHelper {
        return mViewHelper
    }

    override fun draw(canvas: Canvas) {
        mViewHelper.draw(canvas)
    }

    override fun onScrollChanged(left: Int, top: Int, oldLeft: Int, oldTop: Int) {
        mViewHelper.onScrollChanged(left, top, oldLeft, oldTop)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return mViewHelper.onInterceptTouchEvent(event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return mViewHelper.onTouchEvent(event)
    }

    inner class ViewHelper : SimpleViewHelper() {
        override fun getScrollRange(): Int {
            return super.getScrollRange() + paddingTop + paddingBottom
        }

        override fun superDraw(canvas: Canvas) {
            super@FastScrollView.draw(canvas)
        }

        override fun superOnScrollChanged(left: Int, top: Int, oldLeft: Int, oldTop: Int) {
            super@FastScrollView.onScrollChanged(left, top, oldLeft, oldTop)
        }

        override fun superOnInterceptTouchEvent(event: MotionEvent): Boolean {
            return super@FastScrollView.onInterceptTouchEvent(event)
        }

        override fun superOnTouchEvent(event: MotionEvent): Boolean {
            return super@FastScrollView.onTouchEvent(event)
        }

        override fun computeVerticalScrollRange(): Int {
            return this@FastScrollView.computeVerticalScrollRange()
        }

        override fun computeVerticalScrollOffset(): Int {
            return this@FastScrollView.computeVerticalScrollOffset()
        }

        override fun getScrollX(): Int {
            return this@FastScrollView.scrollX
        }

        override fun scrollTo(x: Int, y: Int) {
            this@FastScrollView.scrollTo(x, y)
        }
    }
}
