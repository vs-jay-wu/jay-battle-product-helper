package com.viewsonic.classswift.feature.quizcollection.ui

/**
 * UI-facing models for the MVB Quiz Collection screen — platform-neutral, no Android types.
 * Shaped after data/info/QuizInCollectionInfo + QuizCollectionFolderInfo, but holding only
 * what the UI renders (already-resolved strings, no API response objects).
 */

/** A folder row in the sidebar. */
data class FolderRowUi(
    val id: String,
    val name: String,
    val isSelected: Boolean = false,
    val isOpen: Boolean = false,
)

/** Body of a quiz card: either rendered text, or an image thumbnail. */
sealed interface QuizCardContent {
    data class Text(val text: String) : QuizCardContent

    /** Image-backed quiz. Phase 1 renders a placeholder box (no network image in preview). */
    data object Thumbnail : QuizCardContent
}

/** A quiz card in the grid. */
data class QuizCardUi(
    val id: String,
    val quizType: String,
    val content: QuizCardContent,
    val subject: String? = null,
    val standardsCount: Int = 0,
)

/** Screen state. "Empty" is simply [Content] with no quizzes (a folder with no questions). */
sealed interface QuizCollectionUiState {
    data object Loading : QuizCollectionUiState

    data class Error(
        val title: String = "Failed to find quiz collection",
        val message: String = "Try to refresh this page again",
    ) : QuizCollectionUiState

    data class Content(
        val folders: List<FolderRowUi> = emptyList(),
        val quizzes: List<QuizCardUi> = emptyList(),
    ) : QuizCollectionUiState
}

/** One-way events out of the stateless screen (no-ops in design mode; mock nav in interactive). */
sealed interface QuizCollectionEvent {
    data class QuizClicked(val id: String) : QuizCollectionEvent
    data class FolderClicked(val id: String) : QuizCollectionEvent
    data object RefreshClicked : QuizCollectionEvent
}
