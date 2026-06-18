package com.viewsonic.classswift.windowframework.core

import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.viewbinding.ViewBinding
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.windowframework.core.data.Location
import com.viewsonic.classswift.windowframework.core.data.RelatedPosition
import com.viewsonic.classswift.windowframework.core.data.WindowPair
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.ViewState
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindow
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowContainer
import com.viewsonic.classswift.windowframework.core.interfaces.listener.OnCSWindowChangedListener
import com.viewsonic.classswift.windowframework.core.utils.LocationUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object CSWindowManager {
    private val windowMap = ConcurrentHashMap<WindowTag, IWindowContainer>()
    // Each time a new window is added, it is placed into this list.
    // The last added window has the top Z-order.
    private val windowZOrderList = mutableListOf<WindowTag>()
    private val windowHiddenSet = mutableSetOf<WindowTag>()
    private val windowMinimizedSet = mutableSetOf<WindowTag>()
    // Used to record dependent windowTags, e.g., toolbar and sub-toolbar
    private val windowPairList = mutableListOf<WindowPair>()

    private val windowManager: WindowManager by inject(WindowManager::class.java)
    private val layoutInflater: LayoutInflater by inject(LayoutInflater::class.java)
    private val onCSWindowChangedListeners = CopyOnWriteArrayList<OnCSWindowChangedListener>()
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private val bringToTopMutex = kotlinx.coroutines.sync.Mutex()

    /**
     * Used to clear the internal state of CSWindowManager.
     * Can be called after app startup and before the first floating window is opened,
     * to avoid inconsistent internal state caused by objects not being cleared by the system
     * after an unexpected app crash.
     */
    fun reset() {
        val iterator = windowMap.entries.iterator()
        while (iterator.hasNext()) {
            val (_, iWindowContainer) = iterator.next()
            if (iWindowContainer is WindowContainer) {
                iWindowContainer.removeWindow()
            }
            iterator.remove()
        }
        windowZOrderList.clear()
        windowHiddenSet.clear()
        windowMinimizedSet.clear()
        windowPairList.clear()
        onCSWindowChangedListeners.clear()
    }

    /**
     * Create Window with Location
     */
    fun createWindow(
        window: IWindow<ViewBinding>,
        location: Location,
        isOutOfScreen: Boolean = true,
        isDraggable: Boolean = true
    ): Boolean {
        if (isWindowExisted(window.tag)) {
            return false
        }
        val newWindowContainer = createInternal(window, location, isOutOfScreen, isDraggable)
        return addWindow(newWindowContainer)
    }

    /**
     * Create Window with Gravity
     */
    fun createWindow(
        window: IWindow<ViewBinding>,
        gravity: Gravity,
        isOutOfScreen: Boolean = true,
        isDraggable: Boolean = true
    ): Boolean {
        if (isWindowExisted(window.tag)) {
            return false
        }
        val newWindowContainer = createInternal(window, LocationUtil.gravityToLocation(gravity, window.getCurrentSize()), isOutOfScreen, isDraggable)
        return addWindow(newWindowContainer)
    }

    private fun createInternal(
        window: IWindow<ViewBinding>,
        location: Location,
        isOutOfScreen: Boolean,
        isDraggable: Boolean = true
    ): WindowContainer {
        val newWindowContainer = WindowContainer(windowManager, layoutInflater, this)
        newWindowContainer.initParams(window, location, isOutOfScreen, isDraggable)
        return newWindowContainer
    }

    /**
     * Opens a floating window and adds the windowContainer to the manager
     */
    private fun addWindow(window: WindowContainer): Boolean {
        var result = false
        window.floatingView.visibility = View.INVISIBLE
        if (window.addWindow()) {
            // Adds the window to the windowMap manager
            windowMap[window.config.tag] = window
            onCSWindowChangedListeners.forEach {
                it.onCSWindowCountChanged()
            }
            // record last Added windowTag
            windowZOrderList.add(window.config.tag)
            result = true
        }
        coroutineScope.launch(Dispatchers.Main) {
            if (hasSystemWindowShow()) {
                setSystemDialogOnTop()
                // delay for want system dialog bring to top done
                withContext(Dispatchers.IO) {
                    delay(200)
                }
            }
            // Honor any minimize/hide that landed between addWindow() returning
            // and this Main-queued block running.
            if (window.config.viewState != ViewState.VISIBLE) {
                return@launch
            }
            window.floatingView.visibility = View.VISIBLE
            notifyWindowStateChanged(window.config.tag, WindowState.VISIBLE)
        }
        return result
    }

    fun removeWindow(windowTag: WindowTag): Boolean {
        windowMap[windowTag]?.let { iWindowContainer ->
            val windowContainer = iWindowContainer as WindowContainer
            // Remove the window from the windowMap manager
            windowMap.remove(windowTag)
            onCSWindowChangedListeners.forEach {
                it.onCSWindowCountChanged()
            }
            // windowManager removeView
            windowContainer.removeWindow()
            notifyWindowStateChanged(windowTag, WindowState.CLOSED)
            // Remove the windowTag from the windowZOrderList
            windowZOrderList.remove(windowTag)
            windowHiddenSet.remove(windowTag)
            return true
        }
        return false
    }

    fun removeAllWindowsExcept(exemptWindowTags: List<WindowTag>) {
        val currentWindowTagList = windowMap.keys.toList()
        currentWindowTagList.forEach { windowTag ->
            if (exemptWindowTags.contains(windowTag)) {
                return@forEach
            }
            if (WindowTag.isSubWindowTag(windowTag)) {
                removeSubWindow(windowTag)
            } else {
                removeWindow(windowTag)
            }
        }
    }

    /**
     * Create a subWindow attached to the mainWindow
     * [subWindow]: subWindow itself.
     * [mainWindowTag]: The windowTag being attached.
     * [anchorX]: The horizontal coordinate of the button in the toolbar,
     * i.e., the button's coordinateX.
     * [relatedPosition]: he relative position between the subWindow and the mainWindow.
     */
    fun createSubWindow(
        subWindow: IWindow<ViewBinding>,
        mainWindowTag: WindowTag,
        anchorX: Int,
        relatedPosition: RelatedPosition
    ): Boolean {
        if (isWindowExisted(subWindow.tag)) {
            return false
        }
        getWindow(mainWindowTag)?.let { mainWindowContainer ->
            // Calculate location for subWindow
            val subWindowLocation = LocationUtil.calculateSubWindowLocation(subWindow.getCurrentSize(), mainWindowTag, anchorX, relatedPosition)
            // Create subWindow
            val subWindowContainer = createSubInternal(subWindow, subWindowLocation, mainWindowTag, relatedPosition)

            if (addWindow(subWindowContainer)) {
                // After successful creation, add the pairing relationship between subWindow and mainWindow
                windowPairList.add(
                    WindowPair(mainWindowTag = mainWindowTag, subWindowTag = subWindow.tag)
                )

                // Let the SubWindow know about the MainWindow's position changes
                mainWindowContainer.addOnViewPositionChangedListener(subWindowContainer)

                // Update mainWindow config status
                getWindow(mainWindowTag)?.getWindowConfig()?.apply {
                    hasSubWindow = true
                    anchorXOffset = anchorX - location.coordinateX
                }
                Timber.d("[createSubWindow]: WindowPair(main = $mainWindowTag, sub = ${subWindow.tag})")
                return true
            }
        }
        return false
    }

    private fun createSubInternal(
        window: IWindow<ViewBinding>,
        location: Location,
        mainWindowTag: WindowTag,
        relatedPosition: RelatedPosition,
    ): WindowContainer {
        val newWindowContainer = WindowContainer(windowManager, layoutInflater, this)
        newWindowContainer.initSubWindowParams(window, location, mainWindowTag, relatedPosition)
        return newWindowContainer
    }

    /**
     * Remove a specific subWindow
     * [subWindowTag]: The windowTag of the subWindow to remove
     */
    fun removeSubWindow(subWindowTag: WindowTag): Boolean {
        // Remove the pairing relationship between subWindow and mainWindow
        // and set mainWindow.config.hasSubWindow = false
        for (windowPair in windowPairList) {
            if (windowPair.subWindowTag == subWindowTag) {
                // Set mainWindow hasSubWindow = false
                getWindow(windowPair.mainWindowTag)?.let { mainWindowContainer ->
                    mainWindowContainer.getWindowConfig().hasSubWindow = false
                    Timber.d("[removeSubWindow]: mainWindow: ${mainWindowContainer.getWindowConfig().tag} - " +
                            "hasSubWindow: ${mainWindowContainer.getWindowConfig().hasSubWindow} ")

                    // Remove SubWindow / MainWindow position change detection
                    getWindow(windowPair.subWindowTag)?.let { subWindowContainer ->
                        mainWindowContainer.removeOnViewPositionChangedListener(subWindowContainer as WindowContainer)
                    }
                }

                // Delete the pairing relationship between subWindow and mainWindow
                windowPairList.remove(windowPair)
                break
            }
        }

        // Remove subWindow
        return removeWindow(subWindowTag)
    }

    /**
     * Remove all subWindows paired with the mainWindow
     * [mainWindowTag]: The windowTag of the mainWindow being attached to
     */
    fun removeSubWindowsByMainWindowTag(mainWindowTag: WindowTag) {
        val iterator = windowPairList.iterator()
        while (iterator.hasNext()) {
            val windowPair = iterator.next()
            if (windowPair.mainWindowTag == mainWindowTag) {
                // Remove subWindow
                removeWindow(windowPair.subWindowTag)
                // Remove the pairing relationship between subWindow and mainWindow
                iterator.remove()
            }
        }

        // Set mainWindow.config.hasSubWindow = false
        getWindow(mainWindowTag)?.let {
            it.getWindowConfig().hasSubWindow = false
            Timber.d("[removeSubWindowsByMainWindowTag]: mainWindow: ${it.getWindowConfig().tag} - " +
                    "hasSubWindow: ${it.getWindowConfig().hasSubWindow} ")
        }
    }

    /**
     * Get the windowTag with the highest z-order
     */
    fun getTopWindowTag(): WindowTag {
        if (windowZOrderList.isNotEmpty()) {
            return windowZOrderList.last()
        }
        return WindowTag.NONE
    }

    /**
     * Can be called by the Toolbar to bring the floating window to the top
     */
    suspend fun bringWindowToTop(windowTag: WindowTag): Boolean {
        bringToTopMutex.withLock {
            if (isWindowExisted(windowTag)) {
                getWindow(windowTag)?.let {
                    if (it.getWindowConfig().viewState != ViewState.VISIBLE) {
                        return true
                    }
                    it.hoistWindowZOrder()
                    return true
                }
            }
            return false
        }
    }

    /**
     * Used to refresh the z-order record of the floating window's windowTag,
     * called by the floating window itself
     */
    @Synchronized
    fun refreshTopWindowTagInZOrderList(windowTag: WindowTag): Boolean {
        if (isWindowExisted(windowTag)) {
            // Remove tag
            windowZOrderList.remove(windowTag)
            // Re-add to the end of the list
            windowZOrderList.add(windowTag)
            return true
        }
        return false
    }

    fun setSystemDialogOnTop() {
        getWindow(WindowTag.CS_SYSTEM_DIALOG)?.let {
            if (it is WindowContainer) {
                coroutineScope.launch(Dispatchers.Main) {
                    bringWindowToTop(it.config.tag)
                }
            }
        }
        getWindow(WindowTag.FORCE_LOGOUT_DIALOG)?.let {
            if (it is WindowContainer) {
                coroutineScope.launch(Dispatchers.Main) {
                    bringWindowToTop(it.config.tag)
                }
            }
        }
    }

    fun hideWindow(windowTag: WindowTag, isRecordHiddenState: Boolean = false) {
        if (windowZOrderList.contains(windowTag)) {
            setWindowVisibility(windowTag, ViewState.INVISIBLE)
            if (isRecordHiddenState) {
                windowHiddenSet.add(windowTag)
            }
            onCSWindowChangedListeners.forEach {
                it.onCSWindowHiddenCountChange()
            }
            notifyWindowStateChanged(
                windowTag,
                if (isRecordHiddenState) {
                    WindowState.HIDDEN
                } else {
                    WindowState.TEMPORARILY_HIDDEN
                }
            )
        }
    }

    fun showWindow(windowTag: WindowTag) {
        if (windowZOrderList.contains(windowTag)) {
            setWindowVisibility(windowTag, ViewState.VISIBLE)
            windowHiddenSet.remove(windowTag)
            windowMinimizedSet.remove(windowTag)

            onCSWindowChangedListeners.forEach {
                it.onCSWindowHiddenCountChange()
            }
            notifyWindowStateChanged(windowTag, WindowState.VISIBLE)
        }
    }

    fun isWindowHidden(windowTag: WindowTag): Boolean {
        return windowHiddenSet.contains(windowTag)
    }

    fun isWindowMinimized(windowTag: WindowTag): Boolean {
        return windowMinimizedSet.contains(windowTag)
    }

    fun minimizeWindow(windowTag: WindowTag) {
        if (windowZOrderList.contains(windowTag)) {
            val window = getWindow(windowTag) ?: return
            if (window.getWindowConfig().viewState != ViewState.VISIBLE) return
            setWindowVisibility(windowTag, ViewState.INVISIBLE)
            windowMinimizedSet.add(windowTag)
            onCSWindowChangedListeners.forEach {
                it.onCSWindowHiddenCountChange()
            }
            notifyWindowStateChanged(windowTag, WindowState.MINIMIZED)
        }
    }

    fun minimizeAllWindows(exemptionList: List<WindowTag> = emptyList()) {
        if (windowZOrderList.isEmpty()) return

        for (windowTag in windowZOrderList) {
            if (windowHiddenSet.contains(windowTag) || windowMinimizedSet.contains(windowTag)) {
                continue
            }
            if (exemptionList.contains(windowTag)) {
                continue
            }
            val window = getWindow(windowTag) ?: continue
            if (window.getWindowConfig().viewState != ViewState.VISIBLE) {
                continue
            }
            setWindowVisibility(windowTag, ViewState.INVISIBLE)
            windowMinimizedSet.add(windowTag)
            notifyWindowStateChanged(windowTag, WindowState.MINIMIZED)
        }

        onCSWindowChangedListeners.forEach {
            it.onCSWindowHiddenCountChange()
        }
    }

    fun hasSystemWindowShow(): Boolean {
        return windowZOrderList.any { it in WindowTag.getFirstLevelWindowTagList() }
    }

    /**
     * Hide all floating windows on the screen for the screenshot feature
     * [exemptionList]: A list of windowTags that do not need to be hidden can be passed in
     */
    fun hideAllWindows(exemptionList: List<WindowTag>, isIgnoreHiddenState: Boolean = true) {
        if (windowZOrderList.isEmpty()) return

        for (windowTag in windowZOrderList) {
            if (isIgnoreHiddenState && windowHiddenSet.contains(windowTag)) {
                Timber.d("[B][hideAllWindows] : windowHiddenSet.contains($windowTag)")
                continue
            }
            if (isIgnoreHiddenState && windowMinimizedSet.contains(windowTag)) {
                Timber.d("[B][hideAllWindows] : windowMinimizedSet.contains($windowTag)")
                continue
            }
            if (!exemptionList.contains(windowTag)) {
                setWindowVisibility(windowTag, ViewState.INVISIBLE)
                notifyWindowStateChanged(windowTag, WindowState.TEMPORARILY_HIDDEN)
            }
        }

        onCSWindowChangedListeners.forEach {
            it.onCSWindowHiddenCountChange()
        }
    }

    /**
     * Redisplay floating windows that were hidden for the screenshot feature
     */
    fun showAllWindows(isIgnoreHiddenState: Boolean = true) {
        if (windowZOrderList.isEmpty()) return

        for (windowTag in windowZOrderList) {
            if (isIgnoreHiddenState && windowHiddenSet.contains(windowTag)) {
                continue
            }
            if (isIgnoreHiddenState && windowMinimizedSet.contains(windowTag)) {
                continue
            }
            setWindowVisibility(windowTag, ViewState.VISIBLE)
            notifyWindowStateChanged(windowTag, WindowState.VISIBLE)
        }

        onCSWindowChangedListeners.forEach {
            it.onCSWindowHiddenCountChange()
        }
    }

    /**
     * Set the visibility of a floating window
     * [windowTag] enum: the name of the floating window
     * [viewState] enum: VISIBLE, INVISIBLE, GONE
     */
    private fun setWindowVisibility(windowTag: WindowTag, viewState: ViewState) {
        windowMap[windowTag]?.setVisibility(viewState)
    }

    private fun notifyWindowStateChanged(
        windowTag: WindowTag,
        state: WindowState
    ) {
        onCSWindowChangedListeners.forEach {
            it.onCSWindowStateChanged(windowTag, state)
        }
    }

    /**
     * Get the floating window by windowTag
     */
    fun getWindow(windowTag: WindowTag): IWindowContainer? {
        //TODO: Need to consider the issue of WindowContainer being
        // held externally for a long time causing Memory Leak.
        return windowMap[windowTag]
    }

    /**
     * Check if the floating window exists by windowTag
     */
    fun isWindowExisted(windowTag: WindowTag): Boolean {
        return windowMap.containsKey(windowTag)
    }

    fun addOnWindowChangedListener(onCSWindowChangedListener: OnCSWindowChangedListener) {
        onCSWindowChangedListeners.addIfAbsent(onCSWindowChangedListener)
    }

    fun removeOnWindowChangedListener(onCSWindowChangedListener: OnCSWindowChangedListener) {
        onCSWindowChangedListeners.remove(onCSWindowChangedListener)
    }

    /**
     * Toggle FLAG_NOT_FOCUSABLE on every currently-managed window's LayoutParams.
     *
     * focusable=true  → clear FLAG_NOT_FOCUSABLE (windows can receive focus / be seen by UIAutomator2)
     * focusable=false → set   FLAG_NOT_FOCUSABLE (default production behavior)
     *
     * Intended for stag-build instrumentation only.
     */
    fun setFocusable(focusable: Boolean) {
        windowMap.values.forEach { container ->
            val params = container.getLayoutParam()
            params.flags = if (focusable) {
                params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            } else {
                params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            container.updateLayoutParam(params)
        }
        Timber.d("[CSWindowManager] setFocusable(focusable=$focusable) applied to ${windowMap.size} windows")
    }

    enum class WindowState{
        VISIBLE,
        HIDDEN,
        TEMPORARILY_HIDDEN,
        MINIMIZED,
        CLOSED
    }
}
