package com.viewsonic.classswift.feature.quizcollection.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.viewsonic.classswift.core.designsystem.AppTheme
import com.viewsonic.classswift.core.ui.ChipVariant

enum class QuizStateIllustration { EMPTY, ERROR }

/**
 * Host-supplied images for the Quiz Collection UI.
 *
 * Keeps the screen platform-neutral: the composables ask for images by meaning, and the host
 * provides the real assets — the Android app injects `R.drawable`/Coil, the desktop canvas uses
 * the [PlaceholderQuizCollectionImages] defaults. (See docs/desktop-app-architecture.md.)
 */
interface QuizCollectionImages {
    @Composable fun StateIllustration(kind: QuizStateIllustration, modifier: Modifier)

    @Composable fun QuizTypeIcon(modifier: Modifier)

    @Composable fun ChipIcon(variant: ChipVariant, tint: Color, modifier: Modifier)

    @Composable fun Thumbnail(imageUrl: String, modifier: Modifier)
}

/** Neutral grey placeholders — used by previews / the desktop canvas. */
object PlaceholderQuizCollectionImages : QuizCollectionImages {
    @Composable
    override fun StateIllustration(kind: QuizStateIllustration, modifier: Modifier) =
        Placeholder(modifier, AppTheme.tokens.colors.neutral300, 10.dp)

    @Composable
    override fun QuizTypeIcon(modifier: Modifier) =
        Placeholder(modifier, AppTheme.tokens.colors.neutral900, 2.dp)

    @Composable
    override fun ChipIcon(variant: ChipVariant, tint: Color, modifier: Modifier) =
        Placeholder(modifier, tint, 2.dp)

    @Composable
    override fun Thumbnail(imageUrl: String, modifier: Modifier) =
        Placeholder(modifier, AppTheme.tokens.colors.neutral300, 0.dp)
}

@Composable
private fun Placeholder(modifier: Modifier, color: Color, radius: androidx.compose.ui.unit.Dp) {
    Box(modifier.clip(RoundedCornerShape(radius)).background(color))
}

val LocalQuizCollectionImages = staticCompositionLocalOf<QuizCollectionImages> {
    PlaceholderQuizCollectionImages
}
