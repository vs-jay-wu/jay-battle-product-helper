package com.viewsonic.classswift.fixtures

import com.viewsonic.classswift.feature.quizcollection.ui.FolderRowKind
import com.viewsonic.classswift.feature.quizcollection.ui.FolderRowUi
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCardContent
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCardUi
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCollectionUiState

/**
 * Single source of mock data for the canvas / screenshot tests / (future) demo flavor.
 * Includes realistic content pressure (long text, missing subject, varied standards counts).
 */
object Samples {

    // Mirrors the native sidebar: a Default folder (selected), then the "Your folders"
    // expandable header, then user folders indented under it.
    private val folders = listOf(
        FolderRowUi("default", "Default", isSelected = true, kind = FolderRowKind.DEFAULT),
        FolderRowUi("your_folders", "Your folders", kind = FolderRowKind.YOUR_FOLDERS_HEADER, isExpanded = true),
        FolderRowUi("f1", "Science Grade 1", kind = FolderRowKind.FOLDER),
        FolderRowUi("f2", "Science Grade 2", kind = FolderRowKind.FOLDER),
        FolderRowUi("f3", "History", kind = FolderRowKind.FOLDER),
        FolderRowUi("f4", "Mathematics", kind = FolderRowKind.FOLDER),
    )

    private val quizzes = listOf(
        QuizCardUi(
            id = "q1",
            quizType = "Multiple choice",
            content = QuizCardContent.Text(
                "Which statement below is sometimes used to refer to the time period of the Tang Dynasty?",
            ),
            subject = "History",
            standardsCount = 2,
        ),
        QuizCardUi("q2", "True / False", QuizCardContent.Thumbnail(""), subject = "Science", standardsCount = 0),
        QuizCardUi(
            id = "q3",
            quizType = "Short answer",
            content = QuizCardContent.Text("Explain the water cycle in your own words."),
            subject = "Science",
            standardsCount = 3,
        ),
        QuizCardUi("q4", "Poll", QuizCardContent.Thumbnail(""), subject = null, standardsCount = 1),
        QuizCardUi(
            id = "q5",
            quizType = "Multiple choice",
            content = QuizCardContent.Text("What is 7 × 8?"),
            subject = "Mathematics",
            standardsCount = 0,
        ),
        QuizCardUi("q6", "Sketch response", QuizCardContent.Thumbnail(""), subject = "Art", standardsCount = 0),
    )

    val loading: QuizCollectionUiState = QuizCollectionUiState.Loading
    val error: QuizCollectionUiState = QuizCollectionUiState.Error()
    val empty: QuizCollectionUiState = QuizCollectionUiState.Content(folders = folders, quizzes = emptyList())
    val populated: QuizCollectionUiState = QuizCollectionUiState.Content(folders = folders, quizzes = quizzes)
}
