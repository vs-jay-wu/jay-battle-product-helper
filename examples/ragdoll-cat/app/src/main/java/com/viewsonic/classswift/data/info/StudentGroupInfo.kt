package com.viewsonic.classswift.data.info

data class StudentGroupInfo(
    val isEditing: Boolean = false,
    val groupId: Int = 1,
    val studentList: List<StudentInfo> = emptyList()
) {

    fun canUpdatePoints(): Boolean {
        // not in editing mode and has student in class, can add or decrease point
        return !isEditing && hasAttendingStudent()
    }

    fun getAttendingStudentIDs(): List<String> {
        return studentList
            .filter { it.isAttendedClass() }
            .map { it.studentId }
    }

    // only point > 0 can decrease score
    fun getHasScoreStudentIDs(): List<String> {
        return studentList
            .filter { it.isAttendedClass() && it.points > 0}
            .map { it.studentId }
    }


    private fun hasAttendingStudent(): Boolean {
        studentList.forEach { info ->
            if (info.isAttendedClass()) return true
        }
        return false
    }
}
