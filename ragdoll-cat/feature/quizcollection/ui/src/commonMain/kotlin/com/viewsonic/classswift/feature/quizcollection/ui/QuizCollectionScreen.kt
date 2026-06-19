package com.viewsonic.classswift.feature.quizcollection.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.viewsonic.classswift.core.designsystem.AppTheme
import com.viewsonic.classswift.core.ui.ChipVariant
import com.viewsonic.classswift.core.ui.MvbQuizTagChip
import com.viewsonic.classswift.core.ui.designNode

/**
 * MVB Quiz Collection — stateless screen. Pure function of [QuizCollectionUiState];
 * emits [QuizCollectionEvent] only. No Android-only APIs, so it renders in the desktop
 * canvas and ships via ComposeView (Phase 3).
 */
@Composable
fun QuizCollectionScreen(
    state: QuizCollectionUiState,
    onEvent: (QuizCollectionEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppTheme {
        Surface(modifier = modifier.fillMaxSize(), color = AppTheme.tokens.colors.neutral100) {
            when (state) {
                is QuizCollectionUiState.Loading -> QuizCollectionLoading()
                is QuizCollectionUiState.Error ->
                    QuizCollectionError(
                        onRefresh = { onEvent(QuizCollectionEvent.RefreshClicked) },
                        title = state.title,
                        message = state.message,
                    )
                is QuizCollectionUiState.Content -> ContentLayout(state, onEvent)
            }
        }
    }
}

@Composable
private fun ContentLayout(
    state: QuizCollectionUiState.Content,
    onEvent: (QuizCollectionEvent) -> Unit,
) {
    Row(Modifier.fillMaxSize()) {
        FolderSidebar(state.folders, onEvent)
        Box(Modifier.weight(1f).fillMaxHeight()) {
            if (state.quizzes.isEmpty()) {
                QuizCollectionEmpty()
            } else {
                QuizGrid(state.quizzes, onEvent)
            }
        }
    }
}

@Composable
private fun FolderSidebar(
    folders: List<FolderRowUi>,
    onEvent: (QuizCollectionEvent) -> Unit,
) {
    val tokens = AppTheme.tokens
    Column(
        Modifier
            .width(171.dp)
            .fillMaxHeight()
            .background(tokens.colors.neutral0),
    ) {
        folders.forEach { folder ->
            FolderRow(folder) { onEvent(QuizCollectionEvent.FolderClicked(folder.id)) }
        }
    }
}

@Composable
private fun FolderRow(folder: FolderRowUi, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    val bg = if (folder.isSelected) tokens.colors.neutral300 else tokens.colors.neutral0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = tokens.spacing.s400)
            .designNode("folder_${folder.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            Modifier
                .size(13.33.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(tokens.colors.neutral900),
        )
        Spacer(Modifier.width(tokens.spacing.s300))
        Text(
            text = folder.name,
            color = tokens.colors.neutral900,
            fontSize = tokens.type.sm,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QuizGrid(
    quizzes: List<QuizCardUi>,
    onEvent: (QuizCollectionEvent) -> Unit,
) {
    val tokens = AppTheme.tokens
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize().designNode("quiz_grid"),
        contentPadding = PaddingValues(tokens.spacing.s300),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s300),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s300),
    ) {
        items(quizzes, key = { it.id }) { quiz ->
            QuizCard(quiz) { onEvent(QuizCollectionEvent.QuizClicked(quiz.id)) }
        }
    }
}

@Composable
fun QuizCard(quiz: QuizCardUi, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    val images = LocalQuizCollectionImages.current
    Column(
        modifier = modifier
            .width(150.dp)
            .height(168.dp)
            .clip(RoundedCornerShape(tokens.radius.r600))
            .background(tokens.colors.neutral0)
            .clickable(onClick = onClick)
            .designNode("quiz_card_${quiz.id}"),
    ) {
        // Header: quiz-type icon + label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = tokens.spacing.s400, top = tokens.spacing.s400, end = tokens.spacing.s400),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            images.QuizTypeIcon(Modifier.size(16.dp))
            Spacer(Modifier.width(tokens.spacing.s150))
            Text(
                text = quiz.quizType,
                color = tokens.colors.neutral900,
                fontSize = tokens.type.sm,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.designNode("quiz_type_${quiz.id}"),
            )
        }

        // Body: thumbnail placeholder or text content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = tokens.spacing.s400, top = tokens.spacing.s300, end = tokens.spacing.s400)
                .clip(RoundedCornerShape(tokens.radius.r400))
                .background(tokens.colors.neutral300),
        ) {
            when (val content = quiz.content) {
                is QuizCardContent.Text -> Text(
                    text = content.text,
                    color = tokens.colors.neutral900,
                    fontSize = tokens.type.sm,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(tokens.spacing.s300),
                )
                is QuizCardContent.Thumbnail -> images.Thumbnail(content.imageUrl, Modifier.fillMaxSize())
            }
        }

        // Tags: subject + standards
        Column(
            modifier = Modifier.padding(
                start = tokens.spacing.s400,
                end = tokens.spacing.s400,
                top = tokens.spacing.s300,
                bottom = tokens.spacing.s400,
            ),
        ) {
            quiz.subject?.let { subject ->
                val subjectVariant = if (quiz.subjectIsGeneral) ChipVariant.SUBJECT_GENERAL else ChipVariant.SUBJECT
                MvbQuizTagChip(
                    text = subject,
                    variant = subjectVariant,
                    leadingIcon = { tint -> images.ChipIcon(subjectVariant, tint, Modifier.size(10.66.dp)) },
                )
            }
            if (quiz.standardsCount > 0) {
                Spacer(Modifier.height(tokens.spacing.s150))
                MvbQuizTagChip(
                    text = "${quiz.standardsCount} standards",
                    variant = ChipVariant.STANDARDS,
                    leadingIcon = { tint -> images.ChipIcon(ChipVariant.STANDARDS, tint, Modifier.size(10.66.dp)) },
                )
            }
        }
    }
}

@Composable
fun QuizCollectionEmpty(modifier: Modifier = Modifier) {
    val tokens = AppTheme.tokens
    StateColumn(modifier) {
        LocalQuizCollectionImages.current.StateIllustration(QuizStateIllustration.EMPTY, Modifier.size(100.dp))
        Spacer(Modifier.height(tokens.spacing.s300))
        Text("No questions here yet", color = tokens.colors.neutral900, fontSize = tokens.type.xl, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(tokens.spacing.s150))
        Text("Try switching folders to view other questions", color = tokens.colors.neutral900, fontSize = tokens.type.lg)
    }
}

@Composable
fun QuizCollectionError(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Failed to find quiz collection",
    message: String = "Try to refresh this page again",
) {
    val tokens = AppTheme.tokens
    StateColumn(modifier) {
        LocalQuizCollectionImages.current.StateIllustration(QuizStateIllustration.ERROR, Modifier.size(100.dp))
        Spacer(Modifier.height(tokens.spacing.s300))
        Text(title, color = tokens.colors.neutral900, fontSize = tokens.type.xl, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(tokens.spacing.s150))
        Text(message, color = tokens.colors.neutral900, fontSize = tokens.type.lg)
        Spacer(Modifier.height(tokens.spacing.s900))
        Box(
            modifier = Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(tokens.radius.r800))
                .background(tokens.colors.primary)
                .clickable { onRefresh() }
                .padding(horizontal = tokens.spacing.s900)
                .designNode("qc_refresh_button"),
            contentAlignment = Alignment.Center,
        ) {
            Text("Refresh", color = tokens.colors.neutral0, fontSize = tokens.type.md, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun QuizCollectionLoading(modifier: Modifier = Modifier) {
    val tokens = AppTheme.tokens
    StateColumn(modifier) {
        CircularProgressIndicator(color = tokens.colors.primary)
        Spacer(Modifier.height(tokens.spacing.s300))
        Text("Loading…", color = tokens.colors.neutral900, fontSize = tokens.type.lg)
    }
}

@Composable
private fun StateColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}
