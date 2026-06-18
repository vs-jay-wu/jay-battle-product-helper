package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.api.response.data.LinkMeta
import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.data.task.LinkMetaInfo
import com.viewsonic.classswift.data.task.TaskResultInfo
import kotlin.text.ifEmpty

@JsonClass(generateAdapter = true)
data class GetStudentTaskResponse(
    @Json(name = "task_id")
    val taskId: String = "",
    @Json(name = "seq")
    val sequence: Int = 0,
    @Json(name = "created_at")
    val createdAt: Long = 0,
    @Json(name = "trigger_type")
    val triggerType: String = "",
    @Json(name = "version")
    val version: Int = 0,
    @Json(name = "img_url")
    val imgUrl: String = "",
    @Json(name = "student_img_url")
    val studentImgUrl: String = "",
    @Json(name = "teacher_img_url")
    val teacherImgUrl: String = "",
    @Json(name = "link_url")
    val linkUrl: String = "",
    @Json(name = "is_opened")
    val linkIsOpened: Boolean = false,
    @Json(name = "result_id")
    val resultId: String = "",
    @Json(name = "link_meta")
    val linkMeta: LinkMeta = LinkMeta(
        title = "",
        description = "",
        siteName = "",
        image = ""
    )
)

fun GetStudentTaskResponse.toTaskResultInfo(studentInfo: StudentInfo): TaskResultInfo {
    val taskResultInfo =
        if (linkUrl.isNotEmpty())
            TaskResultInfo.Link.create(
                taskId = taskId,
                displayName = studentInfo.displayName,
                linkMeta = LinkMetaInfo(
                    title = linkMeta.title,
                    description = linkMeta.description,
                    siteName = linkMeta.siteName,
                    image = linkMeta.image
                ),
                linkIsOpened = linkIsOpened,
                linkUrl = linkUrl,
                studentId = studentInfo.studentId,
                seatNumber = studentInfo.displaySeatNumber,
                serialNumber = studentInfo.serialNumber,
                groupId = studentInfo.groupId.toString(),
                version = version
            )
        else TaskResultInfo.Content.create(
            taskId = taskId,
            displayName = studentInfo.displayName,
            studentId = studentInfo.studentId,
            triggerType = triggerType,
            seatNumber = studentInfo.displaySeatNumber,
            imgUrl = studentImgUrl.ifEmpty { imgUrl },
            serialNumber = studentInfo.serialNumber,
            groupId = studentInfo.groupId.toString(),
            version = version
        )
    return taskResultInfo
}