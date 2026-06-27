package com.viewsonic.classswift.api

import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.api.interceptor.CustomLoggingInterceptor
import okhttp3.OkHttpClient

class OkHttpBuilder {

    fun build(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        builder.apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(CustomLoggingInterceptor.getLargeHttpLoggingInterceptor())
            }
        }
        return builder.build()
    }
}