package com.viewsonic.classswift.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.viewsonic.classswift.AccountPreferences
import java.io.InputStream
import java.io.OutputStream

object AccountPreferenceSerializer : Serializer<AccountPreferences> {
    override val defaultValue: AccountPreferences = AccountPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): AccountPreferences {
        return try {
            AccountPreferences.parseFrom(input)
        } catch (exception: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: AccountPreferences, output: OutputStream) {
        t.writeTo(output)
    }
}

class AccountDataStore(dataStore: DataStore<AccountPreferences>): BaseDataStore<AccountPreferences>(dataStore, AccountPreferences.getDefaultInstance()) {
    suspend fun setUserEmail(value: String) {
        dataStore.updateData {
            it.toBuilder().setUserEmail(value).build()
        }
    }

    suspend fun getUserEmail(): String {
        return getPreferences().userEmail
    }

    suspend fun setUserId(value: String) {
        dataStore.updateData {
            it.toBuilder().setUserId(value).build()
        }
    }

    suspend fun getUserId(): String {
        return getPreferences().userId
    }

}

val Context.accountDatastore: DataStore<AccountPreferences> by dataStore(
    fileName = "account_datastore.pb",
    serializer = AccountPreferenceSerializer
)