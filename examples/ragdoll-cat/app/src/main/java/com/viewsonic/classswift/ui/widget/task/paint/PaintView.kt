package com.viewsonic.classswift.ui.widget.task.paint

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.viewsonic.classswift.R
import com.viewsonic.classswift.utils.extension.dpToPx
import timber.log.Timber

class PaintView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class DrawPath(val path: Path, val paint: Paint)

    private var dotX = 0f
    private var dotY = 0f

    private var drawingBitmap: Bitmap? = null
    private var drawingCanvas: Canvas? = null

    private val drawPaths = mutableListOf<DrawPath>()
    private val redoPaths = mutableListOf<DrawPath>()

    private var currentPath = Path()
    private var currentPaint = createPaint(
        ContextCompat.getColor(context, R.color.paint_color_red)
    )

    private var isEraserMode = false
    private var isShowDot = false

    private val dotFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val dotStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 1.33f
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }

    private val radius = 24f.dpToPx() / 2


    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onSizeChanged(
        width: Int,
        height: Int,
        oldWidth: Int,
        oldHeight: Int
    ) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (drawingBitmap == null) {
            drawingBitmap = createBitmap(width, height)
        }

        drawingBitmap?.let {
            drawingCanvas = Canvas(it)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw existing drawing layer
        drawingBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }

        // Draw path in progress
        canvas.drawPath(currentPath, currentPaint)

        // Show draw dot (eraser mode only)
        if (isEraserMode && isShowDot) {
            canvas.let {
                it.drawCircle(dotX, dotY, radius, dotFill)
                it.drawCircle(dotX, dotY, radius, dotStroke)
            }
        }
    }

    fun setBrushColor(color: Int) {
        isEraserMode = false
        val newColor = ContextCompat.getColor(context, color)
        currentPaint = createPaint(newColor)
    }

    fun setEraserMode() {
        isEraserMode = true
        currentPaint = createEraserPaint()
    }

    fun undo() {
        if (drawPaths.isNotEmpty()) {
            val last = drawPaths.removeAt(drawPaths.lastIndex)
            redoPaths.add(last)
            redrawPaths()
        }
    }

    fun redo() {
        if (redoPaths.isNotEmpty()) {
            val last = redoPaths.removeAt(redoPaths.lastIndex)
            drawPaths.add(last)
            redrawPaths()
        }
    }

    fun undoAll() {
        if (drawPaths.isNotEmpty()) {
            redoPaths.addAll(drawPaths)
            drawPaths.clear()
            redrawPaths()
        }
    }

    fun exportToBitmap(): Bitmap {
        val result = createBitmap(width, height)
        val canvas = Canvas(result)

        drawingBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        return result
    }

    private fun redrawPaths() {
        drawingBitmap?.eraseColor(Color.TRANSPARENT)
        for (dp in drawPaths) {
            drawingCanvas?.drawPath(dp.path, dp.paint)
        }
        invalidate()
    }

    private fun createPaint(color: Int): Paint {
        return Paint().apply {
            isAntiAlias = true
            isDither = true
            this.color = color
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 8f
        }
    }

    private fun createEraserPaint(): Paint {
        return Paint().apply {
            isAntiAlias = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 48f
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        Timber.d("onTouchEvent: $event")
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isShowDot = true
                dotX = x
                dotY = y
                currentPath = Path()
                currentPath.moveTo(x, y)
            }

            MotionEvent.ACTION_MOVE -> {
                dotX = x
                dotY = y
                currentPath.lineTo(x, y)
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                Timber.d("drawingCanvas: $drawingCanvas , drawingBitmap : $drawingBitmap")
                isShowDot = false
                currentPath.lineTo(x, y)
                drawingCanvas?.drawPath(currentPath, currentPaint)
                drawPaths.add(DrawPath(Path(currentPath), Paint(currentPaint)))
                currentPath.reset()
                redoPaths.clear()
                invalidate()
            }
        }
        return true
    }

    /**
     * Clear the drawing content while keeping the existing bitmap and canvas.
     */
    fun clearCanvas() {
        drawingBitmap?.eraseColor(Color.TRANSPARENT)
        drawPaths.clear()
        redoPaths.clear()
        invalidate()
    }

    /**
     * Release resources (only call when the View is no longer in use)
     */
    fun release() {
        drawingBitmap?.recycle()
        drawingBitmap = null
        drawingCanvas = null
        drawPaths.clear()
        redoPaths.clear()
    }
}