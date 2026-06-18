package com.viewsonic.classswift.data.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.viewsonic.classswift.SettingsPreferences
import java.io.InputStream
import java.io.OutputStream

object SettingPreferenceSerializer : Serializer<SettingsPreferences> {
    override val defaultValue: SettingsPreferences = SettingsPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SettingsPreferences {
        try {
            return SettingsPreferences.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: SettingsPreferences,
        output: OutputStream
    ) = t.writeTo(output)
}

class SettingsDataStore(dataStore: DataStore<SettingsPreferences>): BaseDataStore<SettingsPreferences>(dataStore, SettingsPreferences.getDefaultInstance()) {
    suspend fun getLanguageIsChinese(): Boolean? {
        val preferences = getPreferences()
        // Use the generated 'has' method to check for presence
        return if (preferences.hasLanguageIsChinese()) {
            preferences.languageIsChinese
        } else {
            // The value was NEVER set (the initial or "cleared" state), Return null to signify "unset"
            null
        }
    }

    suspend fun setLanguageIsChinese(value: Boolean) {
        dataStore.updateData {
            it.toBuilder().setLanguageIsChinese(value).build()
        }
    }
}

val Context.settingsDatastore: DataStore<SettingsPreferences> by dataStore(
    fileName = "settings_datastore.pb",
    serializer = SettingPreferenceSerializer
)
