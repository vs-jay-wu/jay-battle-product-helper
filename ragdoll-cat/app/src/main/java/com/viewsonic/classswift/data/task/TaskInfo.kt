package com.viewsonic.classswift.data.task

sealed class TaskInfo {

    interface EditableTask {
        val isSelected: Boolean
        val isEditable: Boolean
        fun copyWithEditable(isEditable: Boolean): TaskInfo
    }

    /**
     * Represents an entry used to create new content.
     * Typically used as a "create new" item in a content grid or list.
     */
    data object TaskCreate : TaskInfo()

    /**
     * Data class representing a task associated with a lesson that involves image-based content.
     * Typically used to display or upload a screenshot as part of a lesson's task.
     *
     * @property id Unique identifier for the task
     * @property lessonId ID of the lesson this task is associated with
     * @property screenshotImgUrl URL of the screenshot image used for this task
     * @property isSelected Indicates whether the task is currently selected
     * @property isEditable Indicates whether the task is editable
     * @property imagePreSignUrl A pre-signed URL used for uploading an image
     * @property isUploadImageSuccess `true` if the image upload to AWS S3 was successful; `false` otherwise
     */
    data class Content(
        val id: String,
        val lessonId: String,
        val screenshotImgUrl: String,
        override val isSelected: Boolean,
        override val isEditable: Boolean,
        val imagePreSignUrl: String,
        val isUploadImageSuccess: Boolean // true if uploading image to AWS S3, false otherwise
    ) : TaskInfo(), EditableTask {

        /**
         * Returns a copy of the current task with the updated isEditable value.
         *
         * @param isEditable The new editable state to apply
         * @return A new instance of Content with the updated isEditable property
         */
        override fun copyWithEditable(isEditable: Boolean): TaskInfo {
            return this.copy(isEditable = isEditable)
        }
    }

    /**
     * Data class representing a link-based task, typically associated with a specific lesson.
     * This task contains metadata extracted from a URL, such as the title, description, and preview image.
     *
     * @property id Unique identifier for the task
     * @property title Title of the linked content
     * @property lessonId ID of the lesson this task is linked to
     * @property description A brief summary or excerpt of the content
     * @property isSelected Indicates whether the task is currently selected
     * @property isEditable Indicates whether the task is editable
     * @property siteName Name of the source website
     * @property imageUrl URL of a preview image
     * @property url The actual URL pointing to the external content
     */
    data class Link(
        val id: String,
        val title: String,
        val lessonId: String,
        val description: String,
        override val isSelected: Boolean,
        override val isEditable: Boolean,
        val siteName: String,
        val imageUrl: String,
        val url: String,
        val isValid: Boolean,
    ) : TaskInfo(), EditableTask {

        /**
         * Returns a copy of the current task with the updated isEditable value.
         *
         * @param isEditable The new editable state to apply
         * @return A new instance of Link with updated isEditable property
         */
        override fun copyWithEditable(isEditable: Boolean): TaskInfo {
            return this.copy(isEditable = isEditable)
        }
    }
}