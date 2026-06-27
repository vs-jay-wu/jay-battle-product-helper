package com.viewsonic.classswift.data.info

import com.viewsonic.classswift.api.response.GuestRoomData
import com.viewsonic.classswift.api.response.RoomsData

data class ClassroomInfo(
    val id: String = "",
    val number: String = "", // 教室編號
    val icon: Int = 0, // 暫時沒有用到
    val displayName: String = "",
    val status: String = "",
    val lessonId: String = "",
    val maxStudentCount: Int = -1,
    val originId: String = "",
    val originType: OriginType = OriginType.NONE,
    val roomLink: String = "",
    val roomShortUrl: String = "",
    val loginType: String = "",
    val lessonStartTime: Long = 0,
    val hasAddedScoreManually: Boolean = false
) {
    enum class OriginType(val serverValue: String) {
        NONE(""),
        CANVAS("canvas"),
        CLASS_LINK("classlink"),
        GOOGLE_CLASSROOM("google_classroom");

        companion object {
            fun valueOfServerValue(serverValue: String): OriginType {
                return when (serverValue) {
                    CANVAS.serverValue -> CANVAS
                    CLASS_LINK.serverValue -> CLASS_LINK
                    GOOGLE_CLASSROOM.serverValue -> GOOGLE_CLASSROOM
                    else -> NONE
                }
            }
        }
    }

    fun isLessonOnGoing(): Boolean = status == "in_class"

    fun isValid(): Boolean = id.isNotEmpty() and lessonId.isNotEmpty()

    fun isRoster(): Boolean = originType != OriginType.NONE

    companion object {
        fun fromApiRoomData(roomsData: RoomsData): ClassroomInfo {
            return ClassroomInfo(
                id = roomsData.roomId,
                number = roomsData.roomNumber,
                displayName = roomsData.displayName,
                status = roomsData.status,
                lessonId = "",
                maxStudentCount = roomsData.studentCount,
                originId = roomsData.originId,
                originType = OriginType.valueOfServerValue(roomsData.originType),
                roomLink = roomsData.roomLink,
                roomShortUrl = roomsData.roomShortUrl,
                loginType = roomsData.loginType
            )
        }

        fun fromApiGuestRoomData(roomsData: GuestRoomData, displayName: String, maxStudentCount: Int): ClassroomInfo {
            return ClassroomInfo(
                id = roomsData.roomId,
                number = roomsData.roomNumber,
                lessonId = "",
                roomLink = roomsData.roomLink,
                roomShortUrl = roomsData.roomShortUrl,
                maxStudentCount = maxStudentCount,
                displayName = displayName
            )
        }
    }
}
