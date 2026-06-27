package com.viewsonic.classswift.windowframework.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.viewbinding.ViewBinding
import com.viewsonic.classswift.R
import com.viewsonic.classswift.databinding.WindowTempViewBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.windowframework.core.data.Config
import com.viewsonic.classswift.windowframework.core.data.Location
import com.viewsonic.classswift.windowframework.core.data.RelatedPosition
import com.viewsonic.classswift.windowframework.core.data.SizeInPixels
import com.viewsonic.classswift.windowframework.core.enums.ViewState
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.enums.WindowTag.Companion.getFirstLevelWindowTagList
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowContainer
import com.viewsonic.classswift.windowframework.core.interfaces.listener.OnMotionEventChangedListener
import com.viewsonic.classswift.windowframework.core.interfaces.listener.OnViewPositionChangedListener
import com.viewsonic.classswift.windowframework.core.utils.LocationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.reflect.Field
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import com.viewsonic.classswift.utils.extension.awaitGoneOrFullyClipped
import com.viewsonic.classswift.utils.extension.awaitVisible

class WindowContainer(
    private val windowManager: WindowManager,
    private val layoutInflater: LayoutInflater,
    private val csWindowManager: CSWindowManager
): IWindowContainer, OnViewPositionChangedListener {

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    @Volatile
    var config: Config = Config()

    // region Main Window UI component
    override lateinit var floatWindowLayoutParam: WindowManager.LayoutParams
    override lateinit var floatingView: ViewGroup
    private var layoutType: Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    override lateinit var customWindow: IWindow<ViewBinding>
    // endregion

    // region 用來提升 z-order 的 UI component
    private lateinit var preZOrderViewBinding: WindowTempViewBinding
    private lateinit var preZOrderWindowLayoutParam: WindowManager.LayoutParams
    // endregion

    private var onMotionEventChangedListener: OnMotionEventChangedListener? = null
    private val onViewPositionChangedListeners = CopyOnWriteArrayList<OnViewPositionChangedListener>()

    private var isHoisting: AtomicBoolean = AtomicBoolean(false)

    /**
     * For normal-window windowContainer
     */
    fun initParams(
        window: IWindow<ViewBinding>,
        location: Location,
        isOutOfScreen: Boolean,
        isDraggable: Boolean = true
    ) {
        this.customWindow = window
        this.config = config.copy(
            isDraggable = isDraggable
        )
        onCreate()
        initFloatingView(window, location, isOutOfScreen)

        config.apply {
            this.tag = window.tag
            sizeInPixels = window.getCurrentSize()
            this.location = location
            isInitialized = true
        }

        setNormalWindowOnTouchListener()
        onViewCreated()
    }

    /**
     * For sub-window windowContainer
     */
    fun initSubWindowParams(
        window: IWindow<ViewBinding>,
        location: Location,
        mainWindowTag: WindowTag,
        relatedPosition: RelatedPosition
    ) {
        this.customWindow = window
        onCreate()
        initFloatingView(window, location, true)

        config.apply {
            this.tag = window.tag
            sizeInPixels = window.getCurrentSize()
            this.location = location
            isInitialized = true
            isSubWindow = true
            this.mainWindowTag = mainWindowTag
            this.relatedPosition = relatedPosition
        }

        setSubWindowOnTouchListener()
        onViewCreated()
    }

    private fun initFloatingView(
        window: IWindow<ViewBinding>,
        location: Location,
        isOutOfScreen: Boolean
    ) {
        floatingView = layoutInflater.inflate(R.layout.window_empty_view, null) as ViewGroup

        val layoutFlags = if (isOutOfScreen) {
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        this.floatWindowLayoutParam = WindowManager.LayoutParams(
            window.size.width,
            window.size.height,
            layoutType,
            layoutFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = location.coordinateX
            y = location.coordinateY
        }

        val className = "android.view.WindowManager\$LayoutParams"
        try {
            val layoutParamsClass = Class.forName(className)

            val privateFlags: Field = layoutParamsClass.getField("privateFlags")
            val noAnim: Field = layoutParamsClass.getField("PRIVATE_FLAG_NO_MOVE_ANIMATION")

            var privateFlagsValue: Int = privateFlags.getInt(floatWindowLayoutParam)
            val noAnimFlag: Int = noAnim.getInt(floatWindowLayoutParam)
            privateFlagsValue = privateFlagsValue or noAnimFlag

            privateFlags.setInt(floatWindowLayoutParam, privateFlagsValue)

            // Dynamically do stuff with this class
            // List constructors, fields, methods, etc.
        } catch (e: ClassNotFoundException) {
            Timber.d("[initFloatingView] : e: $e")
            // Class not found!
        } catch (e: Exception) {
            Timber.d("[initFloatingView] : e: $e")
            // Unknown exception
        }
        floatingView.addView(this.customWindow.getRootView())
    }

    override fun getLayoutParam(): WindowManager.LayoutParams {
        return floatWindowLayoutParam
    }

    override fun updateLayoutParam(windowLayoutParams: WindowManager.LayoutParams) {
        if (!config.isRemoved) {
            this.floatWindowLayoutParam = windowLayoutParams

            // For 監聽 floatingView Layout 的變化，並即時更新 config 中的 sizeInPixels 參數
            // ，來避免 WindowOverlayUtil 計算面積時未拿到正確的 window size
            // ，更新 config 後即移除 onGlobalLayoutListener
            val onGlobalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val newWidth = floatingView.width
                    val newHeight = floatingView.height
                    val originalSize = config.sizeInPixels
                    if (originalSize.width != newWidth || originalSize.height != newHeight) {
                        Timber.d("[updateLayoutParam]: ${config.tag} - newWidth: $newWidth, newHeight: $newHeight")
                        config.apply {
                            sizeInPixels = SizeInPixels(newWidth, newHeight)
                        }
                        customWindow.onViewSizeChanged(originalSize, config.sizeInPixels)
                    }
                    Timber.d("[updateLayoutParam]: remove onGlobalLayoutListener")
                    floatingView.rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }

            Timber.d("[updateLayoutParam]: add onGlobalLayoutListener")
            floatingView.rootView.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
            windowManager.updateViewLayout(floatingView, floatWindowLayoutParam)

        }
    }

    override fun getWindowConfig(): Config {
        return config
    }

    override fun setVisibility(viewState: ViewState) {
        config.viewState = viewState

        when (viewState) {
            ViewState.VISIBLE -> {
                floatingView.visibility = View.VISIBLE
            }
            ViewState.INVISIBLE -> {
                floatingView.visibility = View.INVISIBLE
            }
            ViewState.GONE -> {
                floatingView.visibility = View.GONE
            }
        }
    }

    // region TouchListeners and other callback listeners
    override fun setOnMotionEventChangedListener(onMotionEventChangedListener: OnMotionEventChangedListener) {
        this.onMotionEventChangedListener = onMotionEventChangedListener
    }

    override fun addOnViewPositionChangedListener(listener: OnViewPositionChangedListener) {
        onViewPositionChangedListeners.addIfAbsent(listener)
    }

    override fun removeOnViewPositionChangedListener(listener: OnViewPositionChangedListener) {
        onViewPositionChangedListeners.remove(listener)
    }

    // From MainWindow if this window is subWindow.
    override fun onViewPositionChanged(coordinateX: Int, coordinateY: Int, anchorXOffset: Int) {
        Timber.d("[B][onViewPositionChanged] : ${config.tag}")
        // Timber.d("receiveLocation: x-${locationAnchorXOffset.location.coordinateX}, y-${locationAnchorXOffset.location.coordinateY}")
        // 計算 sub-window 的新位置
        val newAnchorX = coordinateX + anchorXOffset
        val newSubWindowLocation = LocationUtil.calculateSubWindowLocation(
            config.sizeInPixels, config.mainWindowTag, newAnchorX, config.relatedPosition)
        // 更新 layoutParam
        val subWindowLayoutParams = getLayoutParam()
        subWindowLayoutParams.x = newSubWindowLocation.coordinateX
        subWindowLayoutParams.y = newSubWindowLocation.coordinateY
        updateLayoutParam(subWindowLayoutParams)
    }

    fun addWindow(): Boolean {
        if (config.isInitialized) {
            windowManager.addView(floatingView, floatWindowLayoutParam)
            return true
        }
        return false
    }

    fun removeWindow() {
        config.isRemoved = true
        onDestroy()
        windowManager.removeView(floatingView)
    }

    /**
     * 一般 window onTouchListener: 移動 & z-order 提升方法
     */
    private fun setNormalWindowOnTouchListener() {
        if (!config.isDraggable) {
            return
        }

        floatingView.setOnTouchListener(object : OnTouchListener {
            var previousEventAction: Int = MotionEvent.INVALID_POINTER_ID

            val floatWindowLayoutUpdateParam: WindowManager.LayoutParams = floatWindowLayoutParam
            var x: Double = 0.0
            var y: Double = 0.0
            var px: Double = 0.0
            var py: Double = 0.0

            override fun onTouch(v: View, event: MotionEvent): Boolean {

                // for Toolbar 變更顯示的做法
                // 傳入 event.action，讓 listener 內部決定該 action 對應的行為
                if (previousEventAction != event.action) {
                    Timber.d("onTouch: onMotionEventChanged - ${event.action}")
                    onMotionEventChangedListener?.onMotionEventChanged(previousEventAction, event.action)
                    previousEventAction = event.action
                }

                // for 移動視窗 & 提升視窗 z-order 的做法
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        x = config.location.coordinateX.toDouble()
                        y = config.location.coordinateY.toDouble()

                        // returns the original raw X coordinate of this event
                        px = event.rawX.toDouble()
                        // returns the original raw Y coordinate of this event
                        py = event.rawY.toDouble()
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val pointerOffsetX = event.rawX - px
                        val pointerOffsetY = event.rawY - py

                        floatWindowLayoutUpdateParam.x = (x + pointerOffsetX).toInt()
                        floatWindowLayoutUpdateParam.y = (y + pointerOffsetY).toInt()

                        config.location = Location(floatWindowLayoutUpdateParam.x, floatWindowLayoutUpdateParam.y)
                        // updated parameter is applied to the WindowManager
                        windowManager.updateViewLayout(floatingView, floatWindowLayoutUpdateParam)

                        // 傳送 main-window 位置資訊給 sub-window
                        onViewPositionChangedListeners.forEach {
                            it.onViewPositionChanged(config.location.coordinateX, config.location.coordinateY, config.anchorXOffset)
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        coroutineScope.launch(Dispatchers.Main) { hoistWindowZOrder() }
                    }
                }

                return true
            }
        })
    }

    /**
     * subWindow onTouchListener: z-order 提升方法 (subWindow 無法自行移動)
     */
    private fun setSubWindowOnTouchListener() {
        floatingView.setOnTouchListener(object : OnTouchListener {
            var previousEventAction: Int = MotionEvent.INVALID_POINTER_ID

            override fun onTouch(v: View, event: MotionEvent): Boolean {

                // for Toolbar 變更顯示的做法
                // 傳入 event.action，讓 listener 內部決定該 action 對應的行為
                if (previousEventAction != event.action) {
                    Timber.d("onTouch: onMotionEventChanged - ${event.action}")
                    onMotionEventChangedListener?.onMotionEventChanged(previousEventAction, event.action)
                    previousEventAction = event.action
                }

                // for 提升視窗 z-order 的做法
                when (event.action) {
                    MotionEvent.ACTION_UP -> {
                        coroutineScope.launch(Dispatchers.Main) {hoistWindowZOrder() }
                    }
                }

                return true
            }
        })
    }
    // endregion

    // region Window Lifecycle
    override fun onCreate() {
        Timber.d("${config.tag} - onCreate")
        this.customWindow.onCreate()
    }

    override fun onViewCreated() {
        Timber.d("${config.tag} - onViewCreated")
        this.customWindow.onViewCreated()
    }

    override fun onDestroy() {
        Timber.d("${config.tag} - onDestroy")
        this.customWindow.onDestroy()
        onMotionEventChangedListener = null
        onViewPositionChangedListeners.clear()
    }
    // endregion

    // region Hoisting Window z-order
    override suspend fun hoistWindowZOrder() {
        val topWindowTag = csWindowManager.getTopWindowTag()
        Timber.d("theLatestAddedWindow - $topWindowTag")

        if (topWindowTag != WindowTag.NONE && config.tag != topWindowTag && !isHoisting.get()) {
            isHoisting.compareAndSet(false, true)
            Timber.d("hoistWindowZOrder")
            if (config.tag == WindowTag.TOOLBAR) {
                hoistSubWindowZOrder()
            }
            if (!getFirstLevelWindowTagList().contains(config.tag) && csWindowManager.hasSystemWindowShow()) {
                executeWindowZOrderAdjustmentWithSystemDialog()
            } else {
                executeWindowZOrderAdjustment()
            }
        }
    }

    private fun hoistSubWindowZOrder() {
        val subWindowTagList = WindowTag.getSubWindowTagList()
        subWindowTagList.forEach { subWindowTag ->
            coroutineScope.launch { csWindowManager.getWindow(subWindowTag)?.hoistWindowZOrder() }
        }
    }

    private suspend fun executeWindowZOrderAdjustment() {
        withContext(Dispatchers.IO) {
            // 將目前 Window 的 View 擷取成圖檔，主要用來在調整 Window 順序過程中當前擋圖片，
            // 讓使用者不會感受到畫面切換的感覺。
            val bitmap: Bitmap = viewToImage(floatingView)
            try {
                // 初始化用來顯示前擋圖片的 View，需要將 bitmap size 設定上去，避免顯示大小不一導致閃爍。
                preZOrderViewBinding = WindowTempViewBinding.inflate(layoutInflater)
                preZOrderViewBinding.root.layoutParams = FrameLayout.LayoutParams(bitmap.width, bitmap.height)
                withContext(Dispatchers.Main) {
                    preZOrderViewBinding.tempView.setImageBitmap(bitmap)
                    // 將目前 Window 的 View 移除，補上前擋圖片的 View。
                    floatingView.addView(preZOrderViewBinding.root)
                    floatingView.removeView(customWindow.getRootView())
                }
                // 根據目前 Window 設定去建立新 Window，並加入到 WindowManager 以達成 Z Order 上移的效果
                preZOrderWindowLayoutParam = WindowManager.LayoutParams()
                preZOrderWindowLayoutParam.copyFrom(floatWindowLayoutParam)
                preZOrderWindowLayoutParam.width = customWindow.getCurrentSize().width
                preZOrderWindowLayoutParam.height = customWindow.getCurrentSize().height
                val newFloatingView = layoutInflater.inflate(R.layout.window_empty_view, null) as ViewGroup
                withContext(Dispatchers.Main) {
                    newFloatingView.addView(customWindow.getRootView())
                    windowManager.addView(newFloatingView, preZOrderWindowLayoutParam)
                    newFloatingView.awaitVisible()
                    preZOrderViewBinding.tempView.setImageBitmap(null)
                    if (config.isRemoved) {
                        // Window was removed during hoisting; clean up the newly added view to avoid a leak.
                        windowManager.removeView(newFloatingView)
                    } else {
                        windowManager.removeView(floatingView)
                        floatingView.awaitGoneOrFullyClipped()
                        floatingView = newFloatingView
                        if (config.isSubWindow) {
                            setSubWindowOnTouchListener()
                        } else {
                            setNormalWindowOnTouchListener()
                        }
                        refreshTopWindowTagInZOrderList(config.tag)
                    }
                }
            } finally {
                bitmap.recycle()
                isHoisting.compareAndSet(true, false)
            }
        }
    }

    private suspend fun executeWindowZOrderAdjustmentWithSystemDialog() {
        withContext(Dispatchers.IO) {
            // 將目前 Window 的 View 擷取成圖檔，主要用來在調整 Window 順序過程中當前擋圖片，
            // 讓使用者不會感受到畫面切換的感覺。
            val bitmap: Bitmap = viewToImage(floatingView)
            try {
                // 初始化用來顯示前擋圖片的 View，需要將 bitmap size 設定上去，避免顯示大小不一導致閃爍。
                preZOrderViewBinding = WindowTempViewBinding.inflate(layoutInflater)
                preZOrderViewBinding.root.layoutParams = FrameLayout.LayoutParams(bitmap.width, bitmap.height)
                withContext(Dispatchers.Main) {
                    preZOrderViewBinding.tempView.setImageBitmap(bitmap)
                    // 將目前 Window 的 View 移除，補上前擋圖片的 View。
                    floatingView.addView(preZOrderViewBinding.root)
                    floatingView.removeView(customWindow.getRootView())
                }
                // 根據目前 Window 設定去建立新 Window，並加入到 WindowManager 以達成 Z Order 上移的效果
                preZOrderWindowLayoutParam = WindowManager.LayoutParams()
                preZOrderWindowLayoutParam.copyFrom(floatWindowLayoutParam)
                preZOrderWindowLayoutParam.width = customWindow.getCurrentSize().width
                preZOrderWindowLayoutParam.height = customWindow.getCurrentSize().height
                val newFloatingView = layoutInflater.inflate(R.layout.window_empty_view, null) as ViewGroup
                withContext(Dispatchers.Main) {
                    newFloatingView.addView(customWindow.getRootView())
                    newFloatingView.visibility = View.INVISIBLE
                    windowManager.addView(newFloatingView, preZOrderWindowLayoutParam)
                    if (!config.isRemoved) {
                        refreshTopWindowTagInZOrderList(config.tag)
                    }
                }
                csWindowManager.setSystemDialogOnTop()
                // delay，for ensure system dialog bring to top is done。
                delay(200)
                withContext(Dispatchers.Main) {
                    if (config.isRemoved) {
                        // Window was removed during hoisting; clean up the newly added view to avoid a leak.
                        windowManager.removeView(newFloatingView)
                    } else {
                        newFloatingView.visibility = View.VISIBLE
                        preZOrderViewBinding.tempView.setImageBitmap(null)
                        windowManager.removeView(floatingView)
                        floatingView.awaitGoneOrFullyClipped()
                        floatingView = newFloatingView
                        if (config.isSubWindow) {
                            setSubWindowOnTouchListener()
                        } else {
                            setNormalWindowOnTouchListener()
                        }
                    }
                }
            } finally {
                bitmap.recycle()
                isHoisting.compareAndSet(true, false)
            }
        }
    }

    private suspend fun viewToImage(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        withContext(Dispatchers.Main) {
            val bgDrawable = view.background
            if (bgDrawable != null) {
                bgDrawable.draw(canvas)
            } else {
                canvas.drawColor(Color.TRANSPARENT)
            }
            view.draw(canvas)
        }
        return returnedBitmap
    }


    private fun refreshTopWindowTagInZOrderList(windowTag: WindowTag) {
        csWindowManager.refreshTopWindowTagInZOrderList(windowTag)
    }
    // endregion
}