package com.viewsonic.classswift.utils.extension

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

suspend fun <T, R> Flow<T>.mapAndCollect(
    transform: suspend T.() -> R,
    flowCollector: FlowCollector<R>
) {
    return map(transform)
        .distinctUntilChanged()
        .collect(flowCollector)
}