package com.viewsonic.classswift.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.viewsonic.classswift.LoginPreferences
import java.io.InputStream
import java.io.OutputStream

object LoginPreferenceSerializer : Serializer<LoginPreferences> {
    override val defaultValue: LoginPreferences = LoginPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): LoginPreferences {
        return try {
            LoginPreferences.parseFrom(input)
        } catch (exception: Exception) {
            defaultValue
        }
    }

    override suspend fun writeTo(t: LoginPreferences, output: OutputStream) {
        t.writeTo(output)
    }
}

class LoginDataStore(dataStore: DataStore<LoginPreferences>): BaseDataStore<LoginPreferences>(dataStore, LoginPreferences.getDefaultInstance()) {
    suspend fun isRememberLoginInfo(): Boolean{
        return getPreferences().rememberLoginInfo.takeIf { getPreferences().rememberLoginInfo } ?: false
    }

    suspend fun setRememberLoginInfo(value: Boolean) {
        dataStore.updateData {
            it.toBuilder().setRememberLoginInfo(value).build()
        }
    }

    suspend fun setRefreshToken(value: String) {
        dataStore.updateData {
            it.toBuilder().setRefreshToken(value).build()
        }
    }

    suspend fun getRefreshToken(): String {
        return getPreferences().refreshToken
    }

}

val Context.loginDatastore: DataStore<LoginPreferences> by dataStore(
    fileName = "login_datastore.pb",
    serializer = LoginPreferenceSerializer
)