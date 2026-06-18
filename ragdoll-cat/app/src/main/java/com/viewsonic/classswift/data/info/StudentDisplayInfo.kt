package com.viewsonic.classswift.data.info


data class StudentDisplayInfo(
    var isGroup: Boolean = false,
    var studentList: List<StudentInfo> = ArrayList(),
    var groupList: List<StudentGroupInfo> = ArrayList()
)
