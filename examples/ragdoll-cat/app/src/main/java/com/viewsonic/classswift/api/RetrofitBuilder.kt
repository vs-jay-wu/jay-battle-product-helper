package com.viewsonic.classswift.api

import com.viewsonic.classswift.BuildConfig
import com.viewsonic.classswift.api.interceptor.CustomLoggingInterceptor
import com.viewsonic.classswift.api.moshi.MoshiProvider
import com.viewsonic.classswift.api.retrofit.ApiResponseCallAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitBuilder(private val baseUrl: String = BuildConfig.BASE_URL) {
    private val timeoutPeriod = 10L

    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        builder.apply {
            connectTimeout(timeoutPeriod, TimeUnit.SECONDS)
            readTimeout(timeoutPeriod, TimeUnit.SECONDS)
            writeTimeout(timeoutPeriod, TimeUnit.SECONDS)
            if (BuildConfig.DEBUG) {
                addInterceptor(CustomLoggingInterceptor.getLargeHttpLoggingInterceptor())
            }
        }
        return builder.build()
    }

    // Build and provide Retrofit instance
    fun build(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(MoshiConverterFactory.create(MoshiProvider.moshiDefaultIfNull))
            .addCallAdapterFactory(ApiResponseCallAdapterFactory())
            .build()
    }
}