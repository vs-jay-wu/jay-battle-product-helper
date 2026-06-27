package com.viewsonic.classswift.utils.extension

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Rect
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.view.doOnAttach
import com.viewsonic.classswift.utils.DisplayUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean


fun View.fade(
    fadeOutDuration: Long = 495L,
    fadeInDuration: Long = 495L
): AnimatorSet {
    val fadeOut = ObjectAnimator.ofFloat(this, View.ALPHA, 1f, 0f)
    val fadeIn = ObjectAnimator.ofFloat(this, View.ALPHA, 0f, 1f)

    fadeOut.duration = fadeOutDuration
    fadeIn.duration = fadeInDuration

    val animatorSet = AnimatorSet()
    animatorSet.playSequentially(fadeOut, fadeIn)

    animatorSet.start()
    return animatorSet
}

/**
 * Returns the X and Y coordinates of this View on the screen,
 * with the Y position adjusted to exclude the status bar height.
 *
 * @return Pair of (x, y) coordinates:
 *         - First: X coordinate
 *         - Second: Y coordinate
 */
fun View.getLocationOnScreenWithoutStatusBar(): Pair<Int, Int> {
    val location = IntArray(2)
    getLocationOnScreen(location)

    location[1] -= DisplayUtils.getStatusBarHeight(context, true)

    return location[0] to location[1]
}

fun View.setDebouncedClickListener(interval: Long = 500L, onClick: (View) -> Unit) {
    var lastClickTime = 0L
    this.setOnClickListener { view ->
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime > interval) {
            lastClickTime = currentTime
            onClick(view)
        }
    }
}

/**
 * Suspends until this View is actually visible on screen.
 *
 * "Actually visible" means:
 * 1) attached to a Window,
 * 2) isShown == true (self + all ancestors visible),
 * 3) has a non-empty global visible rect (not fully clipped).
 *
 * If already visible, returns immediately.
 */

suspend fun View.awaitVisible() = suspendCancellableCoroutine<Unit> { cont ->
    fun vto() = viewTreeObserver

    var onGlobalLayout: ViewTreeObserver.OnGlobalLayoutListener? = null
    var onLayoutChange: View.OnLayoutChangeListener? = null
    var attachListener: View.OnAttachStateChangeListener? = null

    fun cleanup() {
        vto().takeIf { it.isAlive }?.let { v ->
            onGlobalLayout?.let { v.removeOnGlobalLayoutListener(it) }
        }
        onLayoutChange?.let { removeOnLayoutChangeListener(it) }
        attachListener?.let { removeOnAttachStateChangeListener(it) }
    }

    // Use AtomicBoolean to ensure resume happens only once
    val resumed = AtomicBoolean(false)

    fun finishOnce() {
        if (resumed.compareAndSet(false, true)) {
            cont.resume(Unit) { cause, _, _ ->
                cleanup()
            }
        }
    }

    // Already visible → complete immediately
    if (isActuallyVisible()) {
        finishOnce()
        return@suspendCancellableCoroutine
    }

    fun tryComplete() {
        if (!cont.isCancelled && isActuallyVisible()) {
            finishOnce()
        }
    }

    onGlobalLayout = ViewTreeObserver.OnGlobalLayoutListener {
        Timber.tag("ViewExt").d("awaitVisible ViewTreeObserver.OnGlobalLayoutListener")
        tryComplete()
    }
    onLayoutChange = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        Timber.tag("ViewExt").d("awaitVisible ViewTreeObserver.OnLayoutChangeListener")
        tryComplete()
    }

    fun register() {
        vto().takeIf { it.isAlive }?.let { v ->
            onGlobalLayout.let { v.addOnGlobalLayoutListener(it) }
        }
        addOnLayoutChangeListener(onLayoutChange)
        tryComplete() // After register, check ui is complete
    }

    fun unregister() = cleanup()

    attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) = register()
        override fun onViewDetachedFromWindow(v: View) = unregister()
    }

    addOnAttachStateChangeListener(attachListener)
    doOnAttach { register() }

    // Clean up on cancellation
    cont.invokeOnCancellation { unregister() }
}

fun View.isActuallyVisible(): Boolean {
    if (!isAttachedToWindow) return false
    if (!isShown) return false
    val r = Rect()
    return getGlobalVisibleRect(r) && r.width() > 0 && r.height() > 0
}

/**
 * Suspends until the View becomes "invisible":
 * - Not attached, !isShown, or its visible area ratio is <= [threshold] (default = 0f, meaning completely invisible).
 *
 * Example:
 *   someView.awaitGoneOrFullyClipped()        // Continues only when completely invisible
 *   someView.awaitGoneOrFullyClipped(0.1f)    // Treated as invisible when visible area ≤ 10%
 */
suspend fun View.awaitGoneOrFullyClipped(threshold: Float = 0f) =
    suspendCancellableCoroutine<Unit> { cont ->

        fun vto() = viewTreeObserver

        var onGlobalLayout: ViewTreeObserver.OnGlobalLayoutListener? = null
        var onLayoutChange: View.OnLayoutChangeListener? = null
        var attachListener: View.OnAttachStateChangeListener? = null

        fun cleanup() {
            vto().takeIf { it.isAlive }?.let { v ->
                onGlobalLayout?.let { v.removeOnGlobalLayoutListener(it) }
            }
            onLayoutChange?.let { removeOnLayoutChangeListener(it) }
            attachListener?.let { removeOnAttachStateChangeListener(it) }
        }

        // Use AtomicBoolean to ensure resume happens only once
        val finished = AtomicBoolean(false)

        fun finishOnce() {
            if (finished.compareAndSet(false, true)) {
                cont.resume(Unit) { cause, _, _ ->
                    cleanup()
                }
            }
        }

        // if view is already gone, just return
        if (isEffectivelyGone(threshold)) {
            finishOnce()
            return@suspendCancellableCoroutine
        }

        fun tryComplete() {
            if (!cont.isCancelled && isEffectivelyGone(threshold)) {
                finishOnce()
            }
        }

        onGlobalLayout = ViewTreeObserver.OnGlobalLayoutListener {
            Timber.tag("ViewExt").d("awaitGoneOrFullyClipped ViewTreeObserver.OnGlobalLayoutListener")
            tryComplete()
        }
        onLayoutChange = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            Timber.tag("ViewExt").d("awaitGoneOrFullyClipped View.OnLayoutChangeListener")
            tryComplete()
        }

        fun register() {
            vto().takeIf { it.isAlive }?.let { v ->
                onGlobalLayout.let { v.addOnGlobalLayoutListener(it) }
            }
            addOnLayoutChangeListener(onLayoutChange)
            tryComplete()
        }

        fun unregister() = cleanup()

        attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = register()
            override fun onViewDetachedFromWindow(v: View) = unregister() // Once detached, also considered "invisible"
        }

        addOnAttachStateChangeListener(attachListener)
        doOnAttach { register() }

        cont.invokeOnCancellation { unregister() }
    }

/**
 * Determines when the View is considered "invisible" (visible ratio ≤ threshold).
 * Default threshold = 0f, meaning completely invisible / clipped / not attached / not shown.
 */
fun View.isEffectivelyGone(threshold: Float = 0f): Boolean {
    if (!isAttachedToWindow || !isShown) return true
    return visibleAreaRatio() <= threshold
}

/**
 * Returns the current visible area ratio (0f..1f);
 * returns 0f if not attached or if the size has not yet been measured.
 */

fun View.visibleAreaRatio(): Float {
    if (!isAttachedToWindow || !isShown) return 0f
    if (width <= 0 || height <= 0) return 0f
    val r = Rect()
    val has = getGlobalVisibleRect(r)
    if (!has) return 0f
    val visibleArea = r.width() * r.height()
    val totalArea = width * height
    if (totalArea <= 0) return 0f
    return (visibleArea.toFloat() / totalArea.toFloat()).coerceIn(0f, 1f)
}
