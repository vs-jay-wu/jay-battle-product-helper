package com.viewsonic.classswift.api.interceptor

import okhttp3.logging.HttpLoggingInterceptor

object CustomLoggingInterceptor {

    fun getLargeHttpLoggingInterceptor() = HttpLoggingInterceptor { message ->
        val isTryToShowFullLog = false
        val logCatMaxLength = 2048
        if (message.length > logCatMaxLength) {
            if (!isTryToShowFullLog) {
                HttpLoggingInterceptor.Logger.DEFAULT.log("long body > $logCatMaxLength, skip showing.")
            } else {
                HttpLoggingInterceptor.Logger.DEFAULT.log("long body > $logCatMaxLength, try to show full content.")
                val sections = message.length / logCatMaxLength
                for (i in 0..sections) {
                    val max = logCatMaxLength * (i + 1)
                    if (max >= message.length) {
                        HttpLoggingInterceptor.Logger.DEFAULT.log(message.substring(logCatMaxLength * i))
                    } else {
                        HttpLoggingInterceptor.Logger.DEFAULT.log(message.substring(logCatMaxLength * i, max))
                    }
                }
            }
        } else {
            HttpLoggingInterceptor.Logger.DEFAULT.log(message)
        }
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

}