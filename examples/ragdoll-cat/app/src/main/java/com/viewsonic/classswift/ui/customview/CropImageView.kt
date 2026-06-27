package com.viewsonic.classswift.ui.customview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.toBitmap
import com.viewsonic.classswift.R
import timber.log.Timber
import kotlin.math.roundToInt

class CropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {


    interface OnCropRangeListener {
        fun cropRange(x: Int, y: Int, width: Int, height: Int)
    }

    private val paint = Paint().apply {
        color = resources.getColor(R.color.window_crop_image_default_paint)
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val maskPaint = Paint().apply {
        color = resources.getColor(R.color.window_crop_image_mask)
        style = Paint.Style.FILL
    }

    private val transparentPaint = Paint().apply {
        color = resources.getColor(R.color.transparent_color)
        style = Paint.Style.FILL
    }

    private var selectionRect = RectF()
    private var rawSelectionRect = RectF()
    private var startX = 0f
    private var startY = 0f
    private var isDragging = false
    private var isProcessing = false
    private var showSelection = false
    private var onCropRangeListener: OnCropRangeListener? = null
    private var rawStartX = 0f
    private var rawStartY = 0f
    private var hasMask = true

    var onCropEvent: ((MotionEvent) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (drawable == null && w > 0 && h > 0) {
            setImageBitmap(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888))
        }
    }

    private fun getImageBounds(): RectF {
        val bounds = RectF()
        drawable?.let { drawable ->
            bounds.set(0f, 0f, getRealImageWidth().toFloat(), getRealImageHeight().toFloat())
            imageMatrix.mapRect(bounds)
        }
        return bounds
    }

    private fun getImageBoundsOnScreen(): RectF {
        val bounds = RectF()
        drawable?.let { drawable ->
            // 計算圖片在 View 內的範圍
            bounds.set(0f, 0f, getRealImageWidth().toFloat(), getRealImageHeight().toFloat())
            imageMatrix.mapRect(bounds)

            // 取得 ImageView 在螢幕上的位置
            val location = IntArray(2)
            getLocationOnScreen(location)

            // 將 View 內的座標轉換成螢幕座標
            bounds.offset(location[0].toFloat(), location[1].toFloat())
        }
        return bounds
    }

    fun setOnCropRangeListener(listener: OnCropRangeListener?) {
        this.onCropRangeListener = listener
    }


    private fun selectedArea() {
        if (drawable == null || isProcessing) return

        try {
            isProcessing = true
            Timber.d("[**][ClassSwiftCropImageView][111] raw selection: x-${rawSelectionRect.left}, y-${rawSelectionRect.top} ,width-${rawSelectionRect.width()}, height-${rawSelectionRect.height()}")
            // 回傳 **螢幕選取範圍**
            onCropRangeListener?.cropRange(rawSelectionRect.left.roundToInt(), rawSelectionRect.top.roundToInt(), rawSelectionRect.width().roundToInt(), rawSelectionRect.height().roundToInt())
        } catch (e: Exception) {
            Timber.e("Crop failed", e)
            e.printStackTrace()
        } finally {
            isProcessing = false
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isProcessing) return false
        onCropEvent?.invoke(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                selectionRect.set(startX, startY, startX, startY)
                rawStartX = event.rawX
                rawStartY = event.rawY
                //螢幕座標 (Screen Coordinates)	以 螢幕 (含 StatusBar & NavigationBar) 左上角為 (0,0)
                rawSelectionRect.set(rawStartX, rawStartY, rawStartX, rawStartY)
                Timber.d("[**][ClassSwiftCropImageView][191] ACTION_DOWN: xy: $startX , $startY , rawXY: $rawStartX , $rawStartY")
                isDragging = true
                showSelection = true
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    selectionRect.right = event.x
                    selectionRect.bottom = event.y
                    rawSelectionRect.right = event.rawX
                    rawSelectionRect.bottom = event.rawY
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    normalizeRect()
                    hasMask = false
                    showSelection = false
                    invalidate()
                    post {
                        selectedArea()
                    }
                }
            }
        }
        return true
    }

    private fun normalizeRect() {
        if (selectionRect.right < selectionRect.left) {
            val temp = selectionRect.right
            selectionRect.right = selectionRect.left
            selectionRect.left = temp
        }
        if (selectionRect.bottom < selectionRect.top) {
            val temp = selectionRect.bottom
            selectionRect.bottom = selectionRect.top
            selectionRect.top = temp
        }

        val imageBounds = getImageBounds()
        selectionRect.intersect(imageBounds)

        if (rawSelectionRect.right < rawSelectionRect.left) {
            val temp = rawSelectionRect.right
            rawSelectionRect.right = rawSelectionRect.left
            rawSelectionRect.left = temp
        }
        if (rawSelectionRect.bottom < rawSelectionRect.top) {
            val temp = rawSelectionRect.bottom
            rawSelectionRect.bottom = rawSelectionRect.top
            rawSelectionRect.top = temp
        }

        val rewImageBounds = getImageBoundsOnScreen()
        rawSelectionRect.intersect(rewImageBounds)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val imageBounds = getImageBounds()
//        //畫圖片外的區塊，如果圖片範圍外不需要mask,才需要
        canvas.drawRect(0f, 0f, width.toFloat(), imageBounds.top, maskPaint)
        canvas.drawRect(0f, imageBounds.bottom, width.toFloat(), height.toFloat(), maskPaint)
        canvas.drawRect(0f, imageBounds.top, imageBounds.left, imageBounds.bottom, maskPaint)
        canvas.drawRect(imageBounds.right, imageBounds.top, width.toFloat(), imageBounds.bottom, maskPaint)


        if (!showSelection) {
            canvas.drawRect(imageBounds, if (hasMask) maskPaint else transparentPaint)
        } else {
            var selectionLeft = selectionRect.left
            var selectionTop = selectionRect.top
            var selectionRight = selectionRect.right
            var selectionBottom = selectionRect.bottom
            //計算起始座標，如果如果左大於右，代表手勢是反向操作，座標互換，才不會造成 mask 畫錯問題
            if (selectionRect.left > selectionRect.right) {
                selectionLeft = selectionRect.right
                selectionRight = selectionRect.left
            }
            if (selectionRect.top > selectionRect.bottom) {
                selectionTop = selectionRect.bottom
                selectionBottom = selectionRect.top
            }
            //畫截圖外的區塊
            canvas.drawRect(
                imageBounds.left,
                imageBounds.top,
                selectionLeft,
                imageBounds.bottom,
                maskPaint
            )
            canvas.drawRect(
                selectionRight,
                imageBounds.top,
                imageBounds.right,
                imageBounds.bottom,
                maskPaint
            )
            canvas.drawRect(selectionLeft, imageBounds.top, selectionRight, selectionTop, maskPaint)
            canvas.drawRect(
                selectionLeft,
                selectionBottom,
                selectionRight,
                imageBounds.bottom,
                maskPaint
            )
            canvas.drawRect(selectionRect, paint)
        }
    }

    fun reset() {
        selectionRect = RectF()
        rawSelectionRect = RectF()
        hasMask = true
    }

    fun getSelectionRect(): RectF = RectF(selectionRect)

    fun setSelectionStyle(
        color: Int = resources.getColor(R.color.window_crop_image_default_paint),
        strokeWidth: Float = 4f,
        isDashed: Boolean = false,
        dashWidth: Float = 10f,
        dashGap: Float = 10f
    ) {
        reset()
        paint.apply {
            this.color = color
            this.strokeWidth = strokeWidth
            pathEffect = if (isDashed) {
                DashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
            } else {
                null
            }
        }
        invalidate()
    }

    fun setMaskColor(color: Int) {
        maskPaint.color = color
        invalidate()
    }


    fun resetSelection() {
        showSelection = false
        selectionRect.setEmpty()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        drawable?.toBitmap().let {
            if (it is Bitmap && it.isRecycled.not()) {
                it.recycle()
            }
        }
    }

    private fun getRealImageWidth(): Int {
        val image = drawable
        return image?.intrinsicWidth ?: -1
    }

    private fun getRealImageHeight(): Int {
        val image = drawable
        return image?.intrinsicHeight ?: -1
    }

    fun getImageWidth(): Int {
        return getImageBounds().width().toInt()
    }

    fun getImageHeight(): Int {
        return getImageBounds().height().toInt()
    }

}
