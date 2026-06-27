package com.viewsonic.classswift.api.retrofit

import com.viewsonic.classswift.api.error.Rfc7807Error
import retrofit2.Response

sealed class ApiResponse<D> {
    class Success<D>(val data: D, val retrofitResponse: Response<Any>) : ApiResponse<D>()
    class ExceptionFailure<D>(val exception: Exception) : ApiResponse<D>()
    class HttpFailure<D>(val responseCode: Int, val retrofitResponse: Response<Any>) : ApiResponse<D>()
    class Rfc7807Failure<D>(val responseCode: Int, val error: Rfc7807Error) : ApiResponse<D>()
    class NetworkDisconnected<D> : ApiResponse<D>()
}