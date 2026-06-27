package com.viewsonic.classswift.data.info

import com.viewsonic.classswift.api.response.ChallengeCodeResponse
import com.viewsonic.classswift.api.response.LoginUrlsResponse
import timber.log.Timber

data class SignInUrlInfo(
    var countryCode: String = "",
    var codeChallenge: String = "",
    var codeChallengeMethod: String = "",
    var viewSonicSignInUrl: String = "",
    var classLinkSignInUrl: String = "",
    var mircoSoftSignInUrl: String = "",
    var googleSignInUrl: String = "",
    var qrSignInUrl: String = ""
) {
    val google = "google"
    val viewSonic = "viewsonic"
    val microsoft = "microsoft"
    val classLink = "classlink"

    // Available country codes for using ClassSwift
    val availableCountryCode = listOf("US", "MY", "TW", "IN", "TR")

    fun getSsoList(): String {
        if (this.countryCode == "US") {
            return "$viewSonic, $classLink, $google, $microsoft"
        }
        return "$viewSonic, $google, $microsoft"
    }

    fun isAvailableRegion(): Boolean {
        val isAvailable = availableCountryCode.contains(this.countryCode)
        Timber.tag("region").d("api countryCode: ${this.countryCode}, isAvailable: $isAvailable")
        return isAvailable
    }

    fun setChallengeCodeInfo(response: ChallengeCodeResponse) {
        codeChallenge = response.codeChallenge
        codeChallengeMethod = response.codeChallengeMethod
    }

    fun setUrlList(response: LoginUrlsResponse) {
        viewSonicSignInUrl = response.loginUrlList.viewSonicSignInUrl
        classLinkSignInUrl = response.loginUrlList.classLinkSignInUrl
        mircoSoftSignInUrl = response.loginUrlList.microsoftSignInUrl
        googleSignInUrl = response.loginUrlList.googleSignInUrl
    }

    fun setQrCodeSignInUrl(response: LoginUrlsResponse) {
        qrSignInUrl = response.qrCodeUrl
    }
}