package com.viewsonic.classswift.utils

import android.content.Context
import androidx.core.os.ConfigurationCompat
import com.viewsonic.classswift.ui.widget.quiz.enums.OptionLanguageType

object LanguageUtils {

    const val TRADITIONAL_CHINESE_CODE = "zh-TW"
    const val GENERAL_CHINESE_CODE = "zh"
    const val ENGLISH_CODE_FOR_VS_ACCOUNT = "en-US"
    const val ENGLISH_CODE = "en"

    var displayLanguageCode = ENGLISH_CODE
        private set

    var vsAccountLanguageCode: String = ENGLISH_CODE_FOR_VS_ACCOUNT
        private set

    var webLanguageCode: String = ENGLISH_CODE
        private set

    fun isAppLanguageChinese(context: Context): Boolean {
        // Get the current app's primary locale using the recommended compatibility library.
        val currentLocale = ConfigurationCompat.getLocales(context.resources.configuration).get(0)
        // The ISO 639-1 code for Chinese is "zh".
        // This correctly handles all variants (zh-CN, zh-TW, zh-HK, etc.).
        currentLocale?.let {
            return it.language == GENERAL_CHINESE_CODE
        }
        return false
    }

    fun setLanguageCode(isChinese: Boolean) {
        displayLanguageCode = if (isChinese) TRADITIONAL_CHINESE_CODE else ENGLISH_CODE
        vsAccountLanguageCode = if (isChinese) TRADITIONAL_CHINESE_CODE else ENGLISH_CODE_FOR_VS_ACCOUNT
        webLanguageCode = if (isChinese) GENERAL_CHINESE_CODE else ENGLISH_CODE
    }

    fun switchLanguageCode() {
        displayLanguageCode = if (displayLanguageCode == TRADITIONAL_CHINESE_CODE) ENGLISH_CODE else TRADITIONAL_CHINESE_CODE
        vsAccountLanguageCode = if (vsAccountLanguageCode == TRADITIONAL_CHINESE_CODE) ENGLISH_CODE_FOR_VS_ACCOUNT else TRADITIONAL_CHINESE_CODE
        webLanguageCode = if (webLanguageCode == GENERAL_CHINESE_CODE) ENGLISH_CODE else GENERAL_CHINESE_CODE
    }

    fun languageCodeIsChinese(): Boolean {
        return displayLanguageCode == TRADITIONAL_CHINESE_CODE
    }

    fun getTfOptionLanguageType(): OptionLanguageType {
        return if (languageCodeIsChinese()) OptionLanguageType.CHINESE else OptionLanguageType.ENGLISH
    }
}
