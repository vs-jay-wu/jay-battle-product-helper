package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.api.response.data.StudentPointResponseData

@JsonClass(generateAdapter = true)
data class UpdateStudentPointResponse(
    @Json(name = "data")
    val studentPointResponseData: StudentPointResponseData = StudentPointResponseData(),
)