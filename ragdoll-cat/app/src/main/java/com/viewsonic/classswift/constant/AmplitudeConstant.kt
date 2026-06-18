package com.viewsonic.classswift.constant

object AmplitudeConstant {
    object EventName {
        const val LOGIN_METHOD_SELECTED = "Login Method Selected"
        const val LOGIN = "Login"
        const val LOGIN_FAILED = "Login Failed"
        const val ORGANIZATION_SELECTED = "Organization Selected"
        const val LESSON_START = "Lesson Start"
        const val LESSON_END = "Lesson End"
        const val QUIZ_START = "Quiz Start"
        const val QUIZ_END = "Quiz End"
        const val DISCONNECT = "Disconnect"
        const val RECONNECT = "Reconnect"
        const val RECONNECT_PROMPT_SHOWN = "Reconnect Prompt Shown"
        const val SCREENSHOT_STARTED = "Screenshot Started"
        const val SCREENSHOT_ENDED = "Screenshot Ended"
        const val SCREENSHOT_CANCELED = "Screenshot Canceled"
        const val SPINNER_CLICKED = "Spinner Clicked"
        const val SPINNER_REMOVED_CLICKED = "Spinner Removed Clicked"
        const val LEADERBOARD_CLICKED = "Leaderboard Clicked"
        const val BATCH_QUIZ_START = "Batch Quiz Start"
        const val BATCH_QUIZ_ENDED = "Batch Quiz Ended"
    }

    object EventProperties {
        object Key {
            const val FAILED_MESSAGE = "failed message"
            const val SOURCE_PROJECT = "source project"
            const val ROOM_ID = "room id"
            const val TEACHER_ID = "teacher id"
            const val ROOM_NAME = "room name"
            const val LESSON_ID = "lesson id"
            const val LESSON_START_TIMESTAMP = "lesson start timestamp"
            const val LESSON_START_DATETIME = "lesson start datetime"
            const val IS_SCORE_IN_LESSON = "is score in lesson"
            const val STUDENT_DEFAULT_COUNT = "student default count"
            const val STUDENT_ATTEND_COUNT = "student attend count"
            const val STUDENT_ATTEND_LIST = "student attend list"
            const val QUIZ_ID = "quiz id"
            const val IMG_KEY = "img key"
            const val QUIZ_TYPE = "quiz type"
            const val QUIZ_SOURCE = "quiz source"
            const val QUIZ_STATUS = "quiz status"
            const val QUIZ_START_TIMESTAMP = "quiz start timestamp"
            const val QUIZ_START_DATETIME = "quiz start datetime"
            const val STUDENT_ANSWER = "student answer" // 	Count of students who have submitted an answer
            const val STUDENT_UNANSWER = "student unanswer" // 	Count of students who have not submitted an answer
            const val STUDENT_ANSWER_CORRECT = "student answer correct" // 	Count of students who have submitted a correct answer
            const val SYS_ERROR_MESSAGE = "sys error message"
            const val SYS_ERROR_CODE = "sys error code"
            const val SCREENSHOT_SOURCE = "screenshot source"
            const val CANCEL_METHOD = "cancel method"
            const val STATUS = "status"
            const val TOTAL_QUESTION_COUNT = "total_question_count"
            const val BATCH_QUIZ_ID = "batch_quiz_id"
            const val QUIZ_DETAIL_LIST = "quiz_detail_list"
        }
        object Value {
            const val GET_REFRESH_TOKEN_FAILED = "Get RefreshToken Failed"
            const val GET_PKCE_FAILED = "Get PKCE Failed"
            const val GET_USER_INFO_FAILED = "Get UserInfo Failed"
            const val GET_LOGIN_URLS_FAILED = "Get Login Urls Failed"
            const val GET_QRCODE_URL_FAILED = "Get QRCode Url Failed"
            const val ANDROID = "Android"
            const val TASK = "TASK"
            const val SELECT_AGAIN = "SELECT AGAIN"
            const val CANCEL = "cancel"
            const val SUCCESS = "success"
            const val FAILURE = "failure"
        }
    }

    object UserProperties {
        object Key {
            const val LOGIN_METHOD = "login method"
            const val FIREBASE_INSTALLATION_ID = "firebase installation id"
            const val USER_ID = "user id"
            const val DISPLAY_NAME = "display name"
            const val CLASSSWIFT_COUNTRY = "classswift country"
            const val CURRENT_ORG_ID = "current org id"
            const val CURRENT_ORG_NAME = "current org name"
            const val CURRENT_ORG_IS_INDIVIDUAL = "current org is individual"
            const val CURRENT_PLAN_TYPE = "current plan type"
            const val CURRENT_PLAN_END_DATE = "current plan end date" // ISO 8601 format with timezone UTC+0 => YYYY-MM-DDTHH:mm:ss
            const val ORGS_DETAIL = "orgs detail"
        }
        object Value {
            const val VIEW_SONIC = "ViewSonic"
            const val CLASS_LINK = "ClassLink"
            const val GOOGLE = "Google"
            const val MICROSOFT = "Microsoft"
            const val QR_CODE = "QR code"
            const val AUTOMATIC = "Automatic"
        }
    }

    enum class LoginMethod(val amplitudePropertyValue: String) {
        Automatic(UserProperties.Value.AUTOMATIC),
        ViewSonic(UserProperties.Value.VIEW_SONIC),
        ClassLink(UserProperties.Value.CLASS_LINK),
        Google(UserProperties.Value.GOOGLE),
        Microsoft(UserProperties.Value.MICROSOFT),
        QrCode(UserProperties.Value.QR_CODE),
    }
}
