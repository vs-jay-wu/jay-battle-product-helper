package com.viewsonic.classswift.manager

import android.content.Context
import com.amplitude.android.Amplitude
import com.amplitude.android.Configuration
import com.amplitude.android.events.BaseEvent
import com.amplitude.common.Logger
import com.amplitude.core.events.EventOptions
import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.constant.AmplitudeConstant
import timber.log.Timber

class AmplitudeManager(
    private val applicationContext: Context,
    private val firebaseInstallationManager: FirebaseInstallationManager
) {
    private val amplitude = Amplitude(
        Configuration(
            apiKey = BuildConfig.AMPLITUDE_API_KEY,
            context = applicationContext,
            flushIntervalMillis = 30000,
            flushQueueSize = 20,
        )
    ).apply {
        logger.logMode = if (BuildConfig.DEBUG) {
            Logger.LogMode.DEBUG
        } else {
            Logger.LogMode.INFO
        }
    }

    fun track(eventType: String, eventProperties: MutableMap<String, Any?>, userProperties: MutableMap<String, Any?>, options: EventOptions?) {
        val baseEvent = BaseEvent()
        baseEvent.eventType = eventType
        // Global Properties
        eventProperties[AmplitudeConstant.EventProperties.Key.SOURCE_PROJECT] = AmplitudeConstant.EventProperties.Value.ANDROID
        baseEvent.eventProperties = eventProperties
        userProperties[AmplitudeConstant.UserProperties.Key.FIREBASE_INSTALLATION_ID] = firebaseInstallationManager.getInstallationId()
        baseEvent.userProperties = userProperties
        if (DEBUG_LOG) {
            Timber.d("[AmplitudeManager] : eventType = $eventType")
            Timber.d("[AmplitudeManager] : eventProperties = $eventProperties")
            Timber.d("[AmplitudeManager] : userProperties = $userProperties")
        }
        amplitude.track(baseEvent, options)

        // In Amplitude, a UserProperty will persist after it has been sent once for a user.
        // Therefore, if you mistakenly send a UserProperty with the wrong UserProperties.Key,
        // remember to use this code to delete the incorrect user property you previously sent.
        //  val identify = Identify()
        //  identify.unset("The name of the property you want to delete")
        //  amplitude.identify(identify)
    }

    companion object {
        private const val DEBUG_LOG: Boolean = true
    }
}