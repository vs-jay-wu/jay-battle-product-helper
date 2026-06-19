package com.viewsonic.classswift.ui.window.quiz.mvb

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.viewsonic.classswift.R
import com.viewsonic.classswift.core.designsystem.AppTheme
import com.viewsonic.classswift.core.ui.ChipVariant
import com.viewsonic.classswift.feature.quizcollection.ui.QuizCollectionImages
import com.viewsonic.classswift.feature.quizcollection.ui.QuizStateIllustration

/**
 * Real ClassSwift assets for the Compose Quiz Collection body — the same drawables the original
 * XML used (img_mvb_qc_*, ic_mvb_qc_*) and Coil for quiz thumbnails. Keeps the platform-neutral
 * feature module free of Android `R`/Coil; the desktop canvas uses the placeholder defaults.
 */
object AppQuizCollectionImages : QuizCollectionImages {

    @Composable
    override fun StateIllustration(kind: QuizStateIllustration, modifier: Modifier) {
        val res = when (kind) {
            QuizStateIllustration.EMPTY -> R.drawable.img_mvb_qc_empty
            QuizStateIllustration.ERROR -> R.drawable.img_mvb_qc_error
        }
        Image(painter = painterResource(res), contentDescription = null, modifier = modifier)
    }

    @Composable
    override fun QuizTypeIcon(modifier: Modifier) {
        Image(
            painter = painterResource(R.drawable.ic_mvb_qc_quiz_type),
            contentDescription = null,
            modifier = modifier,
            colorFilter = ColorFilter.tint(AppTheme.tokens.colors.neutral900),
        )
    }

    @Composable
    override fun ChipIcon(variant: ChipVariant, tint: Color, modifier: Modifier) {
        val res = when (variant) {
            ChipVariant.STANDARDS -> R.drawable.ic_mvb_qc_standards
            ChipVariant.SUBJECT, ChipVariant.SUBJECT_GENERAL -> R.drawable.ic_mvb_qc_subject
        }
        Image(
            painter = painterResource(res),
            contentDescription = null,
            modifier = modifier,
            colorFilter = ColorFilter.tint(tint),
        )
    }

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
