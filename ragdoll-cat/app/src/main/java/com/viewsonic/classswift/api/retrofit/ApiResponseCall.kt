package com.viewsonic.classswift.api.retrofit

import com.viewsonic.classswift.api.error.Rfc7807Error
import com.viewsonic.classswift.api.moshi.MoshiProvider
import com.viewsonic.classswift.api.retrofit.ApiResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Invocation
import retrofit2.Response
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ApiResponseCall(
    private val delegate: Call<Any>
) : Call<Any> by delegate {
    override fun execute(): Response<Any> {
        return try {
            parseResponse(delegate.execute())
        } catch (e: Exception) {
            Response.success(ApiResponse.ExceptionFailure<Any>(e))
        }
    }

    override fun enqueue(callback: Callback<Any>) {
        delegate.enqueue(object : Callback<Any> {
            override fun onResponse(call: Call<Any>, response: Response<Any>) {
                return callback.onResponse(this@ApiResponseCall, parseResponse(response))
            }

            override fun onFailure(call: Call<Any>, t: Throwable) {
                when (t) {
                    is SocketException,
                    is SocketTimeoutException,
                    is UnknownHostException -> {
                        callback.onResponse(this@ApiResponseCall, Response.success(ApiResponse.NetworkDisconnected<Any>()))
                    }
                    else -> {
                        callback.onResponse(this@ApiResponseCall, Response.success(ApiResponse.ExceptionFailure<Any>(Exception(t))))
                    }
                }
            }
        })
    }

    private fun parseResponse(response: Response<Any>): Response<Any> {
        if (response.isSuccessful) {
            val body = response.body()
            return if (body == null) {
                val invocation = delegate.request().tag(Invocation::class.java)!!
                val method = invocation.method()
                val e = KotlinNullPointerException("Response from " +
                        method.declaringClass.name +
                        '.' +
                        method.name +
                        " was null but response body type was declared as non-null")
                Response.success(ApiResponse.ExceptionFailure<Any>(e), response.raw())
            } else {
                Response.success(ApiResponse.Success(body, response), response.raw())
            }
        } else {
            if (isRfc7807Response(response.headers()["Content-Type"])) {
                return Response.success(ApiResponse.Rfc7807Failure<Any>(
                    response.code(),
                    MoshiProvider.fromJson(Rfc7807Error::class.java, response.errorBody()?.string() ?: "{}", true))
                )
            }
            return Response.success(ApiResponse.HttpFailure<Any>(response.code(), response))
        }
    }

    private fun isRfc7807Response(contentType: String?) =
        contentType?.contains("application/problem+json") ?: false
}