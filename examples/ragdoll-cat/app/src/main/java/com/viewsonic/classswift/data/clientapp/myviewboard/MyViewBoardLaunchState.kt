package com.viewsonic.classswift.data.clientapp.myviewboard

import com.viewsonic.classswift.data.clientapp.myviewboard.session.MyViewBoardSessionStore
import kotlinx.coroutines.flow.StateFlow

object MyViewBoardLaunchState {
    val mvbToken: StateFlow<String> = MyViewBoardSessionStore.mvbToken

    fun updateToken(token: String) {
        MyViewBoardSessionStore.updateToken(token)
    }

    fun clearToken() {
        MyViewBoardSessionStore.clearToken()
    }
}
