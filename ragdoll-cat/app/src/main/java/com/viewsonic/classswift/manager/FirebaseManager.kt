package com.viewsonic.classswift.manager

import FirebaseCrashlyticsConstant
import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.viewsonic.classswift.data.datastore.AccountDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class FirebaseManager(
    private val firebaseInstallationManager: FirebaseInstallationManager,
    private val accountDataStore: AccountDataStore
) : AnalyticsReporter {
    suspend fun initialize() = withContext(Dispatchers.IO) {
        accountDataStore.getUserId().takeIf { it.isNotEmpty() }?.let { userId ->
            setUserId(userId)
        }
        accountDataStore.getUserEmail().takeIf { it.isNotEmpty() }?.let { userEmail ->
            setUserEmail(userEmail)
        }
        Firebase.crashlytics.setCustomKey(FirebaseCrashlyticsConstant.Key.FIREBASE_INSTALLATION_ID, firebaseInstallationManager.fetchInstallationId())
    }

    suspend fun setUserInfo(userId: String, userEmail: String) = withContext(Dispatchers.IO) {
        setUserId(userId)
        setUserEmail(userEmail)
        accountDataStore.setUserId(userId)
        accountDataStore.setUserEmail(userEmail)
    }

    private fun Map<String, Any>.toBundle(): Bundle {
        val bundle = Bundle()
        for ((key, value) in this) {
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                is Float -> bundle.putFloat(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        return bundle
    }

    private fun setUserEmail(userEmail: String) {
        Firebase.crashlytics.setCustomKey(FirebaseCrashlyticsConstant.Key.USER_EMAIL, userEmail)
        setUserProperty(FirebaseCrashlyticsConstant.Key.USER_EMAIL, userEmail)
    }

    override fun logEvent(eventName: String, params: Map<String, Any>?) {
        Timber.tag("firebase_log").d("Log event: $eventName, params: $params")
        val bundle = params?.toBundle()
        Firebase.analytics.logEvent(eventName, bundle)
    }

    override fun setUserProperty(name: String, value: String) {
        Firebase.analytics.setUserProperty(name, value)
    }

    override fun setUserId(userId: String) {
        Firebase.crashlytics.setUserId(userId)
        Firebase.analytics.setUserId(userId)
        Firebase.crashlytics.setCustomKey(FirebaseCrashlyticsConstant.Key.USER_ID, userId)
        setUserProperty(FirebaseCrashlyticsConstant.Key.USER_ID, userId)
    }

    override fun logNonFatalError(throwable: Throwable, customKeys: Map<String, Any>?) {
        Timber.tag("firebase_log").d("logNonFatalError: $throwable, customKeys: $customKeys")
        customKeys?.forEach { (key, value) ->
            when (value) {
                is String -> Firebase.crashlytics.setCustomKey(key, value)
                is Int -> Firebase.crashlytics.setCustomKey(key, value)
                is Long -> Firebase.crashlytics.setCustomKey(key, value)
                is Double -> Firebase.crashlytics.setCustomKey(key, value)
                is Boolean -> Firebase.crashlytics.setCustomKey(key, value)
                is Float -> Firebase.crashlytics.setCustomKey(key, value)
                else -> Firebase.crashlytics.setCustomKey(key, value.toString())
            }
        }
        Firebase.crashlytics.recordException(throwable)
    }
}

interface AnalyticsReporter {
    fun logEvent(eventName: String, params: Map<String, Any>? = null)
    fun setUserProperty(name: String, value: String)
    fun setUserId(userId: String)
    fun logNonFatalError(throwable: Throwable, customKeys: Map<String, Any>? = null)
}