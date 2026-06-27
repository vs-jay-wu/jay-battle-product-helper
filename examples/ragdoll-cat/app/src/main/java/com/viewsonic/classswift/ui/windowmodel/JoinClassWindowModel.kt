package com.viewsonic.classswift.ui.windowmodel

import android.content.ClipData
import android.content.ClipboardManager
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.data.enum.ClassType
import com.viewsonic.classswift.data.enum.MvbToolbarPosition
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.MvbToolbarStateManager
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.manager.StudentManager
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JoinClassWindowModel(
    private val classroomManager: ClassroomManager,
    private val accountManager: AccountManager,
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider,
    private val clipboardManager: ClipboardManager,
    private val toolbarManager: ToolbarManager,
    private val studentManager: StudentManager,
    private val networkManager: NetworkManager,                  // VSFT-8256
    private val mvbToolbarStateManager: MvbToolbarStateManager,  // VSFT-7257
) : IWindowModel {

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var seatEventDebounceJob: Job? = null

    private val _uiEvent = MutableSharedFlow<JoinClassUIEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    /** JoinClass 永遠使用後端 occupied_only=true；
     *  此旗標保留於 WindowModel 層，方便日後需要時切回前端過濾。 */
    private val isOccupiedOnlyMode: Boolean = true

    init {
        initSeatEventObservation()
    }

    private val selectedClassroom
        get() = classroomManager.classroomDataStateFlow.value.selectedClassroomInfo

    fun getRoomLink(): String = selectedClassroom.roomLink

    /** 優先顯示短網址（room_short_url），無則 fallback 完整 roomLink */
    fun getDisplayRoomLink(): String =
        selectedClassroom.roomShortUrl.ifEmpty { selectedClassroom.roomLink }

    fun getClassCode(): String = selectedClassroom.number

    fun getClassName(): String = selectedClassroom.displayName

    fun isMyViewBoardBound(): Boolean = myViewBoardConnectionStateProvider.isBound()

    fun isGuestMode(): Boolean = accountManager.isGuestMode

    fun isSelectedClassroomInfoExisted(): Boolean = selectedClassroom.isValid()

    // ── VSFT-8256: Network disconnect ────────────────────────────────────────

    /** true = 有網路；false = 斷線（3 秒延遲通知，避免短暫抖動） */
    val networkAvailabilityFlow: StateFlow<Boolean> =
        networkManager.delayInformNetworkAvailabilityState

    // ─────────────────────────────────────────────────────────────────────────

    // ── VSFT-7257: mVB toolbar position + whiteboard layout ──────────────────

    /**
     * Current myViewBoard main toolbar position.
     *
     * `null` means mVB has not yet reported a position via the
     * `MessageToolbarPositionChanged` IPC. The window keeps its default
     * gravity-based position in that case.
     */
    val mvbToolbarPositionFlow: StateFlow<MvbToolbarPosition?> =
        mvbToolbarStateManager.position

    /**
     * Current mVB whiteboard top edge (dp). `null` until mVB reports it via
     * the optional `whiteboard_top_dp` field of `MessageToolbarPositionChanged`.
     * Observers fall back to a hardcoded default when null.
     */
    val mvbWhiteboardTopDpFlow: StateFlow<Double?> =
        mvbToolbarStateManager.whiteboardTopDp

    // ─────────────────────────────────────────────────────────────────────────

    // ── VSFT-8437: Spinner entry point ───────────────────────────────────────

    /**
     * Spinner button 是否顯示 — true 當 MVB-bound + 有網路；其他狀態 false。
     * combine 網路與 bind 兩條 flow，使 MVB bind/unbind（即使網路沒變）也能即時更新按鈕。
     * 由 View 層翻譯為 visibility（不在 WindowModel 引用 android.view.View）。
     */
    val isSpinnerButtonVisible: StateFlow<Boolean> = combine(
        networkAvailabilityFlow,
        myViewBoardConnectionStateProvider.isBoundFlow()
    ) { connected, bound -> connected && bound }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    // ─────────────────────────────────────────────────────────────────────────

    // ── VSFT-7256: Student attendance panel ──────────────────────────────────

    val studentInfoListFlow = studentManager.studentInfoListFlow

    fun getClassType(): ClassType = ClassType.from(selectedClassroom, isGuestMode())

    /**
     * 依目前模式決定顯示用清單：
     * - isOccupiedOnlyMode=true  → 後端已過濾，直接使用原始清單
     * - isOccupiedOnlyMode=false → 套用前端 getVisibleStudentList() 過濾
     */
    fun getDisplayList(list: List<StudentInfo>): List<StudentInfo> =
        if (isOccupiedOnlyMode) list else getVisibleStudentList(list)

    /**
     * 前端過濾邏輯（[isOccupiedOnlyMode]=false 時的 fallback）：
     * 依班級類型過濾清單，回傳應顯示在 UI 的學生。
     *
     * - GUEST      : 只保留 status == ACTIVE（略過 INACTIVE 佔位）
     * - SSO_GOOGLE : 保留有歷史名稱（displayName 不為空）或目前正在加入（status == ACTIVE）
     * - 其他       : 不過濾，全數顯示
     */
    private fun getVisibleStudentList(list: List<StudentInfo>): List<StudentInfo> =
        filterByClassType(list, getClassType())

    fun getMaxStudentCount(): Int = selectedClassroom.maxStudentCount

    fun isStudentExisted(studentInfo: StudentInfo): Boolean =
        studentManager.getCurrentList().any { it.studentId == studentInfo.studentId }

    /**
     * 學生清單取得，固定使用 occupied_only=true（後端只回傳已入座學生）。
     * 由 [JoinClassWindow] 在開窗後主動呼叫；座位變動與重連時透過
     * [initSeatEventObservation] 自動觸發。
     */
    suspend fun fetchStudentInfoList(): Boolean =
        studentManager.fetchStudentInfoList(selectedClassroom.lessonId, occupiedOnly = true)

    suspend fun removeStudent(studentId: String): Boolean =
        studentManager.removeStudent(selectedClassroom.lessonId, studentId)

    // ─────────────────────────────────────────────────────────────────────────

    fun onCopyLink() {
        val clip = ClipData.newPlainText("Class Link", getRoomLink())
        clipboardManager.setPrimaryClip(clip)
        coroutineScope.launch { _uiEvent.emit(JoinClassUIEvent.ShowCopySuccess) }
    }

    fun onSwitchClass() {
        coroutineScope.launch { _uiEvent.emit(JoinClassUIEvent.ShowSwitchClassDialog) }
    }

    fun confirmSwitchClass() {
        coroutineScope.launch {
            toolbarManager.endLesson()
            toolbarManager.setParticipationState(ToolbarManager.ParticipationState.NOT_JOINED)
            _uiEvent.emit(JoinClassUIEvent.NavigateBackToSelectClass)
        }
    }

    /**
     * 訂閱 StudentManager 的座位變動信號與 socket 重連事件，
     * 由 JoinClass 自行決定以 occupied_only=true 重新 fetch。
     *
     * - studentSeatEventFlow : 300ms debounce，合併同一批次的連續座位變動
     * - SocketReconnected    : 立即重新 fetch，確保重連後清單準確
     */
    private fun initSeatEventObservation() {
        coroutineScope.launch {
            studentManager.studentSeatEventFlow.collect {
                seatEventDebounceJob?.cancel()
                seatEventDebounceJob = coroutineScope.launch {
                    delay(300L)
                    fetchStudentInfoList()
                }
            }
        }
        coroutineScope.launch {
            studentManager.studentChangeReasonFlow.collect { reason ->
                if (reason is StudentManager.StudentChangeReason.SocketReconnected) {
                    fetchStudentInfoList()
                }
            }
        }
    }

    override fun onCleared() {
        seatEventDebounceJob?.cancel()
        coroutineScope.cancel()
    }

    sealed class JoinClassUIEvent {
        data object ShowCopySuccess : JoinClassUIEvent()
        data object ShowSwitchClassDialog : JoinClassUIEvent()
        data object NavigateBackToSelectClass : JoinClassUIEvent()
    }

    companion object {

        /**
         * 前端過濾純函式（[isOccupiedOnlyMode]=false 時的 fallback）。
         *
         * - [ClassType.GUEST]      : 只保留 status == ACTIVE
         * - [ClassType.SSO_GOOGLE] : 保留有歷史名稱或正在加入（status == ACTIVE）
         * - 其他                   : 不過濾，全數回傳
         */
        internal fun filterByClassType(
            list: List<StudentInfo>,
            classType: ClassType
        ): List<StudentInfo> = when (classType) {
            ClassType.GUEST ->
                list.filter { it.status == StudentInfo.Status.ACTIVE }
            ClassType.SSO_GOOGLE ->
                list.filter { it.displayName.isNotEmpty() || it.status == StudentInfo.Status.ACTIVE }
            ClassType.ROSTER, ClassType.TW_DEFAULT, ClassType.OTHER ->
                list
        }
    }
}
