package com.viewsonic.classswift.data.info

import com.viewsonic.classswift.data.enum.SketchAnswerStatus

data class SketchStudentCardInfo(
    val studentId: String,
    val serialNumber: Int = -1,
    val displaySeatNumber: String = "",
    val displayName: String = "",
    val status: SketchAnswerStatus = SketchAnswerStatus.NOT_SUBMITTED,
)
