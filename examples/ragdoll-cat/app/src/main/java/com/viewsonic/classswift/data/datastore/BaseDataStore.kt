package com.viewsonic.classswift.data.datastore

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

open class BaseDataStore<T>(val dataStore: DataStore<T>, val defaultValue: T){
    protected suspend fun getPreferences(): T {
        return dataStore.data.catch { exception ->
            // dataStore.data throws an IOException when an error is encountered when reading data
            if (exception is IOException) {
                emit(defaultValue)
            } else {
                throw exception
            }
        }.first()
    }
}