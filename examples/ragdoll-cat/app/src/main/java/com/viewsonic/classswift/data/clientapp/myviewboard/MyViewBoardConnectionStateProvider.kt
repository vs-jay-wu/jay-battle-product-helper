package com.viewsonic.classswift.data.clientapp.myviewboard

import com.viewsonic.classswift.service.ClassSwiftService
import kotlinx.coroutines.flow.StateFlow

interface MyViewBoardConnectionStateProvider {
    fun isBound(): Boolean

    /** 反應式 bind 狀態，供需要在 bind/unbind 即時更新的 UI 使用（VSFT-8437）。 */
    fun isBoundFlow(): StateFlow<Boolean>
}

class MyViewBoardConnectionStateProviderImpl : MyViewBoardConnectionStateProvider {
    override fun isBound(): Boolean = ClassSwiftService.isMyViewBoardBound()
    override fun isBoundFlow(): StateFlow<Boolean> = ClassSwiftService.isMyViewBoardBoundFlow
}
