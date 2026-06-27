package com.viewsonic.classswift.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.viewsonic.classswift.MaintenanceAnnouncementsPreferences
import java.io.InputStream
import java.io.OutputStream

object MaintenanceAnnouncementsPreferenceSerializer : Serializer<MaintenanceAnnouncementsPreferences> {
    override val defaultValue: MaintenanceAnnouncementsPreferences = MaintenanceAnnouncementsPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): MaintenanceAnnouncementsPreferences {
        return try {
            MaintenanceAnnouncementsPreferences.parseFrom(input)
        } catch (exception: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: MaintenanceAnnouncementsPreferences, output: OutputStream) {
        t.writeTo(output)
    }
}

class MaintenanceAnnouncementsDataStore(dataStore: DataStore<MaintenanceAnnouncementsPreferences>): BaseDataStore<MaintenanceAnnouncementsPreferences>(dataStore, MaintenanceAnnouncementsPreferences.getDefaultInstance()) {
    suspend fun clearTwoDaysBeforeAnnouncementViewed() {
        dataStore.updateData {
            it.toBuilder().clearTwoDaysBeforeAnnouncementViewed().build()
        }
    }

    suspend fun setTwoDaysBeforeAnnouncementViewed(userId: String, twoDaysBeforeShowtimeInSeconds: Long, isViewed: Boolean) {
        val key = "$userId-$twoDaysBeforeShowtimeInSeconds"
        dataStore.updateData {
            it.toBuilder().putTwoDaysBeforeAnnouncementViewed(key, isViewed).build()
        }
    }

    suspend fun isTwoDaysBeforeAnnouncementViewed(userId: String, twoDaysBeforeShowtimeInSeconds: Long): Boolean {
        val key = "$userId-$twoDaysBeforeShowtimeInSeconds"
        return getPreferences().twoDaysBeforeAnnouncementViewedMap[key] ?: false
    }
}

val Context.maintenanceAnnouncementsDatastore: DataStore<MaintenanceAnnouncementsPreferences> by dataStore(
    fileName = "maintenance_announcements.pb",
    serializer = MaintenanceAnnouncementsPreferenceSerializer
)