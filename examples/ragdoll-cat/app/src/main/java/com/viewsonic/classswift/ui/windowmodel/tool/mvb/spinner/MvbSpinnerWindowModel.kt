package com.viewsonic.classswift.ui.windowmodel.tool.mvb.spinner

import android.content.Context
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.data.enum.ClassType
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.socket.SelectStudentSocketMessage
import com.viewsonic.classswift.data.spinner.CandidateStudentInfo
import com.viewsonic.classswift.data.spinner.SpinnerStudentInfo
import com.viewsonic.classswift.factory.AmplitudeFactory
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.manager.StudentManager
import com.viewsonic.classswift.ui.helper.JoinClassWindowOpener
import com.viewsonic.classswift.ui.window.JoinClassWindow
import com.viewsonic.classswift.utils.LanguageUtils
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent
import timber.log.Timber

class MvbSpinnerWindowModel(
    private val socketManager: SocketManager,
    private val studentManager: StudentManager,
    private val networkManager: NetworkManager,
    private val classroomManager: ClassroomManager,
    private val accountManager: AccountManager,
) : IWindowModel {

    private val url = BuildConfig.SPINNER_URL + "?lang=${LanguageUtils.webLanguageCode}"
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private val _uiStateFlow = MutableStateFlow<MvbSpinnerUiState>(MvbSpinnerUiState.Loading)
    private val _uiEventFlow = MutableSharedFlow<MvbSpinnerUiEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val uiEventFlow = _uiEventFlow.asSharedFlow()
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private var networkObserveJob: Job? = null

    /**
     * 預設名單 prefetch 任務；[awaitPrefetchReady] 用此 join，
     * 確保 web JS 啟動拉名單時 StudentManager 已有資料。
     * 非「有預設名單」班級此 job 為 null（join null 等同 no-op）。
     */
    private var prefetchJob: Job? = null

    init {
        observeNetworkStatus()
        prefetchStudentListIfNeeded()
        orchestrateJoinClassIfNeeded()
    }

    /**
     * 給 web 端的轉盤名單。來源為 [currentSpinnerCandidates]，
     * 對 studentId 仍空的未連線學生合成 placeholder ID 讓 web 能渲染，
     * 抽中時於 [sendSelectedStudentEvent] 跳過 socket emit。
     */
    fun getCurrentStudentList(context: Context): SpinnerStudentInfo {
        val studentItems = currentSpinnerCandidates().map {
            CandidateStudentInfo(
                studentId = it.studentId.ifEmpty { PLACEHOLDER_ID_PREFIX + it.serialNumber },
                seatNumber = it.getActualDisplaySeatNumber(),
                name = it.getActualDisplayName(context)
            )
        }
        return SpinnerStudentInfo(data = studentItems)
    }

    /**
     * Spinner 候選名單（轉盤實際會出現的學生）。兩種班級模式（spec § 4 In Scope）：
     * - 有預設名單（ROSTER/TW_DEFAULT/SSO_GOOGLE）：取 StudentManager 整份 list（含 INACTIVE 預載），
     *   與 JoinClass 共用同一份 occupied_only=true 結果；學生連線後 studentId 會自動反映成真實值。
     * - 無名單（GUEST/OTHER）：只取 ACTIVE 學生（過濾未佔位）。
     *
     * 兩者都排除 JOINING（已連線但還沒設名字），避免轉盤出現「Typing...」/「Guest」無意義條目。
     * 由 [getCurrentStudentList]（轉盤名單）與 [orchestrateJoinClassIfNeeded]（< 2 開 JoinClass 判斷）
     * 共用，確保「轉盤名單人數」與「開 JoinClass 的人數判斷」用同一套基準。
     */
    private fun currentSpinnerCandidates(): List<StudentInfo> {
        val rawSource: List<StudentInfo> = if (isPreRosterMode()) {
            studentManager.getCurrentList()
        } else {
            studentManager.getCurrentAttendantList()
        }
        return rawSource.filter {
            it.getParticipationState() != StudentInfo.ParticipationState.JOINING
        }
    }

    private fun isPlaceholderStudentId(studentId: String): Boolean =
        studentId.startsWith(PLACEHOLDER_ID_PREFIX)

    /**
     * 是否為「有預設名單」班級。依 ClassType 列舉靜態判斷。
     * 與 JoinClass 的 runtime hasPreRoster（看 list 是否含 NOT_JOINED）互補：
     * 此處用於決定要不要主動 fetch 完整名冊。
     */
    private fun isPreRosterMode(): Boolean {
        val classroomInfo = classroomManager.classroomDataStateFlow.value.selectedClassroomInfo
        return when (ClassType.from(classroomInfo, accountManager.isGuestMode)) {
            ClassType.ROSTER, ClassType.TW_DEFAULT, ClassType.SSO_GOOGLE -> true
            ClassType.GUEST, ClassType.OTHER -> false
        }
    }

    /**
     * 「有預設名單」班級下，spinner 開啟時主動推一次 occupied_only=true fetch，
     * 確保 web JS 啟動取名單時 manager 內已有資料（不依賴 JoinClass 已先 fetch 過）。
     * 與 JoinClass 共用同一種 fetch 策略（fullReplace=true），不會互相干擾。
     */
    private fun prefetchStudentListIfNeeded() {
        if (!isPreRosterMode()) return
        prefetchJob = coroutineScope.launch {
            val lessonId = classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId
            if (lessonId.isEmpty()) {
                Timber.w("MvbSpinner prefetch skipped: lessonId empty")
                return@launch
            }
            val success = studentManager.fetchStudentInfoList(lessonId, occupiedOnly = true)
            if (success) {
                Timber.d("MvbSpinner prefetch done, manager size=${studentManager.getCurrentList().size}")
            } else {
                Timber.w("MvbSpinner prefetch failed; will read manager cache as-is")
            }
        }
    }

    /**
     * 等待 prefetch 完成。由 [MvbSpinnerWindow] 在 loadUrl 前呼叫，
     * 確保 WebView 啟動 JS 時 manager 已有資料，避免 web getStudentList() 早於 fetch 而拿到空清單。
     */
    suspend fun awaitPrefetchReady() {
        prefetchJob?.join()
    }

    fun sendSelectedStudentEvent(studentId: String) {
        if (isPlaceholderStudentId(studentId)) {
            Timber.d("Skip select_student socket emit for placeholder id: $studentId")
            return
        }
        Timber.d("sendSelectedStudentEvent student id : $studentId")
        socketManager.emit(
            SocketManager.EmittedEvent.SELECT_STUDENT,
            SelectStudentSocketMessage(studentId).toJSONObject()
        )
    }

    fun sendSpinnerClickedAmplitudeEvent(studentId: String) {
        // placeholder（未連線的預載學生）不是真實抽選，不計入 analytics，與 [sendSelectedStudentEvent] 一致。
        if (isPlaceholderStudentId(studentId)) return
        AmplitudeEventBuilder(AmplitudeConstant.EventName.SPINNER_CLICKED)
            .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
            .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
            .send()
    }

    fun sendSpinnerRemoveClickedAmplitudeEvent(studentId: String) {
        if (isPlaceholderStudentId(studentId)) return
        AmplitudeEventBuilder(AmplitudeConstant.EventName.SPINNER_REMOVED_CLICKED)
            .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
            .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
            .send()
    }

    fun getUrl(): String = url

    fun onWebPageFinished() {
        _uiStateFlow.value = MvbSpinnerUiState.Ready
    }

    private fun observeNetworkStatus() {
        networkObserveJob = coroutineScope.launch {
            networkManager.delayInformNetworkAvailabilityState.collect { isNetworkConnected ->
                Timber.d("Network Status: $isNetworkConnected")
                _uiEventFlow.emit(
                    MvbSpinnerUiEvent.NetworkStatusChange(isNetworkConnected = isNetworkConnected)
                )
            }
        }
    }

    private fun orchestrateJoinClassIfNeeded() {
        coroutineScope.launch(Dispatchers.Main) {
            // 等 prefetch 完成才判斷人數。新流程下進班直接開 spinner、不再經 JoinClass 先 fetch，
            // 若此處同步讀，roster 班級剛進班時 getCurrentList() 仍空、會誤判 < 2 而誤開 JoinClass。
            // guest 班級 prefetchJob 為 null，awaitPrefetchReady() 立即通過、行為不變。
            awaitPrefetchReady()
            if (currentSpinnerCandidates().size >= MIN_STUDENTS_FOR_SPIN) return@launch
            if (CSWindowManager.isWindowExisted(WindowTag.JOIN_CLASS)) return@launch
            // 透過 JoinClassWindowOpener 開啟，沿用 mVB toolbar 對齊定位（與其他入口一致）；
            // 直接 createWindow(Gravity.CENTER) 會讓 JoinClass 置中、位置與其他入口不一致。
            val joinClassWindow: JoinClassWindow = KoinJavaComponent.get(JoinClassWindow::class.java)
            JoinClassWindowOpener.open(joinClassWindow)
        }
    }

    override fun onCleared() {
        networkObserveJob?.cancel()
        networkObserveJob = null
    }

    companion object {
        private const val MIN_STUDENTS_FOR_SPIN = 2
        private const val PLACEHOLDER_ID_PREFIX = "placeholder_"
    }
}
