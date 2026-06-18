package com.viewsonic.classswift.data.spinner

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PickupStudentInfo(
    val studentId: String
)