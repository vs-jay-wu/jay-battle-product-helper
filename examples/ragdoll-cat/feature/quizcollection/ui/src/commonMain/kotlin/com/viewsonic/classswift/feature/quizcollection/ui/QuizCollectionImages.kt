package com.viewsonic.classswift.feature.quizcollection.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import com.viewsonic.classswift.core.designsystem.AppTheme
import com.viewsonic.classswift.core.ui.ChipVariant
import com.viewsonic.classswift.feature.quizcollection.ui.generated.resources.Res
import com.viewsonic.classswift.feature.quizcollection.ui.generated.resources.ic_mvb_qc_quiz_type
import com.viewsonic.classswift.feature.quizcollection.ui.generated.resources.ic_mvb_qc_standards
import com.viewsonic.classswift.feature.quizcollection.ui.generated.resources.ic_mvb_qc_subject
import com.viewsonic.classswift.feature.quizcollection.ui.generated.resources.img_mvb_qc_empty
import com.viewsonic.classswift.feature.quizcollection.ui.generated.resources.img_mvb_qc_error
import org.jetbrains.compose.resources.painterResource

enum class QuizStateIllustration { EMPTY, ERROR }

/**
 * Images for the Quiz Collection UI. The default loads the real ClassSwift vector assets bundled
 * in this module's compose resources (so the desktop canvas shows real images too); the Android
 * app overrides only [Thumbnail] with Coil (per-quiz network images).
 */
interface QuizCollectionImages {
    @Composable fun StateIllustration(kind: QuizStateIllustration, modifier: Modifier)

    @Composable fun QuizTypeIcon(modifier: Modifier)

    @Composable fun ChipIcon(variant: ChipVariant, tint: Color, modifier: Modifier)

    @Composable fun Thumbnail(imageUrl: String, modifier: Modifier)
}

/** Real bundled vector assets; thumbnails fall back to a neutral box (network image, no URL offline). */
object DefaultQuizCollectionImages : QuizCollectionImages {
    @Composable
    override fun StateIllustration(kind: QuizStateIllustration, modifier: Modifier) {
        val res = when (kind) {
            QuizStateIllustration.EMPTY -> Res.drawable.img_mvb_qc_empty
            QuizStateIllustration.ERROR -> Res.drawable.img_mvb_qc_error
        }
        Image(painter = painterResource(res), contentDescription = null, modifier = modifier)
    }

    @Composable
    override fun QuizTypeIcon(modifier: Modifier) {
        // Not tinted — the quiz-type icon is multi-color (matches the original ImageView, no tint).
        Image(
            painter = painterResource(Res.drawable.ic_mvb_qc_quiz_type),
            contentDescription = null,
            modifier = modifier,
        )
    }

    @Composable
    override fun ChipIcon(variant: ChipVariant, tint: Color, modifier: Modifier) {
        val res = when (variant) {
            ChipVariant.STANDARDS -> Res.drawable.ic_mvb_qc_standards
            ChipVariant.SUBJECT, ChipVariant.SUBJECT_GENERAL -> Res.drawable.ic_mvb_qc_subject
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
        Box(modifier.background(AppTheme.tokens.colors.neutral300))
    }
}

val LocalQuizCollectionImages = staticCompositionLocalOf<QuizCollectionImages> {
    DefaultQuizCollectionImages
}
