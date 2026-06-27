package com.viewsonic.classswift.api.body

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
@JsonClass(generateAdapter = true)
data class UserInfoBody(
    @Json(name = "default_display_name")
    var defaultDisplayName: String = "",
    @Json(name = "country")
    var country: String = "",
    @Json(name = "eula_id")
    var eulaID: String = "",
    @Json(name = "privacy_id")
    var privacyID: String = "",
    //todo will set by system or app language setting
    @Json(name = "lang")
    var lang: String = "en",
    @Json(name = "service_id")
    var serviceID: String = "",
    @Json(name = "chirp_ai_id")
    var chirpAIID: String? = null,
    @Json(name = "sla_id")
    var slaID: String = ""
)
