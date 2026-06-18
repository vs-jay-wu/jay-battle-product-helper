package com.viewsonic.classswift.ui.viewmodel

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import com.viewsonic.classswift.data.datastore.SettingsDataStore
import com.viewsonic.classswift.utils.LanguageUtils
import timber.log.Timber

class SettingLanguageViewModel( private val settingsDataStore: SettingsDataStore) : ViewModel() {
    suspend fun setAppLanguage() {
        LanguageUtils.switchLanguageCode()
        val localeList = LocaleListCompat.forLanguageTags( LanguageUtils.displayLanguageCode)
        Timber.tag("language").d("ChangeLanguage localeList: $localeList")
        AppCompatDelegate.setApplicationLocales(localeList)
        settingsDataStore.setLanguageIsChinese(LanguageUtils.languageCodeIsChinese())
    }
}