package com.viewsonic.classswift.data.info

import android.content.Context
import com.viewsonic.classswift.R
import com.viewsonic.classswift.api.response.StudentListResponse

data class StudentInfo(
    // For sorting used
    val serialNumber: Int = -1,
    val displaySeatNumber: String = "",
    val displayName: String = "",
    val defaultDisplaySeatNumber: String = "",
    val defaultDisplayName: String = "",
    val studentId: String = "",
    val status: Status = Status.INACTIVE,
    val points: Int = 0,
    val groupId: Int = 1,
    val isEditing: Boolean = false,
) {

    fun getActualDisplaySeatNumber(): String {
        return when {
            displaySeatNumber.isNotEmpty() -> displaySeatNumber
            defaultDisplaySeatNumber.isNotEmpty() -> defaultDisplaySeatNumber
            else -> "-"
        }
    }

    fun getActualDisplayName(context: Context): String {
        return when {
            displayName.isNotEmpty() -> displayName
            defaultDisplayName.isNotEmpty() -> defaultDisplayName
            else -> context.getString(R.string.common_guest)
        }
    }

    fun getParticipationState(): ParticipationState {
        return when {
            status == Status.ACTIVE && studentId.isNotEmpty() && displayName.isNotEmpty() -> ParticipationState.JOINED
            status == Status.ACTIVE && studentId.isNotEmpty() -> ParticipationState.JOINING
            else -> ParticipationState.NOT_JOINED

        }
    }

    fun isJoinedClass(): Boolean {
        return getParticipationState() == ParticipationState.JOINED
    }

    fun isAttendedClass(): Boolean {
        return getParticipationState() != ParticipationState.NOT_JOINED
    }

    fun canDecreasePoints(): Boolean {
        return points > 0 && isJoinedClass()
    }

    enum class Status {
        ACTIVE,
        INACTIVE
    }

    companion object {
        fun fromAttendedStudentDetail(detail: StudentListResponse.AttendedStudentDetail): StudentInfo {
            val status = when(detail.status) {
                Status.ACTIVE.name -> Status.ACTIVE
                Status.INACTIVE.name -> Status.INACTIVE
                else -> Status.INACTIVE
            }
            return StudentInfo(
                serialNumber = detail.serialNumber,
                displaySeatNumber = detail.seatNumber,
                displayName = detail.displayName,
                studentId = detail.studentId,
                status = status,
                points = detail.points,
                groupId = detail.groupId
            )
        }
    }

    enum class ParticipationState {
        NOT_JOINED,
        JOINING,
        JOINED,
    }
}