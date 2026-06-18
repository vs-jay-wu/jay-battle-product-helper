package com.viewsonic.classswift.data.clientapp.myviewboard.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MyViewBoardSessionStore {
    private val _mvbToken = MutableStateFlow("")
    val mvbToken: StateFlow<String> = _mvbToken.asStateFlow()

    fun updateToken(token: String) {
        _mvbToken.value = token
    }

    fun clearToken() {
        _mvbToken.value = ""
    }
}
