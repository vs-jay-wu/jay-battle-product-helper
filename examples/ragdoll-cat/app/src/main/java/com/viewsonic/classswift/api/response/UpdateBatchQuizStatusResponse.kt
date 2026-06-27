package com.viewsonic.classswift.api.response

import com.squareup.moshi.JsonClass
import com.viewsonic.classswift.data.batchquiz.BatchQuizStatus

@JsonClass(generateAdapter = true)
data class UpdateBatchQuizStatusResponse(
    val status: BatchQuizStatus = BatchQuizStatus.UNSPECIFIED,
)