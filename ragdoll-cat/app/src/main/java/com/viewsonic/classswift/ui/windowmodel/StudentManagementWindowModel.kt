package com.viewsonic.classswift.ui.windowmodel

import com.viewsonic.classswift.data.clientapp.myviewboard.MyViewBoardConnectionStateProvider
import com.viewsonic.classswift.data.datastore.TutorialDataStore
import com.viewsonic.classswift.data.info.StudentDisplayInfo
import com.viewsonic.classswift.data.info.StudentGroupInfo
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.manager.AccountManager
import com.viewsonic.classswift.manager.BatchQuizManager
import com.viewsonic.classswift.manager.ClassroomManager
import com.viewsonic.classswift.manager.CoroutineManager
import com.viewsonic.classswift.manager.NetworkManager
import com.viewsonic.classswift.manager.QuizManager
import com.viewsonic.classswift.manager.StudentManager
import com.viewsonic.classswift.windowframework.core.interfaces.IWindowModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class StudentManagementWindowModel(
    private val studentManager: StudentManager,
    private val accountManager: AccountManager,
    private val classroomManager: ClassroomManager,
    private val tutorialDataStore: TutorialDataStore,
    private val networkManager: NetworkManager,
    private val quizManager: QuizManager,
    private val batchQuizManager: BatchQuizManager,
    private val myViewBoardConnectionStateProvider: MyViewBoardConnectionStateProvider,
) : IWindowModel {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var collectNetworkStateJob: Job? = null
    private var lessonId: String = classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId

    private val _studentManagementUiState = MutableStateFlow(StudentManagementUiState())
    val studentManagementUiState: StateFlow<StudentManagementUiState> = combine(
        _studentManagementUiState,
        studentManager.studentInfoListFlow) { state, studentInfoList ->
            state.copy(
                studentInfoList =  studentInfoList.map { studentInfo -> studentInfo.copy(isEditing = state.isEditing) }
            )
        }.stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(5000L),
            _studentManagementUiState.value
        )

    private val _uiEventFlow = MutableSharedFlow<UiEvent>()
    val uiEventFlow = _uiEventFlow.asSharedFlow()

    init {
        initCollection()
    }

    fun isMyViewBoardBound() = myViewBoardConnectionStateProvider.isBound()

    fun isGuestMode() = accountManager.isGuestMode

    private fun initCollection() {
        collectNetworkStateJob = coroutineScope.launch {
            networkManager.delayInformNetworkAvailabilityState.collect { hasNetwork ->
                _studentManagementUiState.update { it.copy(hasNetwork = hasNetwork) }
            }
        }
    }

    override fun onCleared() {
        collectNetworkStateJob?.cancel()
        coroutineScope.cancel()
    }

    fun getSelectedClassroomInfo() = classroomManager.classroomDataStateFlow.value.selectedClassroomInfo

    fun isSelectedClassroomInfoExisted(): Boolean {
        return classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.isValid()
    }

    fun isStudentExisted(studentInfo: StudentInfo): Boolean {
        return studentManager.getCurrentList().find { it.studentId == studentInfo.studentId } != null
    }

    suspend fun fetchStudentInfoList(): Boolean =
        studentManager.fetchStudentInfoList(lessonId, occupiedOnly = false)

    fun setGroupState(value: Boolean) {
        Timber.d("setGroupState: $value")
        _studentManagementUiState.update { it.copy(isGroup = value) }
    }

    fun setEditState(value: Boolean) {
        Timber.d("setEditState: $value")
        _studentManagementUiState.update {
            it.copy(isEditing = value, studentInfoList = it.studentInfoList.map { studentInfo -> studentInfo.copy(isEditing = value) })
        }
    }

    suspend fun increaseAllStudentsPointByOnePoint(): Boolean {
        classroomManager.updateHasAddedScoreManually(true)
        return studentManager.increaseAllStudentsPointByOnePoint(lessonId)
    }

    suspend fun decreaseAllStudentsPointByOnePoint(): Boolean = studentManager.decreaseAllStudentsPointByOnePoint(lessonId)

    suspend fun increaseSpecificStudentPointByOnePoint(studentId: String): Boolean {
        classroomManager.updateHasAddedScoreManually(true)
        return studentManager.increaseSpecificStudentPointByOnePoint(lessonId, studentId)
    }

    suspend fun decreaseSpecificStudentPointByOnePoint(studentId: String): Boolean = studentManager.decreaseSpecificStudentPointByOnePoint(lessonId, studentId)

    suspend fun increaseGroupPointByOnePoint(studentIdList: List<String>): Boolean {
        classroomManager.updateHasAddedScoreManually(true)
        return studentManager.increaseGroupPointByOnePoint(lessonId, studentIdList)
    }

    suspend fun decreaseGroupPointByOnePoint(studentIdList: List<String>): Boolean = studentManager.decreaseGroupPointByOnePoint(lessonId, studentIdList)

    suspend fun removeStudent(studentId: String): Boolean = studentManager.removeStudent(lessonId, studentId)

    suspend fun setClassPagePhaseCompleted() = withContext(Dispatchers.IO) {
        if (isMyViewBoardBound()) return@withContext
        accountManager.userInfo.userId.takeIf { it.isNotEmpty() }?.let { userId ->
            tutorialDataStore.setClassPagePhaseCompletion(userId, true)
        }
    }

    suspend fun setStudentPagePhaseCompleted() = withContext(Dispatchers.IO) {
        accountManager.userInfo.userId.takeIf { it.isNotEmpty() }?.let { userId ->
            tutorialDataStore.setStudentPagePhaseCompletion(userId, true)
        }
    }

    suspend fun isStudentPagePhaseCompleted(): Boolean = withContext(Dispatchers.IO) {
        if (isMyViewBoardBound()) return@withContext true
        val userId = accountManager.userInfo.userId
        return@withContext tutorialDataStore.isStudentPagePhaseCompleted(userId)
    }

    data class StudentManagementUiState(
        val isEditing: Boolean = false,
        val isGroup: Boolean = false,
        val hasNetwork: Boolean = true,
        val studentInfoList: List<StudentInfo> = emptyList()
    ) {
        fun toStudentDisplayInfo(): StudentDisplayInfo {
            return StudentDisplayInfo(
                isGroup = isGroup,
                studentList = studentInfoList,
                groupList = studentInfoList.groupBy { it.groupId }.map { (groupId, students) -> StudentGroupInfo(isEditing, groupId, ArrayList(students)) }
            )
        }
    }

    sealed class UiEvent {
        data object StartBatchQuizStartWindow : UiEvent()
        data object StartBatchQuizResultWindow : UiEvent()
    }

}