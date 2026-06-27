package com.viewsonic.classswift.constant

object ApiConstant {
    enum class UserPreferenceType(val typeInServer: String) {
        QUIZ_COLLECTION_KNSH("quiz_collection_knsh"),
        LEADERBOARD("leaderboard"),
        QUIZ("quiz"),
        TRANSLATION_TOOL("translation_tool"),
        TUTORIAL("tutorial"),
        QUIZ_COLLECTION_FILTER("quiz_collection_filter"),
    }

    /** Query values for `GET /api/v3/rooms?sort_type=…` (OpenAPI `RoomSortType`). */
    enum class RoomSortType(val queryValue: String) {
        LATEST_IN_CLASS_LESSON("LATEST_IN_CLASS_LESSON"),
        LATEST_POST_CLASS_LESSON("LATEST_POST_CLASS_LESSON"),
        LATEST_ACTIVITY("LATEST_ACTIVITY"),
    }
}
