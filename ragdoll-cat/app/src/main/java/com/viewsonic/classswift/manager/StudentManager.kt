package com.viewsonic.classswift.manager

import android.content.Context
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.LessonApiService
import com.viewsonic.classswift.api.body.BatchUpdateStudentPointBody
import com.viewsonic.classswift.api.body.BatchUpdateStudentPointBody.StudentPoint
import com.viewsonic.classswift.api.body.UpdateMultipleStudentPointsBody
import com.viewsonic.classswift.api.body.UpdateStudentPointBody
import com.viewsonic.classswift.api.body.UpdateStudentPointsBody
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.info.StudentInfo.ParticipationState
import com.viewsonic.classswift.data.info.StudentInfo.Status
import com.viewsonic.classswift.data.socket.ChooseAndRejoinSeatSocketMessage
import com.viewsonic.classswift.data.socket.ReleaseSeatSocketMessage
import com.viewsonic.classswift.data.socket.SetStudentNameSocketMessage
import com.viewsonic.classswift.utils.extension.localizedContext
import com.viewsonic.classswift.utils.extension.repeatWithDelay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class StudentManager(
    private val applicationContext: Context,
    private val accountManager: AccountManager,
    private val socketManager: SocketManager,
    private val classroomManager: ClassroomManager,
    private val lessonApiService: LessonApiService
) {
    private val coroutineScope: CoroutineScope = CoroutineManager.getScope(this)
    private var collectSocketReceivedEventJob: Job? = null
    private var collectSocketConnectionStateJob: Job? = null
    private var studentInfoPollingJob: Job? = null

    private val _studentInfoListFlow = MutableStateFlow<List<StudentInfo>>(emptyList())
    val studentInfoListFlow = _studentInfoListFlow.asStateFlow()

    /** 座位相關 socket event（choose/rejoin/release/setName）發生時 emit；
     *  外層 Window 可訂閱並依需求決定是否 re-fetch。 */
    private val _studentSeatEventFlow = MutableSharedFlow<Unit>()
    val studentSeatEventFlow = _studentSeatEventFlow.asSharedFlow()

    private val _studentChangeReasonFlow = MutableSharedFlow<StudentChangeReason>()
    val studentChangeReasonFlow = _studentChangeReasonFlow.asSharedFlow()

    fun getCurrentList(): List<StudentInfo> = studentInfoListFlow.value.toList()

    fun getCurrentAttendantList(): List<StudentInfo> = studentInfoListFlow.value.filter { studentInfo ->
        studentInfo.status == Status.ACTIVE
    }

    /**
     * 學生清單取得。
     *
     * @param lessonId 課堂 ID。
     * @param isFromSocketReconnection true 時，fetch 成功後發出 [StudentChangeReason.SocketReconnected]。
     * @param occupiedOnly true → 後端只回傳已佔用座位（`occupied_only=true`），
     *   [updateList] 以 fullReplace 全量替換；false → 回傳完整名冊，以 merge 更新。
     *   **必填**，每個 caller 需明確宣告意圖。
     */
    suspend fun fetchStudentInfoList(
        lessonId: String,
        isFromSocketReconnection: Boolean = false,
        occupiedOnly: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val defaultStudentDisplayNameMap: HashMap<Int, String> = hashMapOf()
        val defaultStudentDisplaySeatNumberMap: HashMap<Int, String> = hashMapOf()

        val isGetLessonSuccess = getLessonInfo(
            studentDisplayNameMap = defaultStudentDisplayNameMap,
            studentDisplaySeatNumberMap = defaultStudentDisplaySeatNumberMap
        )

        if (!isGetLessonSuccess) {
            false
        } else {
            getStudentList(
                token = accountManager.getBearerToken(),
                lessonId = lessonId,
                studentDisplayNameMap = defaultStudentDisplayNameMap,
                studentDisplaySeatNumberMap = defaultStudentDisplaySeatNumberMap,
                isFromSocketReconnection = isFromSocketReconnection,
                occupiedOnly = occupiedOnly
            )
        }
    }

    private suspend fun getLessonInfo(
        studentDisplayNameMap: HashMap<Int, String>, //defaultStudentDisplayNameMap
        studentDisplaySeatNumberMap: HashMap<Int, String> //defaultStudentDisplaySeatNumberMap
    ): Boolean {
        return when (val response = classroomManager.getLessonInfo()) {
            is ApiResponse.Success -> {
                response.data.defaultStudentDetailList.forEach {
                    if (it.displayName.isNotEmpty()) {
                        studentDisplayNameMap[it.serialNumber] = it.displayName
                    }
                    if (it.seatNumber.isNotEmpty()) {
                        studentDisplaySeatNumberMap[it.serialNumber] = it.displayName
                    }
                }
                true
            }
            else -> {
                false
            }
        }
    }

    private suspend fun getStudentList(
        token: String,
        lessonId: String,
        studentDisplayNameMap: HashMap<Int, String>, //defaultStudentDisplayNameMap
        studentDisplaySeatNumberMap: HashMap<Int, String>, //defaultStudentDisplaySeatNumberMap
        isFromSocketReconnection: Boolean,
        occupiedOnly: Boolean
    ): Boolean {
        val response = lessonApiService.getStudentList(
            token = token,
            lessonId = lessonId,
            occupiedOnly = occupiedOnly
        )

        return when (response) {
            is ApiResponse.Success -> {
                val studentInfoList = response.data.attendedStudentDetailList.map {
                    var studentInfo = StudentInfo.fromAttendedStudentDetail(it)

                    studentDisplayNameMap[it.serialNumber]?.let { displayName ->
                        studentInfo = studentInfo.copy(defaultDisplayName = displayName)
                    }

                    studentDisplaySeatNumberMap[it.serialNumber]?.let { displaySeatNumber ->
                        studentInfo = studentInfo.copy(defaultDisplaySeatNumber = displaySeatNumber)
                    }
                    studentInfo
                }
                val newStudentList = updateList(studentInfoList, fullReplace = occupiedOnly)

                if (isFromSocketReconnection) {
                    val oldStudentList = getCurrentList()
                    updateStudentChangeReason(
                        StudentChangeReason.SocketReconnected(
                            oldStudentList,
                            newStudentList
                        )
                    )
                }
                true
            }
            else -> {
                false
            }
        }
    }

    private fun updateList(
        newList: List<StudentInfo>,
        fullReplace: Boolean = false
    ): List<StudentInfo> {
        val result = buildMergedList(studentInfoListFlow.value, newList, fullReplace)
        _studentInfoListFlow.update { result }
        return result
    }

    fun clearStudentList() {
        _studentInfoListFlow.update {
            emptyList()
        }
    }

    suspend fun increaseAllStudentsPointByOnePoint(lessonId: String): Boolean =
        withContext(Dispatchers.IO) {
            val currentList = studentInfoListFlow.value.toMutableList()
            val updateAllStudentsPointResponse = lessonApiService.updateAllStudentsPoint(
                accountManager.getBearerToken(),
                lessonId,
                UpdateStudentPointBody(point = 1)
            )
            return@withContext when (updateAllStudentsPointResponse) {
                is ApiResponse.Success -> {
                    val updatedStudentInfoList = ArrayList<StudentInfo>()
                    updateAllStudentsPointResponse.data.studentPointResponseDataList.forEach { studentPointResponseData ->
                        val index =
                            currentList.indexOfFirst { it.studentId == studentPointResponseData.studentId }
                        if (index >= 0) {
                            val oldStudent = currentList[index]
                            updatedStudentInfoList.add(
                                oldStudent.copy(
                                    studentId = studentPointResponseData.studentId,
                                    points = studentPointResponseData.totalPoints
                                )
                            )
                        }
                    }
                    if (updatedStudentInfoList.isNotEmpty()) {
                        updateList(updatedStudentInfoList)
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }

    suspend fun decreaseAllStudentsPointByOnePoint(lessonId: String): Boolean =
        withContext(Dispatchers.IO) {
            val currentList = studentInfoListFlow.value.toMutableList()
            val updateAllStudentsPointResponse = lessonApiService.updateAllStudentsPoint(
                accountManager.getBearerToken(),
                lessonId,
                UpdateStudentPointBody(point = -1)
            )
            return@withContext when (updateAllStudentsPointResponse) {
                is ApiResponse.Success -> {
                    val updatedStudentInfoList = ArrayList<StudentInfo>()
                    updateAllStudentsPointResponse.data.studentPointResponseDataList.forEach { studentPointResponseData ->
                        val index =
                            currentList.indexOfFirst { it.studentId == studentPointResponseData.studentId }
                        if (index >= 0) {
                            val oldStudent = currentList[index]
                            updatedStudentInfoList.add(
                                oldStudent.copy(
                                    studentId = studentPointResponseData.studentId,
                                    points = studentPointResponseData.totalPoints
                                )
                            )
                        }
                    }
                    if (updatedStudentInfoList.isNotEmpty()) {
                        updateList(updatedStudentInfoList)
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }

    suspend fun increaseSpecificStudentPointByOnePoint(
        lessonId: String,
        studentId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val currentList = studentInfoListFlow.value.toMutableList()
        val updateAllStudentsPointResponse = lessonApiService.updateStudentPoint(
            accountManager.getBearerToken(),
            lessonId,
            studentId,
            UpdateStudentPointsBody(points = 1)
        )
        return@withContext when (updateAllStudentsPointResponse) {
            is ApiResponse.Success -> {
                val updatedStudentInfoList = ArrayList<StudentInfo>()
                currentList.find { it.studentId == updateAllStudentsPointResponse.data.studentPointResponseData.studentId }
                    ?.let { oldStudentInfo ->
                        updatedStudentInfoList.add(
                            oldStudentInfo.copy(
                                studentId = updateAllStudentsPointResponse.data.studentPointResponseData.studentId,
                                points = updateAllStudentsPointResponse.data.studentPointResponseData.totalPoints
                            )
                        )
                    }
                if (updatedStudentInfoList.isNotEmpty()) {
                    updateList(updatedStudentInfoList)
                }
                true
            }
            else -> {
                false
            }
        }
    }

    suspend fun decreaseSpecificStudentPointByOnePoint(
        lessonId: String,
        studentId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val currentList = studentInfoListFlow.value.toMutableList()
        val updateAllStudentsPointResponse = lessonApiService.updateStudentPoint(
            accountManager.getBearerToken(),
            lessonId,
            studentId,
            UpdateStudentPointsBody(points = -1)
        )
        return@withContext when (updateAllStudentsPointResponse) {
            is ApiResponse.Success -> {
                val updatedStudentInfoList = ArrayList<StudentInfo>()
                currentList.find { it.studentId == updateAllStudentsPointResponse.data.studentPointResponseData.studentId }
                    ?.let { oldStudentInfo ->
                        updatedStudentInfoList.add(
                            oldStudentInfo.copy(
                                studentId = updateAllStudentsPointResponse.data.studentPointResponseData.studentId,
                                points = updateAllStudentsPointResponse.data.studentPointResponseData.totalPoints
                            )
                        )
                    }
                if (updatedStudentInfoList.isNotEmpty()) {
                    updateList(updatedStudentInfoList)
                }
                true
            }
            else -> {
                false
            }
        }
    }

    suspend fun increaseGroupPointByOnePoint(
        lessonId: String,
        studentIdList: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        val currentList = studentInfoListFlow.value.toMutableList()
        val updateAllStudentsPointResponse = lessonApiService.updateMultipleStudentsPoint(
            accountManager.getBearerToken(),
            lessonId,
            UpdateMultipleStudentPointsBody(studentIdList, points = 1)
        )
        return@withContext when (updateAllStudentsPointResponse) {
            is ApiResponse.Success -> {
                val updatedStudentInfoList = ArrayList<StudentInfo>()
                studentIdList.forEach { studentID ->
                    val index = currentList.indexOfFirst { it.studentId == studentID }
                    if (index >= 0) {
                        val oldStudent = currentList[index]
                        updatedStudentInfoList.add(
                            oldStudent.copy(
                                studentId = studentID,
                                points = oldStudent.points + 1
                            )
                        )
                    }
                }
                if (updatedStudentInfoList.isNotEmpty()) {
                    updateList(updatedStudentInfoList)
                }
                true
            }
            else -> {
                false
            }
        }
    }

    suspend fun decreaseGroupPointByOnePoint(
        lessonId: String,
        studentIdList: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        val currentList = studentInfoListFlow.value.toMutableList()
        val updateAllStudentsPointResponse = lessonApiService.updateMultipleStudentsPoint(
            accountManager.getBearerToken(),
            lessonId,
            UpdateMultipleStudentPointsBody(studentIdList, points = -1)
        )
        return@withContext when (updateAllStudentsPointResponse) {
            is ApiResponse.Success -> {
                val updatedStudentInfoList = ArrayList<StudentInfo>()
                studentIdList.forEach { studentID ->
                    val index = currentList.indexOfFirst { it.studentId == studentID }
                    if (index >= 0) {
                        val oldStudent = currentList[index]
                        updatedStudentInfoList.add(
                            oldStudent.copy(
                                studentId = studentID,
                                points = oldStudent.points - 1
                            )
                        )
                    }
                }
                if (updatedStudentInfoList.isNotEmpty()) {
                    updateList(updatedStudentInfoList)
                }
                true
            }
            else -> {
                false
            }
        }
    }

    suspend fun batchUpdateStudentPoint(
        lessonId: String,
        studentPoints: Map<String, Int>
    ): Boolean = withContext(Dispatchers.IO) {
        val studentPointList = studentPoints.map { (studentId, points) ->
            StudentPoint(studentId = studentId, points = points)
        }
        val currentList = studentInfoListFlow.value.toMutableList()
        val batchUpdateStudentPointResponse = lessonApiService.batchUpdateStudentPoint(
            accountManager.getBearerToken(),
            lessonId,
            BatchUpdateStudentPointBody(studentPointList)
        )
        return@withContext when (batchUpdateStudentPointResponse) {
            is ApiResponse.Success -> {
                val updatedStudentInfoList = ArrayList<StudentInfo>()
                studentPointList.forEach { studentPoint ->
                    val index = currentList.indexOfFirst { it.studentId == studentPoint.studentId }
                    if (index >= 0) {
                        val oldStudent = currentList[index]
                        updatedStudentInfoList.add(
                            oldStudent.copy(
                                studentId = studentPoint.studentId,
                                points = oldStudent.points + studentPoint.points
                            )
                        )
                    }
                }
                if (updatedStudentInfoList.isNotEmpty()) {
                    updateList(updatedStudentInfoList)
                }
                true
            }
            else -> {
                false
            }
        }
    }

    suspend fun removeStudent(lessonId: String, studentId: String): Boolean =
        withContext(Dispatchers.IO) {
            val response =
                lessonApiService.removeStudent(accountManager.getBearerToken(), lessonId, studentId)
            return@withContext when (response) {
                is ApiResponse.Success -> true
                else -> {
                    false
                }
            }
        }

    private fun updateStudent(
        socketMessage: ChooseAndRejoinSeatSocketMessage,
        isRejoined: Boolean
    ) {
        val currentList = studentInfoListFlow.value.toMutableList()
        if (socketMessage.serialNumber >= 0) {
            val index = currentList.indexOfFirst { it.serialNumber == socketMessage.serialNumber }
            if (index >= 0) {
                val oldStudent = currentList[index]
                val newStudent = oldStudent.copy(
                    studentId = socketMessage.studentId,
                    displayName = socketMessage.displayName,
                    displaySeatNumber = socketMessage.seatNumber,
                    status = Status.ACTIVE,
                    points = socketMessage.points
                )
                currentList[index] = newStudent
                _studentInfoListFlow.update {
                    currentList
                }
                if (isRejoined) {
                    updateStudentChangeReason(
                        StudentChangeReason.RejoinSeat(
                            oldStudent,
                            newStudent
                        )
                    )
                } else {
                    updateStudentChangeReason(
                        // displayName is empty means guest choose seat, otherwise means register student be set seat won't have SetStudentName event.
                        if (newStudent.displayName.isEmpty())
                            StudentChangeReason.ChooseSeat(
                                oldStudent,
                                newStudent
                            )
                        else
                            StudentChangeReason.SetStudentName(
                                oldStudent,
                                newStudent
                            )
                    )
                }
            }
        }
    }

    private fun updateStudent(socketMessage: ReleaseSeatSocketMessage) {
        val currentList = studentInfoListFlow.value.toMutableList()
        if (socketMessage.serialNumber >= 0) {
            val index = currentList.indexOfFirst { it.serialNumber == socketMessage.serialNumber }
            if (index >= 0) {
                val oldStudent = currentList[index]
                val newStudent = oldStudent.copy(
                    studentId = "",
                    displayName = "",
                    displaySeatNumber = socketMessage.seatNumber,
                    status = Status.INACTIVE,
                    points = 0
                )
                currentList[index] = newStudent
                _studentInfoListFlow.update {
                    currentList
                }
                updateStudentChangeReason(StudentChangeReason.ReleaseSeat(oldStudent, newStudent))
            }
        }
    }

    private fun updateStudent(socketMessage: SetStudentNameSocketMessage) {
        val currentList = studentInfoListFlow.value.toMutableList()
        if (socketMessage.serialNumber >= 0) {
            val index = currentList.indexOfFirst { it.serialNumber == socketMessage.serialNumber }
            if (index >= 0) {
                val oldStudent = currentList[index]
                val newStudent = oldStudent.copy(
                    studentId = socketMessage.studentId,
                    displayName = socketMessage.displayName.takeIf { it.isNotEmpty() }
                        ?: applicationContext.localizedContext().getString(R.string.common_guest),
                )
                currentList[index] = newStudent
                _studentInfoListFlow.update {
                    currentList
                }
                updateStudentChangeReason(
                    StudentChangeReason.SetStudentName(
                        oldStudent,
                        newStudent
                    )
                )
            }
        }
    }

    /**
     * 本地 socket event 處理：收到座位相關 event 後直接更新 [studentInfoListFlow]。
     * 不發出任何 re-fetch 請求；呼叫方（[initCollection]）負責在此後
     * 透過 [studentSeatEventFlow] 通知外層訂閱者。
     */
    private fun handleStudentSocketEventLocally(receivedEventData: SocketManager.ReceivedEventData) {
        when (receivedEventData.event) {
            SocketManager.ReceivedEvent.EVENT_CHOOSE_SEAT -> {
                receivedEventData.messageJsonObject?.let {
                    updateStudent(ChooseAndRejoinSeatSocketMessage.fromJSONObject(it), isRejoined = false)
                }
            }
            SocketManager.ReceivedEvent.EVENT_REJOIN_SEAT -> {
                receivedEventData.messageJsonObject?.let {
                    updateStudent(ChooseAndRejoinSeatSocketMessage.fromJSONObject(it), isRejoined = true)
                }
            }
            SocketManager.ReceivedEvent.EVENT_RELEASE_SEAT -> {
                receivedEventData.messageJsonObject?.let {
                    updateStudent(ReleaseSeatSocketMessage.fromJSONObject(it))
                }
            }
            SocketManager.ReceivedEvent.EVENT_SET_STUDENT_NAME -> {
                receivedEventData.messageJsonObject?.let {
                    updateStudent(SetStudentNameSocketMessage.fromJSONObject(it))
                }
            }
            else -> Unit
        }
    }

    private fun initCollection() {
        collectSocketReceivedEventJob?.cancel()
        collectSocketReceivedEventJob = coroutineScope.launch(Dispatchers.IO) {
            socketManager.receivedEventDataFlow.distinctUntilChanged()
                .collect { receivedEventData ->
                    Timber.d("[B][onReceived] : receivedEventData -> $receivedEventData")
                    when (receivedEventData.event) {
                        SocketManager.ReceivedEvent.EVENT_CHOOSE_SEAT,
                        SocketManager.ReceivedEvent.EVENT_REJOIN_SEAT,
                        SocketManager.ReceivedEvent.EVENT_RELEASE_SEAT,
                        SocketManager.ReceivedEvent.EVENT_SET_STUDENT_NAME -> {
                            handleStudentSocketEventLocally(receivedEventData)
                            // 通知外層（如 JoinClassWindowModel）有座位變動，
                            // 由外層決定是否 re-fetch 及使用哪種 occupiedOnly 設定。
                            coroutineScope.launch { _studentSeatEventFlow.emit(Unit) }
                        }

                        else -> {}
                    }
                }
        }
        collectSocketConnectionStateJob?.cancel()
        collectSocketConnectionStateJob = coroutineScope.launch(Dispatchers.IO) {
            socketManager.connectionStateSharedFlow.collect { connectionState ->
                classroomManager.classroomDataStateFlow.value.selectedClassroomInfo.lessonId.takeIf { it.isNotEmpty() }?.let { lessonId ->
                    when (socketManager.isConnected()) {
                        true -> {
                            studentInfoPollingJob?.cancel()
                            fetchStudentInfoList(lessonId, true, occupiedOnly = false)
                        }

                        false -> {
                            studentInfoPollingJob?.cancel()
                            studentInfoPollingJob = coroutineScope.repeatWithDelay(5000) {
                                fetchStudentInfoList(lessonId, true, occupiedOnly = false)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateStudentChangeReason(reason: StudentChangeReason) {
        coroutineScope.launch(Dispatchers.IO) {
            _studentChangeReasonFlow.emit(reason)
        }
    }

    // condition: joined student > 2 and at least one student be add pointed.
    fun showLeaderboard(): Boolean {
        studentInfoListFlow.value.toList().apply {
            val joinedCount = count { it.getParticipationState() == ParticipationState.JOINED }
            val hasPositivePoints = any { it.points > 0 }
            return joinedCount > 2 && hasPositivePoints
        }
    }

    fun setStartLessonState() {
        clearStudentList()
        initCollection()
    }

    fun setEndLessonState() {
        //stop to collect event
        studentInfoPollingJob?.cancel()
        collectSocketConnectionStateJob?.cancel()
        collectSocketReceivedEventJob?.cancel()
    }

    sealed class StudentChangeReason {
        data class ChooseSeat(val oldStudent: StudentInfo, val newStudent: StudentInfo) :
            StudentChangeReason()

        data class RejoinSeat(val oldStudent: StudentInfo, val newStudent: StudentInfo) :
            StudentChangeReason()

        data class ReleaseSeat(val oldStudent: StudentInfo, val newStudent: StudentInfo) :
            StudentChangeReason()

        data class SetStudentName(val oldStudent: StudentInfo, val newStudent: StudentInfo) :
            StudentChangeReason()

        data class SocketReconnected(
            val oldStudents: List<StudentInfo>,
            val newStudents: List<StudentInfo>
        ) : StudentChangeReason()
    }

    companion object {

        /**
         * 合併或全量替換學生清單的純函式（不依賴任何 instance state）。
         *
         * - fullReplace=true  : 忽略 [currentList]，以 [newList] 完整取代
         * - fullReplace=false : 以 [StudentInfo.serialNumber] 為 key 做 merge，
         *   保留 [currentList] 中未出現在 [newList] 的項目
         *
         * 結果依 [StudentInfo.serialNumber] 升序排列；
         * serialNumber < 0 或無 displayName / displaySeatNumber 的項目排到最後。
         */
        internal fun buildMergedList(
            currentList: List<StudentInfo>,
            newList: List<StudentInfo>,
            fullReplace: Boolean
        ): List<StudentInfo> {
            val base = if (fullReplace) mutableListOf() else currentList.toMutableList()
            newList.forEach { newInfo ->
                base.removeIf { it.serialNumber == newInfo.serialNumber }
                base.add(newInfo)
            }
            base.sortBy {
                when {
                    it.serialNumber < 0 -> Int.MAX_VALUE
                    it.displaySeatNumber.isEmpty() && it.displayName.isEmpty() -> Int.MAX_VALUE
                    else -> it.serialNumber
                }
            }
            return base
        }
    }
}