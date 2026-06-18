package com.viewsonic.classswift.utils.extension

import android.content.Context
import com.viewsonic.classswift.utils.LanguageUtils
import timber.log.Timber

// for floating window get setting language
fun Context.localizedContext(): Context {
    Timber.tag("changeLanguage").d("LanguageCode: ${LanguageUtils.displayLanguageCode}")
    val locale = java.util.Locale.forLanguageTag(LanguageUtils.displayLanguageCode)
    val config = resources.configuration
    config.setLocale(locale)
    return createConfigurationContext(config)
}