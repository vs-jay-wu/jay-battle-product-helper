package com.viewsonic.classswift.data.info

import com.viewsonic.classswift.api.response.data.QuizCollectionFolder

data class QuizCollectionFolderInfo(
    val folder: QuizCollectionFolder = QuizCollectionFolder(),
    val isSelected: Boolean = false
)
