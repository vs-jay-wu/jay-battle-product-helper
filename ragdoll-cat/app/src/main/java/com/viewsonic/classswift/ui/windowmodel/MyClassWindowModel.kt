package com.viewsonic.classswift.ui.windowmodel

import android.content.Context
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.body.CreateRoomBody
import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.data.info.ClassroomInfo
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.SocketManager
import com.viewsonic.classswift.manager.TutorialManager
import com.viewsonic.classswift.ui.helper.JoinClassWindowOpener
import com.viewsonic.classswift.ui.window.JoinClassWindow
import com.viewsonic.classswift.windowframework.core.CSWindowManager
import com.viewsonic.classswift.windowframework.core.enums.Gravity
import com.viewsonic.classswift.windowframework.core.enums.WindowTag
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.get


class MyClassWindowModel(
    private val androidContext: Context,
    private val classroomManager: ClassroomManager,
    private val accountManager: AccountManager,
    private val csWindowManager: CSWindowManager,
    private val toolbarManager: ToolbarManager,
    private val tutorialManager: TutorialManager,
    private val socketManager: SocketManager,
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider
) : IWindowModel {
    var orgId = ""
        private set
    var userId = ""
        private set

    // to check get class list is success or not
    private var retryGetClassList = true

    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)

    private val _updateUIFlow = MutableSharedFlow<ClassUIEvent>()
    val updateUIFlow = _updateUIFlow.asSharedFlow()

    init {
        accountManager.selectedOrg?.let {
            orgId = it.orgId
        }
        userId = accountManager.getUserOrganizationInfo().userId
    }

    fun isMyViewBoardBound() = myViewBoardConnectionStateProvider.isBound()

    fun getClassroomList() {
        coroutineScope.launch(Dispatchers.IO) {
            when (val responseData = classroomManager.getClassroomList(orgId, userId)) {
                is ClassroomManager.ClassroomResponseData.ErrorClassroomNotFound -> {
                    //新增預設class
                    createClassroom(className = androidContext.getString(R.string.my_class_class_default_name), fromGetClassList = true)
                }
                is ClassroomManager.ClassroomResponseData.ErrorNetworkDisconnected,
                is ClassroomManager.ClassroomResponseData.ErrorOther -> {
                    _updateUIFlow.emit((ClassUIEvent.ShowRefreshUi))
                    _updateUIFlow.emit(ClassUIEvent.ShowErrorToast(androidContext.getString(R.string.my_class_error_msg_fetch_class)))
                }
                is ClassroomManager.ClassroomResponseData.Success -> {
                    _updateUIFlow.emit(ClassUIEvent.UpdateClassList(responseData.resultData))
                }
                else -> {}
            }
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

    fun createDefaultClass() = createClassroom(androidContext.getString(R.string.my_class_class_default_name))

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
                        _updateUIFlow.emit((ClassUIEvent.ShowRefreshUi))
                    } else {
                        _updateUIFlow.emit(ClassUIEvent.EnableAddClassButton)
                    }
                }
            }
        }
    }

    fun refreshClassPage() {
        // if get class list is success, just create new class
        if (retryGetClassList) {
            getClassroomList()
        } else {
            //新增預設class
            createClassroom(className = androidContext.getString(R.string.my_class_class_default_name), fromGetClassList = true)
        }
    }

    fun updateClassroom(
        classroomId: String,
        updateName: String,
        icon: Int = 0
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            val responseData = classroomManager.updateRooms(classroomId, updateName, icon)
            withContext(Dispatchers.Main) {
                when (responseData) {
                    is ClassroomManager.ClassroomResponseData.Success -> {
                        _updateUIFlow.emit(ClassUIEvent.UpdateClassInfo(responseData.resultData))
                    }
                    else -> {
                        _updateUIFlow.emit(ClassUIEvent.UpdateClassInfoFailed)
                    }
                }
            }
        }

    }

    fun isSocketConnected() = socketManager.isConnected()

    fun createLesson(classroomInfo: ClassroomInfo, tag: WindowTag) {
        coroutineScope.launch(Dispatchers.IO) {
            when (val responseData = classroomManager.createLesson(classroomInfo)) {
                is ClassroomManager.ClassroomResponseData.Success -> {
                    if (classroomInfo.isLessonOnGoing()) {
                        _updateUIFlow.emit(ClassUIEvent.ShouldCheckUnclosedMission)
                    }
                    withContext(Dispatchers.Main) {
                        csWindowManager.hideWindow(WindowTag.WINDOW_QUIZ_COLLECTION, isRecordHiddenState = true)
                        csWindowManager.removeWindow(tag)
                        JoinClassWindowOpener.open(get(JoinClassWindow::class.java))
                    }
                    if (responseData.resultData.isLessonOnGoing()) {
                        toolbarManager.setParticipationState(ToolbarManager.ParticipationState.LESSON_STARTED)
                    } else {
                        toolbarManager.setParticipationState(ToolbarManager.ParticipationState.JOINED)
                        if (myViewBoardConnectionStateProvider.isBound()) {
                            val isSucceeded = classroomManager.startLesson()
                            if (isSucceeded) {
                                toolbarManager.setParticipationState(ToolbarManager.ParticipationState.LESSON_STARTED)
                            }
                        }
                    }
                }
                is ClassroomManager.ClassroomResponseData.ErrorMultipleTeachersInSession -> {
                    _updateUIFlow.emit(ClassUIEvent.ShowErrorToast(androidContext.getString(R.string.my_class_error_msg_join_lesson_conflict)))
                    _updateUIFlow.emit(ClassUIEvent.EnableEnterClassButton)
                }
                else -> {
                    // on going error message is different
                    if (classroomInfo.isLessonOnGoing()) {
                        _updateUIFlow.emit(ClassUIEvent.ShowErrorToast(androidContext.getString(R.string.my_class_error_msg_rejoin_class)))
                    } else {
                        _updateUIFlow.emit(ClassUIEvent.ShowErrorToast(androidContext.getString(R.string.my_class_error_msg_join_class)))
                    }
                    _updateUIFlow.emit(ClassUIEvent.EnableEnterClassButton)
                }
            }
        }
    }

    fun disconnectSocket() {
        accountManager.disconnectSocket()
    }

    fun clearClassroomData() {
        classroomManager.clear()
    }

    fun stopMultipleLoginCheck() {
        accountManager.stopMultipleLoginCheck()
    }

    fun hasRosterClassroom(): Boolean = classroomManager.hasRosterClassroom

    suspend fun isNeedToShowClassPageTutorial(): Boolean = withContext(Dispatchers.IO) {
        if (isMyViewBoardBound()) return@withContext false
        return@withContext !tutorialManager.isClassPagePhaseCompleted() && !tutorialManager.isClassPagePhaseLooked()
    }

    fun setClassPagePhaseLooked() {
        coroutineScope.launch {
            tutorialManager.setClassPagePhaseLooked()
        }
    }

    override fun onCleared() {

    }

    sealed class ClassUIEvent {
        data class UpdateClassList(val classList: List<ClassroomInfo>): ClassUIEvent()
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
    }
}