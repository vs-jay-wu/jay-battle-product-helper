package com.viewsonic.classswift.ui.window.quiz.mvb

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.viewsonic.classswift.core.ui.ChipVariant
import com.viewsonic.classswift.feature.quizcollection.ui.DefaultQuizCollectionImages
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCollectionImages
import com.viewsonic.classswift.feature.quizcollection.ui.QuizStateIllustration

/**
 * Android image provider for the Compose Quiz Collection body. Static assets (icons + illustrations)
 * come from the shared bundled vectors via [DefaultQuizCollectionImages]; only per-quiz thumbnails
 * are platform-specific (Coil network load).
 */
object AppQuizCollectionImages : QuizCollectionImages {

    @Composable
    override fun StateIllustration(kind: QuizStateIllustration, modifier: Modifier) =
        DefaultQuizCollectionImages.StateIllustration(kind, modifier)

    @Composable
    override fun QuizTypeIcon(modifier: Modifier) =
        DefaultQuizCollectionImages.QuizTypeIcon(modifier)

    @Composable
    override fun ChipIcon(variant: ChipVariant, tint: Color, modifier: Modifier) =
        DefaultQuizCollectionImages.ChipIcon(variant, tint, modifier)

    @Composable
    override fun Thumbnail(imageUrl: String, modifier: Modifier) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}
