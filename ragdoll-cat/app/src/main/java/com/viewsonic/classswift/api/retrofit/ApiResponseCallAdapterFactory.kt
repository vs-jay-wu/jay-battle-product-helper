package com.viewsonic.classswift.api.retrofit

import com.squareup.moshi.rawType
import com.viewsonic.classswift.api.retrofit.ApiResponse
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class ApiResponseCallAdapterFactory: CallAdapter.Factory() {
    override fun get(returnType: Type, annotations: Array<out Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java) {
            return null
        }
        require(returnType is ParameterizedType) { "Call return type must be parameterized as Call<Foo> or Call<? extends Foo>" }

        return if (returnType.actualTypeArguments.first().rawType.isAssignableFrom(ApiResponse::class.java)) {
            val dataType = (returnType.actualTypeArguments.first() as ParameterizedType).actualTypeArguments.first()
            ApiResponseCallAdapter(dataType = dataType)
        } else {
            null
        }
    }
}