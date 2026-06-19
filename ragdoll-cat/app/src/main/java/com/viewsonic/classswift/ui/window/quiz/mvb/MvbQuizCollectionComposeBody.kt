package com.viewsonic.classswift.ui.window.quiz.mvb

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.viewsonic.classswift.R
import com.viewsonic.classswift.core.designsystem.AppTheme
import com.viewsonic.classswift.data.info.QuizInCollectionInfo
import com.viewsonic.classswift.data.quiz.QuizType
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCard
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCardContent
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCardUi
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCollectionEmpty
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCollectionError
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCollectionLoading
import com.viewsonic.classswift.feature.quizcollection.ui.LocalQuizCollectionImages
import com.viewsonic.classswift.ui.windowmodel.MvbQuizCollectionWindowModel

private const val QUIZ_GRID_SPAN = 4

/**
 * Compose replacement for the quiz-list body of MvbQuizCollectionWindow.
 *
 * Drives the SAME [QuizCard] / state composables used by the desktop canvas, but from the real
 * Paging 3 flow + window UiState — so the shipped list and the design preview are one codebase
 * (no drift). Folder sidemenu / header / breadcrumb / detail overlay remain native.
 */
@Composable
fun MvbQuizCollectionComposeBody(windowModel: MvbQuizCollectionWindowModel) {
    val uiState by windowModel.uiStateFlow.collectAsState()
    val quizzes = windowModel.quizzesPagingDataFlow.collectAsLazyPagingItems()
    val context = LocalContext.current

    AppTheme {
      CompositionLocalProvider(LocalQuizCollectionImages provides AppQuizCollectionImages) {
        val refresh = quizzes.loadState.refresh
        when {
            uiState.folderLoadFailed || refresh is LoadState.Error ->
                QuizCollectionError(onRefresh = { windowModel.refreshAfterError() })

            uiState.isLoadingFolders || refresh is LoadState.Loading ->
                QuizCollectionLoading()

            quizzes.itemCount == 0 ->
                QuizCollectionEmpty()

            else ->
                LazyVerticalGrid(
                    columns = GridCells.Fixed(QUIZ_GRID_SPAN),
                    modifier = Modifier.fillMaxSize().padding(AppTheme.tokens.spacing.s400),
                ) {
                    items(quizzes.itemCount) { index ->
                        val info = quizzes[index] ?: return@items
                        QuizCard(
                            quiz = mapQuizCardUi(info, context, uiState.canUseStandards),
                            onClick = { windowModel.selectQuiz(info) },
                        )
                    }
                    if (quizzes.loadState.append is LoadState.Loading) {
                        item(span = { GridItemSpan(maxLineSpan) }) { QuizCollectionLoading() }
                    }
                }
        }
      }
    }
}

/** Resolves the app's paged quiz model to the platform-neutral [QuizCardUi] (strings resolved here). */
private fun mapQuizCardUi(
    info: QuizInCollectionInfo,
    context: Context,
    canUseStandards: Boolean,
): QuizCardUi {
    val displayName = info.subjectDisplayName.ifBlank { info.quizData.subject }
    val isGeneral = displayName.isBlank()
    return QuizCardUi(
        id = info.quizData.id,
        quizType = context.getString(quizTypeLabelResId(QuizType.safeValueOf(info.quizData.quizType))),
        content = if (info.isTextQuiz()) {
            QuizCardContent.Text(info.quizData.content)
        } else {
            QuizCardContent.Thumbnail(info.quizData.imgUrl)
        },
        subject = if (isGeneral) context.getString(R.string.mvb_qc_quiz_subject_general) else displayName.toSubjectTitleCase(),
        subjectIsGeneral = isGeneral,
        standardsCount = if (canUseStandards) info.standardsCount else 0,
    )
}

private fun String.toSubjectTitleCase(): String =
    split("_", " ").filter { it.isNotEmpty() }.joinToString(" ") { word ->
        word.lowercase().replaceFirstChar(Char::titlecase)
    }

private fun quizTypeLabelResId(type: QuizType): Int = when (type) {
    QuizType.SINGLE_SELECT,
    QuizType.MULTIPLE_SELECT,
    QuizType.UNSPECIFIED -> R.string.quiz_types_multiple_choice
    QuizType.TRUE_FALSE -> R.string.quiz_types_true_false
    QuizType.SHORT_ANSWER -> R.string.quiz_types_short_answer
    QuizType.SKETCH_RESPONSE -> R.string.quiz_types_sketch_response
    QuizType.RECORD -> R.string.quiz_types_audio
    QuizType.SINGLE_POLL,
    QuizType.MULTIPLE_POLL -> R.string.quiz_types_poll
}
