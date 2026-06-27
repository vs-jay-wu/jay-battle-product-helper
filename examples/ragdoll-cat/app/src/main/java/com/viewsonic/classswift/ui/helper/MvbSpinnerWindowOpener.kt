package com.viewsonic.classswift.ui.helper

import com.viewsonic.classswift.ui.window.tool.mvb.MvbSpinnerWindow
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import org.koin.java.KoinJavaComponent.get

/**
 * Opens [MvbSpinnerWindow] with the standard show-or-create idiom:
 * surface the existing window if it is already in the manager, otherwise
 * create a fresh instance centered on screen.
 *
 * Centralizes the open mechanic previously duplicated across
 * `JoinClassWindow`'s spinner button, the `OpenWindow` IPC handler, and
 * `SelectOrgAndSelectClassWindowModel`'s pending-window consumer.
 *
 * The MVB-bound guard is intentionally **not** part of this helper:
 * different call sites apply different policies (warn-and-skip in the IPC
 * path, fall back to JoinClass in the pending consumer), so the guard
 * stays at each call site and the helper stays focused on the open
 * mechanic.
 *
 * Must be invoked from a coroutine on the Main dispatcher — the
 * show-or-bring branch calls [CSWindowManager.bringWindowToTop] which is
 * a suspend function. Callers that need to fire-and-forget from a
 * non-suspend context (e.g. a click listener) should wrap the call in
 * `coroutineScope.launch(Dispatchers.Main) { ... }`.
 */
object MvbSpinnerWindowOpener {

    suspend fun open(): Boolean {
        return if (CSWindowManager.isWindowExisted(WindowTag.MVB_SPINNER)) {
            CSWindowManager.showWindow(WindowTag.MVB_SPINNER)
            CSWindowManager.bringWindowToTop(WindowTag.MVB_SPINNER)
            true
        } else {
            CSWindowManager.createWindow(get(MvbSpinnerWindow::class.java), Gravity.CENTER)
        }
    }
}
