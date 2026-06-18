package com.viewsonic.classswift.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.viewsonic.classswift.TutorialPreferences
import java.io.InputStream
import java.io.OutputStream

object TutorialPreferenceSerializer : Serializer<TutorialPreferences> {
    override val defaultValue: TutorialPreferences = TutorialPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): TutorialPreferences {
        return try {
            TutorialPreferences.parseFrom(input)
        } catch (exception: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: TutorialPreferences, output: OutputStream) {
        t.writeTo(output)
    }
}

class TutorialDataStore(dataStore: DataStore<TutorialPreferences>): BaseDataStore<TutorialPreferences>(dataStore, TutorialPreferences.getDefaultInstance()) {
    suspend fun clearClassPagePhaseCompletion() {
        dataStore.updateData {
            it.toBuilder().clearClassPagePhaseCompletion().build()
        }
    }

    suspend fun setClassPagePhaseCompletion(userId: String, isCompleted: Boolean) {
        dataStore.updateData {
            it.toBuilder().putClassPagePhaseCompletion(userId, isCompleted).build()
        }
    }

    suspend fun isClassPagePhaseCompleted(userId: String): Boolean {
        return getPreferences().classPagePhaseCompletionMap[userId] ?: false
    }

    suspend fun clearStudentPagePhaseCompletion() {
        dataStore.updateData {
            it.toBuilder().clearStudentPagePhaseCompletion().build()
        }
    }

    suspend fun setStudentPagePhaseCompletion(userId: String, isCompleted: Boolean) {
        dataStore.updateData {
            it.toBuilder().putStudentPagePhaseCompletion(userId, isCompleted).build()
        }
    }

    suspend fun isStudentPagePhaseCompleted(userId: String): Boolean {
        return getPreferences().studentPagePhaseCompletionMap[userId] ?: false
    }
}

val Context.tutorialDataStore: DataStore<TutorialPreferences> by dataStore(
    fileName = "tutorial_datastore.pb",
    serializer = TutorialPreferenceSerializer
)