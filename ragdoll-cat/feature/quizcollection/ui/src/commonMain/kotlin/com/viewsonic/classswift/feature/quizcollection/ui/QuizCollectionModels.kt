package com.viewsonic.classswift.feature.quizcollection.ui

/**
 * UI-facing models for the MVB Quiz Collection screen — platform-neutral, no Android types.
 * Shaped after data/info/QuizInCollectionInfo + QuizCollectionFolderInfo, but holding only
 * what the UI renders (already-resolved strings, no API response objects).
 */

/** What kind of sidebar row this is — mirrors the native `MvbQuizCollectionFolderListAdapter`. */
enum class FolderRowKind {
    /** The default folder (top), uses the `ic_mvb_qc_default` icon. */
    DEFAULT,

    /** The "Your folders" expandable section header (person icon + chevron). */
    YOUR_FOLDERS_HEADER,

    /** A user folder under "Your folders" (folder icon, indented). */
    FOLDER,
}

/** A folder row in the sidebar. */
data class FolderRowUi(
    val id: String,
    val name: String,
    val isSelected: Boolean = false,
    val isOpen: Boolean = false,
    val kind: FolderRowKind = FolderRowKind.FOLDER,
    /** Only meaningful for [FolderRowKind.YOUR_FOLDERS_HEADER]: whether the section is expanded. */
    val isExpanded: Boolean = true,
)

/** Body of a quiz card: either rendered text, or an image thumbnail. */
sealed interface QuizCardContent {
    data class Text(val text: String) : QuizCardContent

    /** Image-backed quiz; [imageUrl] is loaded by the host (Coil on Android; placeholder in preview). */
    data class Thumbnail(val imageUrl: String) : QuizCardContent
}

/** A quiz card in the grid. */
data class QuizCardUi(
    val id: String,
    val quizType: String,
    val content: QuizCardContent,
    val subject: String? = null,
    /** When true the subject chip uses the muted "general" tint (no specific subject). */
    val subjectIsGeneral: Boolean = false,
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
    data object YourFoldersToggled : QuizCollectionEvent
    data object RefreshClicked : QuizCollectionEvent
}
