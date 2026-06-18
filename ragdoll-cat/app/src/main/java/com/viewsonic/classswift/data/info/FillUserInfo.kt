package com.viewsonic.classswift.data.info

import timber.log.Timber

data class FillUserInfo(val isNeedFillInfoUI: Boolean = false, val isChirpAIConsent: Boolean = true, val isNeedConsentUI: Boolean = false, val country: String = "") {
    // isChirpAIConsent是false還有國家是GB才要秀
    var isNeedAIConsentUI = !isChirpAIConsent && country == "GB"
    fun needFillAccountPage(): Boolean {
        Timber.tag("accountInfo").d("country name: $country")
        return isNeedFillInfoUI || isNeedConsentUI || isNeedAIConsentUI
    }
}
