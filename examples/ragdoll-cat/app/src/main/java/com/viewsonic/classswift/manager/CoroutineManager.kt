package com.viewsonic.classswift.manager

import com.viewsonic.classswift.constant.AppConstants.ONE_SEC_DELAY
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * The purpose of CoroutineManager is to provide CoroutineScope generating method
 * for caller to use in non-lifecycle-aware component.
 *
 * Every CoroutineScope which is generated from this manager will be cancelled automatically
 * when it is not used anymore.
 */
object CoroutineManager {
    private const val DEFAULT_CLEAN_MS = 3 * 60 * ONE_SEC_DELAY // 3 minutes
    private const val DEFAULT_TIME_OUT_MS = 5 * 60 * ONE_SEC_DELAY // 5 minutes

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scopeMap = ConcurrentHashMap<WeakReference<Any>, CoroutineScope>()

    init {
        scope.launch {
            while (true) {
                delay(DEFAULT_CLEAN_MS)
                Timber.d("[Cleaner]: Start")
                synchronized(this@CoroutineManager) {
                    val iterator = scopeMap.iterator()
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        if (entry.key.get() == null) {
                            Timber.d("[Cleaner]: Remove (${entry.key}, ${entry.value})")
                            entry.value.cancel("Not be referenced.")
                            iterator.remove()
                        }
                    }
                }
            }
        }
    }

    /**
     * This CoroutineScope will only run in a single thread and is typically used in database
     * components that are frequently accessed by multiple threads.
     *
     * @param timeoutMs : Indicate that generated CoroutineScope will be cancelled after the
     *  specified time.
     */
    @JvmStatic
    @Synchronized
    fun getSingleThreadScope(timeoutMs: Long = DEFAULT_TIME_OUT_MS): CoroutineScope {
        val coroutineScope =
            CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
        scope.launch {
            delay(timeoutMs)
            coroutineScope.cancel("Timeout.")
            Timber.d("[getSingleThreadScope]: Cancel $coroutineScope")
        }
        Timber.d("[getSingleThreadScope]: Generate ($timeoutMs, $coroutineScope)")
        return coroutineScope
    }

    /**
     * This CoroutineScope is typically used in common cases.
     *
     * @param timeoutMs : Indicate that generated CoroutineScope will be cancelled after the
     *  specified time.
     */
    @JvmStatic
    @Synchronized
    fun getScope(timeoutMs: Long = DEFAULT_TIME_OUT_MS): CoroutineScope {
        val coroutineScope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            delay(timeoutMs)
            coroutineScope.cancel("Timeout.")
            Timber.d("[getScope]: Cancel $coroutineScope")
        }
        Timber.d("[getScope]: Generate ($timeoutMs, $coroutineScope)")
        return coroutineScope
    }

    /**
     * This CoroutineScope will only run in a single thread and is typically used in database
     * components that are frequently accessed by multiple threads.
     *
     * @param obj : Indicate that generated CoroutineScope will be cancelled after system released
     *  specified object.
     */
    @JvmStatic
    @Synchronized
    fun getSingleThreadScope(obj: Any): CoroutineScope {
        Timber.d("[getSingleThreadScope]: Object = $obj")
        scopeMap.forEach { (wk, cs) ->
            if (wk.get() == obj) {
                return if (cs.isActive) {
                    Timber.d("[getSingleThreadScope]: Hit Cache!")
                    cs
                } else {
                    Timber.d("[getSingleThreadScope]: Inactive scope, replace with new one")
                    val newScope =
                        CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
                    scopeMap[wk] = newScope
                    newScope
                }
            }
        }

        val coroutineScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
        val weakReference = WeakReference(obj)
        Timber.d("[getSingleThreadScope]: Generate ($weakReference, $coroutineScope)\")")
        scopeMap[weakReference] = coroutineScope
        return coroutineScope
    }

    /**
     * This CoroutineScope is typically used in common cases.
     *
     * @param obj : Indicate that generated CoroutineScope will be cancelled after system released
     *  specified object.
     */
    @JvmStatic
    @Synchronized
    fun getScope(obj: Any): CoroutineScope {
        Timber.d("[getScope]: Object = $obj")
        scopeMap.forEach { (wk, cs) ->
            if (wk.get() == obj) {
                return if (cs.isActive) {
                    Timber.d("[getScope]: Hit Cache!")
                    cs
                } else {
                    Timber.d("[getScope]: Inactive scope, replace with new one")
                    val newScope = CoroutineScope(Dispatchers.Default)
                    scopeMap[wk] = newScope
                    newScope
                }
            }
        }


        val coroutineScope = CoroutineScope(Dispatchers.Default)
        val weakReference = WeakReference(obj)
        Timber.d("[getScope]: Generate ($weakReference, $coroutineScope)")
        scopeMap[weakReference] = coroutineScope
        return coroutineScope
    }

    /**
     * It is typically used in whenever you want to cancel generated CoroutineScope manually.
     */
    @JvmStatic
    @Synchronized
    fun cancelScope(obj: Any) {
        Timber.d("[cancelScope]: Object = $obj")
        scopeMap.keys.firstOrNull { wk -> wk.get() == obj }
            ?.let { wk ->
                scopeMap[wk]?.let { coroutineScope ->
                    Timber.d("[cancelScope]: Cancel $coroutineScope")
                    coroutineScope.cancel("Manually.")
                }
                scopeMap.remove(wk)
            }
    }
}