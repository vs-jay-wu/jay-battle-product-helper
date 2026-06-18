package com.viewsonic.classswift.manager

/**
 * 暫存「mVB 要求在老師進班成功後才開啟」的開窗動作（VSFT-8429 spinner 冷啟動流程）。
 *
 * Flutter 在 CS 就緒、但老師尚未進班時送 `OpenWindowAfterClassEntry`；若當下尚未進班,
 * `MyViewBoardMessageHandler` 在解析 tag 那當下就把對應的開窗動作建成 closure 存進這裡，
 * 待進班成功（[com.viewsonic.classswift.ui.windowmodel.SelectOrgAndSelectClassWindowModel]）
 * 時 `consume()?.invoke()` 執行該動作，**不**另外開 JoinClass —— JoinClass 是否顯示
 * 改由該視窗自身依學生人數決定，避免 JoinClass 先被進班流程開出來再被關掉造成閃現。
 *
 * 存 closure 而非 tag 的用意：tag → opener 的對應屬於 IPC 解析層的職責，
 * `SelectOrgAndSelectClassWindowModel` 只負責「進班成功，執行 pending 動作或開預設 JoinClass」,
 * 不必為了新 tag 而被迫去認識每個 opener。
 *
 * 所有 call site 都在 Main thread（IPC handler `withContext(Dispatchers.Main)`、
 * WindowModel `handleCreateLessonSuccess` 內 `withContext(Dispatchers.Main)`、
 * `ClassSwiftService.onDestroy()` 也在 Main），單執行緒存取無 race，因此不需要鎖。
 */
class PendingClassEntryWindowManager {

    private var pendingAction: (suspend () -> Unit)? = null

    fun set(action: suspend () -> Unit) {
        pendingAction = action
    }

    /** 取出並清除暫存的開窗動作；沒有則回傳 null。 */
    fun consume(): (suspend () -> Unit)? {
        val action = pendingAction
        pendingAction = null
        return action
    }

    fun clear() {
        pendingAction = null
    }
}
