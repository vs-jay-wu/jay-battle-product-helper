package com.viewsonic.classswift.manager

import com.viewsonic.classswift.api.ClassroomApiService
import com.viewsonic.classswift.api.LessonApiService
import com.viewsonic.classswift.constant.ApiConstant
import com.viewsonic.classswift.api.body.CreateRoomBody
import com.viewsonic.classswift.api.body.GuestRoomBody
import com.viewsonic.classswift.api.body.UpdateRoomBody
import com.viewsonic.classswift.api.response.LessonResponse
import com.viewsonic.classswift.api.retrofit.ApiResponse
import com.viewsonic.classswift.builder.AmplitudeEventBuilder
import com.viewsonic.classswift.constant.AmplitudeConstant
import com.viewsonic.classswift.data.enum.MissionType
import com.viewsonic.classswift.data.info.ClassroomInfo
import com.viewsonic.classswift.data.socket.JoinLessonSocketMessage
import com.viewsonic.classswift.factory.AmplitudeFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ClassroomManager(
    private val accountManager: AccountManager,
    private val socketManager: SocketManager,
    private val classroomApiService: ClassroomApiService,
    private val lessonApiService: LessonApiService
) {
    private val studentManager: StudentManager by inject(StudentManager::class.java)
    private val _classroomDataStateFlow = MutableStateFlow(ClassroomDataState())
    val classroomDataStateFlow = _classroomDataStateFlow.asStateFlow()

    /**
     * Check whether there are any rostered classrooms in the current classroom list.
     */
    var hasRosterClassroom: Boolean = false
        private set

    suspend fun getUnclosedMission(): MissionType = withContext(Dispatchers.IO) {
        return@withContext when (val response = classroomApiService.getUnclosedMissions(accountManager.getBearerToken(), classroomDataStateFlow.value.selectedClassroomInfo.lessonId)) {
            is ApiResponse.Success -> {
                response.data.data.missionType
            }
            else -> MissionType.NONE
        }
    }

    suspend fun getClassroomList(orgId: String, userId: String): ClassroomResponseData<List<ClassroomInfo>> = withContext(Dispatchers.IO) {
        when (
            val apiResponse = classroomApiService.getRooms(
                accountManager.getBearerToken(),
                orgId,
                userId,
                sortType = ApiConstant.RoomSortType.LATEST_ACTIVITY.queryValue,
            )
        ) {
            is ApiResponse.Rfc7807Failure -> {
                if (apiResponse.responseCode == 404) {
                    // 目前404都代表沒有class資料,以後可能會有別的狀態
                    return@withContext ClassroomResponseData.ErrorClassroomNotFound()
                }
                return@withContext ClassroomResponseData.ErrorOther()
            }

            is ApiResponse.NetworkDisconnected -> {
                return@withContext ClassroomResponseData.ErrorNetworkDisconnected()
            }

            is ApiResponse.Success -> {
                val resultList = apiResponse.data.roomsData.map {
                    ClassroomInfo.fromApiRoomData(it)
                }
                hasRosterClassroom = resultList.any { it.originId.isNotEmpty() }
                _classroomDataStateFlow.update {
                    it.copy(
                        classroomList = resultList
                    )
                }
                return@withContext ClassroomResponseData.Success(resultList)
            }

            else -> {
                return@withContext ClassroomResponseData.ErrorOther()
            }
        }
    }

    suspend fun deleteClassroom(roomId: String): ClassroomResponseData<Boolean> = withContext(Dispatchers.IO) {
        when (classroomApiService.deleteRooms(accountManager.getBearerToken(), roomId)) {
            is ApiResponse.NetworkDisconnected -> {
                return@withContext ClassroomResponseData.ErrorNetworkDisconnected()
            }

            is ApiResponse.Success -> {
                return@withContext ClassroomResponseData.Success(true)
            }
            else -> {
                return@withContext ClassroomResponseData.ErrorOther()
            }
        }
    }

    suspend fun createClassroom(
        body: CreateRoomBody,
        studentList: Boolean = true,
        grouping: Boolean = true
    ): ClassroomResponseData<ClassroomInfo> = withContext(Dispatchers.IO) {
        when (val apiResponse = classroomApiService.createRooms(
            accountManager.getBearerToken(),
            studentList,
            grouping,
            body
        )) {
            is ApiResponse.NetworkDisconnected -> {
                return@withContext ClassroomResponseData.ErrorNetworkDisconnected()
            }

            is ApiResponse.Success -> {
                return@withContext ClassroomResponseData.Success(
                    ClassroomInfo(
                        id = apiResponse.data.createRoomData.roomId,
                        number = apiResponse.data.createRoomData.roomNumber,
                        roomLink = apiResponse.data.createRoomData.roomLink,
                        displayName = body.displayName,
                        maxStudentCount = accountManager.selectedOrg?.studentConcurrent ?: 0
                    )
                )
            }
            else -> {
                return@withContext ClassroomResponseData.ErrorOther()
            }
        }
    }

    suspend fun updateRooms(
        roomId: String,
        updateName: String,
        icon: Int = 0
    ): ClassroomResponseData<ClassroomInfo> = withContext(Dispatchers.IO) {
        when (val apiResponse = classroomApiService.updateRooms(accountManager.getBearerToken(), roomId, UpdateRoomBody(updateName, icon))) {
            is ApiResponse.NetworkDisconnected -> {
                return@withContext ClassroomResponseData.ErrorNetworkDisconnected()
            }

            is ApiResponse.Success -> {
                return@withContext ClassroomResponseData.Success(
                    ClassroomInfo.fromApiRoomData(apiResponse.data.updateData)
                )
            }

            else -> {
                return@withContext ClassroomResponseData.ErrorOther()
            }
        }
    }

    suspend fun createLesson(classroomInfo: ClassroomInfo): ClassroomResponseData<ClassroomInfo> = withContext(Dispatchers.IO) {
        when (val apiResponse = classroomApiService.createLesson(accountManager.getBearerToken(), classroomInfo.id)) {
            is ApiResponse.NetworkDisconnected -> {
                return@withContext ClassroomResponseData.ErrorNetworkDisconnected()
            }

            is ApiResponse.Success -> {
                // userInfo.userId, not userOrgInfo.userId: guest login only fills userInfo,
                // and SocketManager.connect() uses the same source — keep them consistent so the
                // server can route per-user socket broadcasts to guest teachers.
                val joinLessonMessage = JoinLessonSocketMessage(
                    userId = accountManager.userInfo.userId,
                    lessonId = apiResponse.data.lessonData.lessonId
                )
                socketManager.setJoinLessonMessage(joinLessonMessage)
                socketManager.emit(
                    SocketManager.EmittedEvent.JOIN_LESSON,
                    joinLessonMessage.toJSONObject()
                )
                val resultClassroomInfo = classroomInfo.copy(
                    lessonId = apiResponse.data.lessonData.lessonId
                )
                _classroomDataStateFlow.update {
                    it.copy(
                        selectedClassroomInfo = resultClassroomInfo
                    )
                }
                studentManager.setStartLessonState()
                return@withContext ClassroomResponseData.Success(resultClassroomInfo)
            }

            is ApiResponse.Rfc7807Failure<*> -> {
                if (apiResponse.responseCode == 409) {
                    return@withContext ClassroomResponseData.ErrorMultipleTeachersInSession()
                }
                return@withContext ClassroomResponseData.ErrorOther()
            }

            else -> {
                return@withContext ClassroomResponseData.ErrorOther()
            }
        }
    }

    suspend fun startLesson(): Boolean = withContext(Dispatchers.IO) {
        if (!classroomDataStateFlow.value.selectedClassroomInfo.isValid()) {
            Timber.e("[B][startLesson] : selectedClassroomInfo isn't valid")
            return@withContext false
        }
        val lessonId = classroomDataStateFlow.value.selectedClassroomInfo.lessonId
        if (lessonId.isEmpty()) {
            Timber.e("[B][startLesson] : selectedClassroomInfo lessonId is empty")
            return@withContext false
        }

        return@withContext when (val response = lessonApiService.startLesson(accountManager.getBearerToken(), lessonId)) {
            is ApiResponse.Success -> {
                Timber.d("[B][startLesson] : response = ${response.data}")
                response.data.informationList.firstOrNull()?.let { lessonInfo ->
                    _classroomDataStateFlow.update {
                        it.copy(
                            selectedClassroomInfo = it.selectedClassroomInfo.copy(
                                lessonStartTime = lessonInfo.startTime
                            )
                        )
                    }
                }
                AmplitudeEventBuilder(AmplitudeConstant.EventName.LESSON_START)
                    .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
                    .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
                    .send()
                response.data.result == "success"
            }
            else -> false
        }
    }

    suspend fun endLesson(): Boolean = withContext(Dispatchers.IO) {
        if (!classroomDataStateFlow.value.selectedClassroomInfo.isValid()) {
            Timber.e("[B][endLesson] : selectedClassroomInfo isn't valid")
            return@withContext false
        }
        val lessonId = classroomDataStateFlow.value.selectedClassroomInfo.lessonId
        if (lessonId.isEmpty()) {
            Timber.e("[B][endLesson] : selectedClassroomInfo lessonId is empty")
            return@withContext false
        }

        val response = lessonApiService.endLesson(accountManager.getBearerToken(), lessonId)
        return@withContext when (response) {
            is ApiResponse.Success -> {
                studentManager.setEndLessonState()
                AmplitudeEventBuilder(AmplitudeConstant.EventName.LESSON_END)
                    .appendEventProperty(AmplitudeFactory.EventPropertyType.ROOM_DATA)
                    .appendEventProperty(AmplitudeFactory.EventPropertyType.LESSON_DATA)
                    .send()
                true
            }
            else -> false
        }
    }

    suspend fun getLessonInfo(): ApiResponse<LessonResponse> = withContext(Dispatchers.IO) {
        val lessonId = classroomDataStateFlow.value.selectedClassroomInfo.lessonId
        val response = lessonApiService.getLessonInfo(accountManager.getBearerToken(), lessonId)
        if (response is ApiResponse.Success) {
            _classroomDataStateFlow.update {
                it.copy(
                    selectedClassroomInfo = it.selectedClassroomInfo.copy(
                        lessonStartTime = response.data.startTimeInSeconds.toLong()
                    )
                )
            }
        }
        return@withContext response
    }

    fun getCurrentTime(): String {
        val current = LocalDateTime.now()

        // 格式說明：
        // MMM -> 月份簡寫 (Feb)
        // d   -> 日期 (23)
        // h:mm -> 小時:分鐘 (8:30)
        // a   -> 上下午標記 (AM/PM)
        val formatter = DateTimeFormatter.ofPattern("MMM d h:mma", Locale.ENGLISH)

        return current.format(formatter)
    }

    suspend fun getGuestClassroomList(): ClassroomResponseData<List<ClassroomInfo>> =
        withContext(Dispatchers.IO) {
            val roomDisplayName = getCurrentTime()
            when (val apiResponse = classroomApiService.getGuestRoom(
                accountManager.getBearerToken(),
                GuestRoomBody(displayName = roomDisplayName)
            )) {
                is ApiResponse.Rfc7807Failure -> {
                    if (apiResponse.responseCode == 404) {
                        // 目前404都代表沒有class資料,以後可能會有別的狀態
                        return@withContext ClassroomResponseData.ErrorClassroomNotFound()
                    }
                    return@withContext ClassroomResponseData.ErrorOther()
                }

                is ApiResponse.NetworkDisconnected -> {
                    return@withContext ClassroomResponseData.ErrorNetworkDisconnected()
                }

                is ApiResponse.Success -> {
                    val resultList = listOfNotNull(apiResponse.data.roomData).map {
                        ClassroomInfo.fromApiGuestRoomData(
                            it,
                            roomDisplayName,
                            accountManager.guestOrgInfo.studentConcurrent
                        )
                    }
                    hasRosterClassroom = resultList.any { it.originId.isNotEmpty() }
                    _classroomDataStateFlow.update {
                        it.copy(
                            classroomList = resultList
                        )
                    }
                    return@withContext ClassroomResponseData.Success(resultList)
                }

                else -> {
                    return@withContext ClassroomResponseData.ErrorOther()
                }
            }
        }

    fun updateHasAddedScoreManually(hasAdded: Boolean) {
        _classroomDataStateFlow.update {
            it.copy(
                selectedClassroomInfo = it.selectedClassroomInfo.copy(
                    hasAddedScoreManually = hasAdded
                )
            )
        }
    }

    fun clear() {
        _classroomDataStateFlow.update { ClassroomDataState() }
    }

    data class ClassroomDataState(
        val classroomList: List<ClassroomInfo> = emptyList(),
        val selectedClassroomInfo: ClassroomInfo = ClassroomInfo()
    )

    sealed class ClassroomResponseData<D> {
        data class Success<D>(val resultData: D): ClassroomResponseData<D>()
        class ErrorClassroomNotFound<D>: ClassroomResponseData<D>()
        class ErrorNetworkDisconnected<D>: ClassroomResponseData<D>()
        class ErrorMultipleTeachersInSession<D>: ClassroomResponseData<D>()
        data class ErrorOther<D>(val message: String = ""): ClassroomResponseData<D>()
    }
}