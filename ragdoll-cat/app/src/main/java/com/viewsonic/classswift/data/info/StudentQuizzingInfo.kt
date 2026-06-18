package com.viewsonic.classswift.data.info

import android.content.Context
import com.viewsonic.classswift.data.info.StudentInfo.Status
import com.viewsonic.classswift.data.socket.quiz.data.AnswerData

data class StudentQuizzingInfo(
    // For sorting used
    val serialNumber: Int = -1,
    val displaySeatNumber: String = "",
    var displayName: String = "",
    var studentId: String = "",
    var status: Status = Status.INACTIVE,
    val answerDataList: MutableList<AnswerData> = mutableListOf(),
    var answerStringData: String = "",
    var canShowAnswer: Boolean = false
) {
    companion object {
        /**
         * VSFT-8612: 直接以 backend 回的 [StudentInfo.status] 為準。
         * 舊版透過「studentId + displayName 都非空」重新判定，會把學生剛 ChooseSeat
         * 但還沒 SetStudentName 的 ACTIVE 學生誤判成 INACTIVE，造成 UI 短暫顯示 Absent。
         */
        fun fromStudentInfo(context: Context, studentInfo: StudentInfo): StudentQuizzingInfo {
            return StudentQuizzingInfo(
                serialNumber = studentInfo.serialNumber,
                displaySeatNumber = studentInfo.getActualDisplaySeatNumber(),
                displayName = studentInfo.getActualDisplayName(context),
                studentId = studentInfo.studentId,
                status = studentInfo.status
            )
        }
    }
}
