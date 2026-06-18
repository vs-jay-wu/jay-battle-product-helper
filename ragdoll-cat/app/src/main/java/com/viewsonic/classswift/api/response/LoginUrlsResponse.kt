package com.viewsonic.classswift.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginUrlsResponse(
    @Json(name = "code_challenge")
    val codeChallenge: String = "",
    @Json(name = "url")
    val qrCodeUrl: String = "",
    @Json(name = "urls")
    val loginUrlList: UrlList = UrlList()
) {
    @JsonClass(generateAdapter = true)
    data class UrlList(
        @Json(name = "viewsonic")
        val viewSonicSignInUrl: String = "",
        @Json(name = "google")
        val googleSignInUrl: String = "",
        @Json(name = "microsoft")
        val microsoftSignInUrl: String = "",
        @Json(name = "classlink")
        val classLinkSignInUrl: String = "",
    )
}
