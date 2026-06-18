package com.viewsonic.classswift.data.info

import com.viewsonic.classswift.utils.extension.withMyViewBoardConsentPromptsSuppressed
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FillUserInfoMyViewBoardConsentTest {

    @Test
    fun `withMyViewBoardConsentPromptsSuppressed clears consent and AI UI flags when only consent was required`() {
        val original = FillUserInfo(
            isNeedFillInfoUI = false,
            isChirpAIConsent = false,
            isNeedConsentUI = true,
            country = "GB",
        )
        assertTrue(original.needFillAccountPage())
        val suppressed = original.withMyViewBoardConsentPromptsSuppressed()
        assertFalse(suppressed.isNeedConsentUI)
        assertFalse(suppressed.isNeedAIConsentUI)
        assertFalse(suppressed.needFillAccountPage())
    }

    @Test
    fun `withMyViewBoardConsentPromptsSuppressed clears all UI flags even when profile is incomplete`() {
        val original = FillUserInfo(
            isNeedFillInfoUI = true,
            isChirpAIConsent = false,
            isNeedConsentUI = true,
            country = "GB",
        )
        assertTrue(original.needFillAccountPage())
        val suppressed = original.withMyViewBoardConsentPromptsSuppressed()
        assertFalse(suppressed.isNeedFillInfoUI)
        assertFalse(suppressed.isNeedConsentUI)
        assertFalse(suppressed.isNeedAIConsentUI)
        assertFalse(suppressed.needFillAccountPage())
    }

    @Test
    fun `without suppression consent-only still requires fill page`() {
        val original = FillUserInfo(
            isNeedFillInfoUI = false,
            isChirpAIConsent = true,
            isNeedConsentUI = true,
            country = "TW",
        )
        assertTrue(original.needFillAccountPage())
    }
}
