package com.viewsonic.classswift.constant

object AppConstants {
    const val HTTPS_PREFIX = "https://"
    const val ONE_SEC_DELAY = 1000L
    const val SOCKET_DISCONNECT_POLLING_INTERVAL = 5000L
    const val SKETCH_RESPONSE_POLLING_INTERVAL = 3000L
    const val THREE_SEC_DELAY = ONE_SEC_DELAY * 3
    const val FIVE_SEC_DELAY = ONE_SEC_DELAY * 5
    const val STOPWATCH_INTERVAL_TIME = 30L

    // Product Flavors - environment dimension
    const val ENVIRONMENT_STAG = "stag"
    const val ENVIRONMENT_RC = "rc"
    const val ENVIRONMENT_PROD = "prod"
}