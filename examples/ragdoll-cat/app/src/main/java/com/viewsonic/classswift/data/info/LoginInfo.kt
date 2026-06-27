package com.viewsonic.classswift.data.info

class LoginInfo(var accessToken: String = "", var idToken: String = "", var refreshToken: String = "") {
    fun clear() {
        accessToken = ""
        idToken = ""
        refreshToken = ""
    }
}