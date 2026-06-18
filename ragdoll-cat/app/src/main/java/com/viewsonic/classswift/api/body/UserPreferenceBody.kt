package com.viewsonic.classswift.api.body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.api.moshi.MoshiProvider
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@JsonClass(generateAdapter = true)
data class TranslationToolUserPreferenceBody(@Json(name = "is_on") val isOn: Boolean) {
    fun toRequestBody(): RequestBody {
        val contentType = "application/json; charset=utf-8".toMediaType()
        val body = MoshiProvider.toJson(this, true).toRequestBody(contentType)
        return body
    }
}

@JsonClass(generateAdapter = true)
data class InAppTutorialUserPreferenceBody(@Json(name = "is_shown") val isShown: Boolean) {
    fun toRequestBody(): RequestBody {
        val contentType = "application/json; charset=utf-8".toMediaType()
        val body = MoshiProvider.toJson(this, true).toRequestBody(contentType)
        return body
    }
}
