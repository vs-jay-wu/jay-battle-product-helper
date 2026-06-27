package com.viewsonic.classswift.data.task

import com.viewsonic.classswift.data.info.StudentInfo
import com.viewsonic.classswift.ui.widget.task.enums.SubmitStatus

sealed class TaskResultInfo {

    interface SelectableResult {
        val isSelected: Boolean
        val isEditable: Boolean
        val isPushable: Boolean
        fun copyWithEditable(isEditable: Boolean): TaskResultInfo
        fun copyWithEditableAndSelected(isEditable: Boolean, isSelected: Boolean): TaskResultInfo
        fun copyWithPushable(isEditable: Boolean, isSelected: Boolean, isPushable: Boolean): TaskResultInfo
    }

    abstract val taskId: String
    abstract val displayName: String
    abstract val studentId: String
    abstract val seatNumber: String
    abstract val serialNumber: Int
    abstract val groupId: String

    data class Content(
        override val taskId: String = "",
        override val displayName: String = "",
        override val studentId: String = "",
        override val seatNumber: String = "",
        override val serialNumber: Int = 0,
        override val groupId: String = "",
        override val isSelected: Boolean = false,
        override val isEditable: Boolean = false,
        override val isPushable: Boolean = false,
        val triggerType: SubmitStatus = SubmitStatus.UNKNOWN,
        val imgUrl: String = "",
        val status: String = "",
        val version: Int = 0,
    ) : TaskResultInfo(), SelectableResult {

        fun isSameItemAs(other: Content): Boolean {
            return studentId == other.studentId &&
                    seatNumber == other.seatNumber &&
                    serialNumber == other.serialNumber &&
                    groupId == other.groupId &&
                    status == other.status &&
                    version == other.version &&
                    imgUrl == other.imgUrl &&
                    displayName == other.displayName &&
                    triggerType == other.triggerType &&
                    isEditable == other.isEditable &&
                    isSelected == other.isSelected &&
                    isPushable == other.isPushable
        }

        fun isContentSameAs(other: Content): Boolean {
            return studentId == other.studentId &&
                    seatNumber == other.seatNumber &&
                    serialNumber == other.serialNumber &&
                    groupId == other.groupId &&
                    status == other.status &&
                    version == other.version &&
                    imgUrl == other.imgUrl &&
                    displayName == other.displayName &&
                    triggerType == other.triggerType &&
                    isEditable == other.isEditable &&
                    isSelected == other.isSelected &&
                    isPushable == other.isPushable
        }

        override fun copyWithEditable(isEditable: Boolean): TaskResultInfo {
            return this.copy(isEditable = isEditable)
        }

        override fun copyWithEditableAndSelected(
            isEditable: Boolean,
            isSelected: Boolean
        ): TaskResultInfo {
            return this.copy(isEditable = isEditable, isSelected = isSelected)
        }

        override fun copyWithPushable(
            isEditable: Boolean,
            isSelected: Boolean,
            isPushable: Boolean
        ): TaskResultInfo {
            return this.copy(
                isEditable = isEditable,
                isSelected = isSelected,
                isPushable = isPushable
            )
        }

        companion object {
            fun create(
                taskId: String = "",
                triggerType: String = "",
                displayName: String = "",
                studentId: String = "",
                seatNumber: String = "",
                imgUrl: String = "",
                serialNumber: Int = 0,
                groupId: String = "",
                status: String = "",
                version: Int = 0,
                isSelected: Boolean = false,
                isEditable: Boolean = false,
                isPushable: Boolean = false
            ): Content {
                return Content(
                    taskId = taskId,
                    displayName = displayName,
                    studentId = studentId,
                    triggerType = SubmitStatus.fromCode(triggerType),
                    seatNumber = seatNumber,
                    imgUrl = imgUrl,
                    serialNumber = serialNumber,
                    groupId = groupId,
                    status = status,
                    version = version,
                    isSelected = isSelected,
                    isEditable = isEditable,
                    isPushable = isPushable
                )
            }
        }
    }

    data class Link(
        override val taskId: String = "",
        override val displayName: String = "",
        override val studentId: String = "",
        override val seatNumber: String = "",
        override val serialNumber: Int = 0,
        override val groupId: String = "",
        val linkMeta: LinkMetaInfo = LinkMetaInfo(
            title = "",
            description = "",
            siteName = "",
            image = ""
        ),
        val linkUrl: String = "",
        val linkIsOpened: Boolean = false,
        val status: String = "",
        val version: Int = 0
    ) : TaskResultInfo() {

        fun isSameItemAs(other: Link): Boolean {
            return studentId == other.studentId &&
                    seatNumber == other.seatNumber &&
                    serialNumber == other.serialNumber &&
                    groupId == other.groupId &&
                    status == other.status &&
                    version == other.version &&
                    displayName == other.displayName
        }

        fun isContentSameAs(other: Link): Boolean {
            return studentId == other.studentId &&
                    seatNumber == other.seatNumber &&
                    serialNumber == other.serialNumber &&
                    groupId == other.groupId &&
                    status == other.status &&
                    version == other.version &&
                    displayName == other.displayName &&
                    linkMeta == other.linkMeta &&
                    linkUrl == other.linkUrl &&
                    linkIsOpened == other.linkIsOpened
        }

        companion object {
            fun create(
                taskId: String = "",
                displayName: String = "",
                studentId: String = "",
                seatNumber: String = "",
                linkMeta: LinkMetaInfo = LinkMetaInfo(
                    title = "",
                    description = "",
                    siteName = "",
                    image = ""
                ),
                linkUrl: String = "",
                linkIsOpened: Boolean = false,
                serialNumber: Int = 0,
                groupId: String = "",
                status: String = "",
                version: Int = 0
            ): Link {
                return Link(
                    taskId = taskId,
                    displayName = displayName,
                    studentId = studentId,
                    seatNumber = seatNumber,
                    linkMeta = linkMeta,
                    linkUrl = linkUrl,
                    linkIsOpened = linkIsOpened,
                    serialNumber = serialNumber,
                    groupId = groupId,
                    status = status,
                    version = version
                )
            }
        }
    }

    data class Guest(
        override val taskId: String = "",
        override val displayName: String = "",
        override val studentId: String = "",
        override val seatNumber: String = "",
        override val serialNumber: Int = 0,
        override val groupId: String = "",
        override val isSelected: Boolean = false,
        override val isEditable: Boolean = false,
        override val isPushable: Boolean = false,
    ) : TaskResultInfo(), SelectableResult {

        override fun copyWithEditable(isEditable: Boolean): TaskResultInfo {
            return this.copy(isEditable = isEditable)
        }

        override fun copyWithEditableAndSelected(
            isEditable: Boolean,
            isSelected: Boolean
        ): TaskResultInfo {
            return this.copy(isEditable = isEditable, isSelected = isSelected)
        }

        override fun copyWithPushable(
            isEditable: Boolean,
            isSelected: Boolean,
            isPushable: Boolean
        ): TaskResultInfo {
            return this.copy(
                isEditable = isEditable,
                isSelected = isSelected,
                isPushable = isPushable
            )
        }
    }

    data class ApiFail(
        override val taskId: String = "",
        override val displayName: String = "",
        override val studentId: String = "",
        override val seatNumber: String = "",
        override val serialNumber: Int = 0,
        override val groupId: String = "",
        override val isSelected: Boolean = false,
        override val isEditable: Boolean = false,
        override val isPushable: Boolean = false,
    ) : TaskResultInfo(), SelectableResult {
        override fun copyWithEditable(isEditable: Boolean): TaskResultInfo {
            return this.copy(isEditable = isEditable)
        }

        override fun copyWithEditableAndSelected(
            isEditable: Boolean,
            isSelected: Boolean
        ): TaskResultInfo {
            return this.copy(isEditable = isEditable, isSelected = isSelected)
        }

        override fun copyWithPushable(
            isEditable: Boolean,
            isSelected: Boolean,
            isPushable: Boolean
        ): TaskResultInfo {
            return this.copy(
                isEditable = isEditable,
                isSelected = isSelected,
                isPushable = isPushable
            )
        }

        fun isSameItemAs(other: ApiFail): Boolean {
            return studentId == other.studentId &&
                    seatNumber == other.seatNumber &&
                    serialNumber == other.serialNumber &&
                    groupId == other.groupId &&
                    displayName == other.displayName
        }
    }

    companion object {
        fun TaskResultInfo.syncWithClassSwiftHubData(
            studentInfo: StudentInfo
        ): TaskResultInfo = when (this) {
            is Content -> copy(
                seatNumber = studentInfo.displaySeatNumber,
                displayName = studentInfo.defaultDisplayName,
                groupId = studentInfo.groupId.toString()
            )

            is Link -> copy(
                seatNumber = studentInfo.displaySeatNumber,
                displayName = studentInfo.defaultDisplayName,
                groupId = studentInfo.groupId.toString()
            )

            is Guest -> copy(
                seatNumber = studentInfo.displaySeatNumber,
                displayName = studentInfo.defaultDisplayName,
                groupId = studentInfo.groupId.toString()
            )

            is ApiFail -> copy(
                seatNumber = studentInfo.displaySeatNumber,
                displayName = studentInfo.defaultDisplayName,
                groupId = studentInfo.groupId.toString()
            )
        }
    }

}