package com.viewsonic.classswift.builder

import com.viewsonic.classswift.factory.AmplitudeFactory
import com.viewsonic.classswift.factory.AmplitudeFactory.UserPropertyType
import com.viewsonic.classswift.factory.AmplitudeFactory.EventPropertyType
import com.viewsonic.classswift.manager.AmplitudeManager
import org.koin.java.KoinJavaComponent.inject

class AmplitudeEventBuilder(
    private val eventName: String
) {
    private val amplitudeManager: AmplitudeManager by inject(AmplitudeManager::class.java)
    private val amplitudeFactory: AmplitudeFactory by inject(AmplitudeFactory::class.java)
    private val eventProperties: MutableMap<String, Any?> = mutableMapOf()
    private val userProperties: MutableMap<String, Any?> = mutableMapOf()

    fun appendEventProperties(properties: MutableMap<String, Any?>): AmplitudeEventBuilder {
        eventProperties.putAll(properties)
        return this
    }

    fun appendEventProperty(key: String, value: Any): AmplitudeEventBuilder {
        eventProperties[key] = value
        return this
    }

    fun appendEventProperty(eventPropertyType: EventPropertyType): AmplitudeEventBuilder {
        eventProperties.putAll(amplitudeFactory.generateEventPropertiesMap(eventPropertyType))
        return this
    }

    fun appendUserProperties(properties: MutableMap<String, Any?>): AmplitudeEventBuilder {
        userProperties.putAll(properties)
        return this
    }

    fun appendUserProperty(key: String, value: Any): AmplitudeEventBuilder {
        userProperties[key] = value
        return this
    }

    fun appendUserProperty(userPropertyType: UserPropertyType): AmplitudeEventBuilder {
        userProperties.putAll(amplitudeFactory.generateUserPropertiesMap(userPropertyType))
        return this
    }

    fun send() {
        amplitudeManager.track(
            eventType = eventName,
            eventProperties = eventProperties,
            userProperties = userProperties,
            options = null
        )
    }
}