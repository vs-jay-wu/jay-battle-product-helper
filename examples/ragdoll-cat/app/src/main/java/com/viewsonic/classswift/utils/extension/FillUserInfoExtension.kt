package com.viewsonic.classswift.utils.extension

import com.viewsonic.classswift.data.info.FillUserInfo

/**
 * MyViewBoard token login: consent / AI checkbox UI and profile fill UI are all handled outside
 * ClassSwift (CLSWAN-1242). [isChirpAIConsent] is forced to true so [isNeedAIConsentUI] stays
 * off for the GB AI row.
 */
fun FillUserInfo.withMyViewBoardConsentPromptsSuppressed(): FillUserInfo =
    copy(isNeedFillInfoUI = false, isNeedConsentUI = false, isChirpAIConsent = true)
