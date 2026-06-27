package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OtaVersionInfoResponse(
    @Json(name = "version_info")
    val versionInfoList: List<OtaVersionInfo> = emptyList(),
    @Json(name = "latest_version")
    val latestVersion: String = "",
    @Json(name = "is_bypass_ota")
    val isBypassOta: Boolean = false
)

@JsonClass(generateAdapter = true)
data class OtaVersionInfo(
    @Json(name = "model_name")
    var modelName: String = "",
    @Json(name = "vs_model_name")
    var vsModelName: String = "",
    @Json(name = "app_version")
    var appVersion: String = ""
)
