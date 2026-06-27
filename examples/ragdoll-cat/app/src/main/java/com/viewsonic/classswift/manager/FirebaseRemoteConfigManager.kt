package com.viewsonic.classswift.manager

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.R
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseRemoteConfigManager{

    suspend fun initialize(): Boolean = suspendCoroutine { continuation ->
        // Remote Config
        Firebase.remoteConfig.apply {
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
                fetchTimeoutInSeconds = 3
            }
            setConfigSettingsAsync(configSettings)
            setDefaultsAsync(R.xml.default_remote_config)
            try {
                fetchAndActivate().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val updated = task.result
                        Timber.d("Firebase remote config fetch and activate succeeded, updated: $updated")
                    } else {
                        Timber.d("Firebase remote config fetch failed")
                    }
                    continuation.resume(task.isSuccessful)
                }.addOnCanceledListener {
                    continuation.resume(false)
                }
            } catch (e: Exception) {
                Timber.e(e, "Firebase remote config occurs exception ")
                e.printStackTrace()
                continuation.resume(false)
            }
        }
    }

    fun getBoolean(remoteKey: String): Boolean {
        return Firebase.remoteConfig.getBoolean(remoteKey)
    }

    fun getString(remoteKey: String): String {
        return Firebase.remoteConfig.getString(remoteKey)
    }

    fun getLong(remoteKey: String): Long {
        return Firebase.remoteConfig.getLong(remoteKey)
    }

    fun getDouble(remoteKey: String): Double {
        return Firebase.remoteConfig.getDouble(remoteKey)
    }
}