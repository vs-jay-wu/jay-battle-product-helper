package com.viewsonic.classswift.data.task

import com.viewsonic.classswift.api.error.Rfc7807Error

data class TaskApiResult<T>(
    val data: T,
    val isSuccess: Boolean,
    val errMsg: String = "",
    val errorBody: Rfc7807Error? = null
)
