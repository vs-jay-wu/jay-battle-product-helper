package com.viewsonic.classswift.data.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.viewsonic.classswift.TestPreferences
import java.io.InputStream
import java.io.OutputStream

object TestPreferencesSerializer : Serializer<TestPreferences> {
    override val defaultValue: TestPreferences = TestPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): TestPreferences {
        try {
            return TestPreferences.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: TestPreferences,
        output: OutputStream
    ) = t.writeTo(output)
}

class TestDataStore(dataStore: DataStore<TestPreferences>): BaseDataStore<TestPreferences>(dataStore, TestPreferences.getDefaultInstance()) {
    suspend fun getVersion() = getPreferences().version

    suspend fun setVersion(value: Int) {
        dataStore.updateData {
            it.toBuilder().setVersion(value).build()
        }
    }
}

val Context.testDatastore: DataStore<TestPreferences> by dataStore(
    fileName = "test_datastore.pb",
    serializer = TestPreferencesSerializer
)