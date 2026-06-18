package com.viewsonic.classswift.ui.windowmodel

import android.content.Context
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.body.CreateRoomBody
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.data.info.ClassroomInfo
import com.viewsonic.classswift.data.info.OrganizationInfo
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.PendingClassEntryWindowManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.ui.helper.JoinClassWindowOpener
import com.viewsonic.classswift.ui.window.JoinClassWindow
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get
import org.koin.java.KoinJavaComponent.inject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.getValue

class SelectOrgAndSelectClassWindowModel(
    private val androidContext: Context,
    private val classroomManager: ClassroomManager,
    private val accountManager: AccountManager,
    private val csWindowManager: CSWindowManager,
    private val toolbarManager: ToolbarManager,
    private val socketManager: SocketManager,
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider
) : IWindowModel {
    var orgId = ""
        private set
    private var userId = ""

    private var retryGetClassList = true

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private val pendingClassEntryWindowManager: PendingClassEntryWindowManager by inject(PendingClassEntryWindowManager::class.java)

    private val _updateUIFlow = MutableSharedFlow<ClassUIEvent>()
    val updateUIFlow = _updateUIFlow.asSharedFlow()

    val orgList: List<OrganizationInfo>
        get() = accountManager.getUserOrganizationInfo().organizations?.filter { it.isShownOrgItem } ?: emptyList()

    val isMultiOrg: Boolean
        get() = orgList.size > 1

    val selectedOrg: OrganizationInfo?
        get() = accountManager.selectedOrg

    init {
        accountManager.selectedOrg?.let {
            orgId = it.orgId
        }
        userId = accountManager.getUserOrganizationInfo().userId
    }

    fun switchOrg(org: OrganizationInfo) {
        accountManager.selectedOrg = org
        orgId = org.orgId
        retryGetClassList = true
        coroutineScope.launch(Dispatchers.IO) {
            // Emit OrgSwitched and fetch in the same coroutine so the Window
            // always processes OrgSwitched before any UpdateClassList/AddClass,
            // even when the API responds instantly (cached case).
            _updateUIFlow.emit(ClassUIEvent.OrgSwitched(org))
            fetchClassroomList()
        }
    }

    fun getClassroomList() {
        coroutineScope.launch(Dispatchers.IO) {
            fetchClassroomList()
        }
    }

    private suspend fun fetchClassroomList() {
        when (val responseData = classroomManager.getClassroomList(orgId, userId)) {
            is ClassroomManager.ClassroomResponseData.ErrorClassroomNotFound -> {
                createDefaultClass()
            }
            is ClassroomManager.ClassroomResponseData.ErrorNetworkDisconnected,
            is ClassroomManager.ClassroomResponseData.ErrorOther -> {
                _updateUIFlow.emit(ClassUIEvent.ShowRefreshUi)
                _updateUIFlow.emit(ClassUIEvent.ShowErrorToast(androidContext.getString(R.string.my_class_error_msg_fetch_class)))
            }
            is ClassroomManager.ClassroomResponseData.Success -> {
                _updateUIFlow.emit(ClassUIEvent.UpdateClassList(responseData.resultData))
            }
            else -> {}
        }
    }

    fun deleteClassroom(classInfo: ClassroomInfo) {
        coroutineScope.launch(Dispatchers.IO) {
            when (classroomManager.deleteClassroom(classInfo.id)) {
                is ClassroomManager.ClassroomResponseData.Success -> {
                    _updateUIFlow.emit(ClassUIEvent.DeleteClassSuccess(classInfo))
                }
                else -> {
                    _updateUIFlow.emit(ClassUIEvent.DeleteClassFailed(classInfo.displayName))
                }
            }
        }
    }

    /**
     * Auto-creates a fallback class when the backend reports no classes for this org
     * (either 404 ErrorClassroomNotFound or a 200 with an empty list). Routes failure
     * through the `fromGetClassList=true` branch so the refresh-page UI is shown rather
     * than re-enabling the create button on top of a still-empty loading state.
     */
    fun createDefaultClass() =
        createTimestampClassroom(LocalDateTime.now(), emptyList(), fromGetClassList = true)

    fun createTimestampClassroom(
        now: LocalDateTime,
        existingNames: List<String>,
        fromGetClassList: Boolean = false
    ) {
        val counter = nextGuestClassCounter(existingNames, now)
        val displayName = buildGuestClassName(now, counter)
        createClassroom(displayName, fromGetClassList = fromGetClassList)
    }

    fun createClassroom(
        className: String,
        studentList: Boolean = true,
        grouping: Boolean = true,
        fromGetClassList: Boolean = false
    ) {
        val body = CreateRoomBody(
            className,
            icon = 0,
            orgId = orgId,
            teacherId = userId,
            studentCount = accountManager.selectedOrg?.studentConcurrent ?: 0
        )
        coroutineScope.launch(Dispatchers.IO) {
            when (val responseData = classroomManager.createClassroom(body, studentList, grouping)) {
                is ClassroomManager.ClassroomResponseData.Success -> {
                    _updateUIFlow.emit(ClassUIEvent.AddClass(responseData.resultData, body))
                    _updateUIFlow.emit(ClassUIEvent.EnableAddClassButton)
                }
                else -> {
                    _updateUIFlow.emit(ClassUIEvent.ShowErrorToast(androidContext.getString(R.string.my_class_error_msg_create_class)))
                    if (fromGetClassList) {
                        retryGetClassList = false
                        _updateUIFlow.emit(ClassUIEvent.ShowRefreshUi)
                    } else {
                        _updateUIFlow.emit(ClassUIEvent.EnableAddClassButton)
                    }
                }
            }
        }
    }

    fun refreshClassPage() {
        if (retryGetClassList) {
            getClassroomList()
        } else {
            createTimestampClassroom(LocalDateTime.now(), emptyList())
        }
    }

    fun updateClassroom(
        classroomId: String,
        updateName: String,
        icon: Int = 0
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            when (val responseData = classroomManager.updateRooms(classroomId, updateName, icon)) {
                is ClassroomManager.ClassroomResponseData.Success -> {
                    _updateUIFlow.emit(ClassUIEvent.UpdateClassInfo(responseData.resultData))
                }
                else -> {
                    _updateUIFlow.emit(ClassUIEvent.UpdateClassInfoFailed)
                }
            }
        }
    }

    fun isSocketConnected() = socketManager.isConnected()

    fun createLesson(classroomInfo: ClassroomInfo, tag: WindowTag) {
        coroutineScope.launch(Dispatchers.IO) {
            when (val responseData = classroomManager.createLesson(classroomInfo)) {
                is ClassroomManager.ClassroomResponseData.Success -> {
                    handleCreateLessonSuccess(classroomInfo, responseData.resultData, tag)
                }
                is ClassroomManager.ClassroomResponseData.ErrorMultipleTeachersInSession -> {
                    _updateUIFlow.emit(ClassUIEvent.ShowErrorToast(androidContext.getString(R.string.my_class_error_msg_join_lesson_conflict)))
                    _updateUIFlow.emit(ClassUIEvent.EnableEnterClassButton)
                }
                else -> {
                    val msgRes = if (classroomInfo.isLessonOnGoing()) R.string.my_class_error_msg_rejoin_class else R.string.my_class_error_msg_join_class
                    _updateUIFlow.emit(ClassUIEvent.ShowErrorToast(androidContext.getString(msgRes)))
                    _updateUIFlow.emit(ClassUIEvent.EnableEnterClassButton)
                }
            }
        }
    }

    private suspend fun handleCreateLessonSuccess(
        classroomInfo: ClassroomInfo,
        resultData: ClassroomInfo,
        tag: WindowTag
    ) {
        if (classroomInfo.isLessonOnGoing()) {
            _updateUIFlow.emit(ClassUIEvent.ShouldCheckUnclosedMission)
        }
        withContext(Dispatchers.Main) {
            csWindowManager.hideWindow(WindowTag.WINDOW_QUIZ_COLLECTION, isRecordHiddenState = true)
            csWindowManager.removeWindow(tag)
            // VSFT-8429: pending 由 IPC handler 在解析 tag 當下就建好 closure（含 bound 重判 + fallback 政策),
            // 這裡只負責執行；沒 pending 才走預設 JoinClass。
            val pendingAction = pendingClassEntryWindowManager.consume()
            if (pendingAction == null) {
                JoinClassWindowOpener.open(get(JoinClassWindow::class.java))
            } else {
                pendingAction()
            }
        }
        if (resultData.isLessonOnGoing()) {
            toolbarManager.setParticipationState(ToolbarManager.ParticipationState.LESSON_STARTED)
            return
        }
        toolbarManager.setParticipationState(ToolbarManager.ParticipationState.JOINED)
        if (myViewBoardConnectionStateProvider.isBound() && classroomManager.startLesson()) {
            toolbarManager.setParticipationState(ToolbarManager.ParticipationState.LESSON_STARTED)
        }
    }

    override fun onCleared() = Unit

    companion object {
        private val GUEST_CLASS_NAME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MMM d, yyyy (a)", Locale.ENGLISH)

        /** Format a guest-class displayName as `MMM d, yyyy (a) #NNN` (e.g. `Apr 28, 2026 (AM) #001`). */
        fun buildGuestClassName(now: LocalDateTime, counter: Int): String {
            val prefix = now.format(GUEST_CLASS_NAME_FORMATTER)
            return "$prefix #${"%03d".format(counter)}"
        }

        /**
         * Returns the next counter for the (date, AM/PM) block defined by [now], by scanning
         * [existingNames] for names already in that block. Counter resets to 1 on every block boundary.
         */
        fun nextGuestClassCounter(existingNames: List<String>, now: LocalDateTime): Int {
            val prefix = now.format(GUEST_CLASS_NAME_FORMATTER)
            val escapedPrefix = Regex.escape(prefix)
            val pattern = Regex("""^$escapedPrefix #(\d{3})$""")
            val maxCounter = existingNames
                .mapNotNull { pattern.matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }
                .maxOrNull() ?: 0
            return maxCounter + 1
        }
    }

    sealed class ClassUIEvent {
        data class UpdateClassList(val classList: List<ClassroomInfo>) : ClassUIEvent()
        data class AddClass(val classInfo: ClassroomInfo, val createRoomBody: CreateRoomBody) : ClassUIEvent()
        data class ShowErrorToast(val msg: String) : ClassUIEvent()
        data class UpdateClassInfo(val classInfo: ClassroomInfo) : ClassUIEvent()
        data object UpdateClassInfoFailed : ClassUIEvent()
        data class DeleteClassSuccess(val classInfo: ClassroomInfo) : ClassUIEvent()
        data class DeleteClassFailed(val className: String) : ClassUIEvent()
        data object EnableAddClassButton : ClassUIEvent()
        data object EnableEnterClassButton : ClassUIEvent()
        data object ShowRefreshUi : ClassUIEvent()
        data object ShouldCheckUnclosedMission : ClassUIEvent()
        data class OrgSwitched(val org: OrganizationInfo) : ClassUIEvent()
    }
}
