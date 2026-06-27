package com.viewsonic.classswift.data.enum

import com.viewsonic.classswift.data.info.ClassroomInfo

enum class ClassType {
    ROSTER,       // origin_type: google_classroom / classlink / canvas
    TW_DEFAULT,   // login_type: ""（空字串）
    SSO_GOOGLE,   // login_type: "google"
    GUEST,        // login_type: "guest"，或 isGuestMode（未登入）
    OTHER;        // 其他（fallback）

    companion object {
        fun from(classroomInfo: ClassroomInfo, isGuestMode: Boolean): ClassType = when {
            classroomInfo.isRoster()                           -> ROSTER
            classroomInfo.loginType == "guest" || isGuestMode -> GUEST
            classroomInfo.loginType.isEmpty()                  -> TW_DEFAULT
            classroomInfo.loginType == "google"                -> SSO_GOOGLE
            else                                               -> OTHER
        }
    }

    /** 是否使用分數格式（joined / total） */
    fun showFractionCount(hasPreRoster: Boolean): Boolean = when (this) {
        ROSTER, TW_DEFAULT -> true
        SSO_GOOGLE         -> hasPreRoster
        else               -> false
    }
}
