package com.viewsonic.classswift.data.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.viewsonic.classswift.DebugPreferences
import java.io.InputStream
import java.io.OutputStream

object DebugPreferencesSerializer : Serializer<DebugPreferences> {
    override val defaultValue: DebugPreferences = DebugPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): DebugPreferences {
        try {
            return DebugPreferences.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: DebugPreferences,
        output: OutputStream
    ) = t.writeTo(output)
}

class DebugDataStore(dataStore: DataStore<DebugPreferences>): BaseDataStore<DebugPreferences>(dataStore, DebugPreferences.getDefaultInstance()) {
    suspend fun isUseAndroidTestJsonForMaintenanceAnnouncements() = getPreferences().isUseAndroidTestJsonForMaintenanceAnnouncements

    suspend fun setSsUseAndroidTestJsonForMaintenanceAnnouncements(value: Boolean) {
        dataStore.updateData {
            it.toBuilder().setIsUseAndroidTestJsonForMaintenanceAnnouncements(value).build()
        }
    }
}

val Context.debugDatastore: DataStore<DebugPreferences> by dataStore(
    fileName = "debug_datastore.pb",
    serializer = DebugPreferencesSerializer
)