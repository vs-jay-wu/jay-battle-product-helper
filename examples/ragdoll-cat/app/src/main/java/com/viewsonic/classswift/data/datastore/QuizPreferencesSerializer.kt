package com.viewsonic.classswift.data.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.viewsonic.classswift.QuizPreferences
import java.io.InputStream
import java.io.OutputStream

object QuizPreferencesSerializer : Serializer<QuizPreferences> {
    override val defaultValue: QuizPreferences = QuizPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): QuizPreferences {
        try {
            return QuizPreferences.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: QuizPreferences,
        output: OutputStream
    ) = t.writeTo(output)
}

class QuizDataStore(dataStore: DataStore<QuizPreferences>): BaseDataStore<QuizPreferences>(dataStore, QuizPreferences.getDefaultInstance()) {
    suspend fun getOptionCount() = getPreferences().optionCount

    suspend fun setOptionCount(value: Int) {
        dataStore.updateData {
            it.toBuilder().setOptionCount(value).build()
        }
    }

    suspend fun getOptionType() = getPreferences().optionType

    suspend fun setOptionType(value: String) {
        dataStore.updateData {
            it.toBuilder().setOptionType(value).build()
        }
    }

    suspend fun getSelectionType() = getPreferences().selectionType ?: ""

    suspend fun setSelectionType(value: String) {
        dataStore.updateData {
            it.toBuilder().setSelectionType(value).build()
        }
    }
}

val Context.quizDatastore: DataStore<QuizPreferences> by dataStore(
    fileName = "quiz_datastore.pb",
    serializer = QuizPreferencesSerializer
)