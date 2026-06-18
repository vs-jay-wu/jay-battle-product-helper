package com.viewsonic.classswift.api.amazon

import com.viewsonic.classswift.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class AmazonClient(private val okHttpClient: OkHttpClient) {
    fun build(): Retrofit {
        return Retrofit.Builder()
                .baseUrl(BuildConfig.PLACEHOLDER_BASE_URL)
                .client(okHttpClient)
                .build()
    }
}