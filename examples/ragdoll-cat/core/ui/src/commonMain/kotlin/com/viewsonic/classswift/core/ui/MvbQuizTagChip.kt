package com.viewsonic.classswift.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.viewsonic.classswift.core.designsystem.AppTheme

enum class ChipVariant { SUBJECT, SUBJECT_GENERAL, STANDARDS }

/**
 * Compose port of MvbQuizTagChipView: a tinted leading icon + label.
 * Tint follows the original (SUBJECT_GENERAL -> neutral650, else neutral900).
 *
 * Phase 1: the icon is a tinted placeholder box (the VSDS vector assets live in the
 * Android `:app` and aren't in common yet); shape/label/tint are faithful.
 */
@Composable
fun MvbQuizTagChip(
    text: String,
    variant: ChipVariant,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (tint: Color) -> Unit = { tint ->
        Spacer(
            Modifier
                .size(10.66.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(tint),
        )
    },
) {
    val tokens = AppTheme.tokens
    val tint = if (variant == ChipVariant.SUBJECT_GENERAL) tokens.colors.neutral650 else tokens.colors.neutral900
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        leadingIcon(tint)
        Spacer(Modifier.width(tokens.spacing.s150))
        Text(text = text, color = tint, fontSize = tokens.type.sm, maxLines = 1)
    }
}
