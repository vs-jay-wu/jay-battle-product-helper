package com.viewsonic.classswift.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Platform-neutral design tokens for the Designer-to-Code preview.
 *
 * Values mirror the ClassSwift VSDS tokens (see .claude/rules/figma-design-tokens.md and
 * res/values: colors neutral_*, mvb_spacing_*, quiz_mvb_text_*). Kept in Kotlin so the same
 * tokens drive both the desktop canvas and the shipped Android app — no Android `R` here.
 */
data class AppColors(
    val neutral0: Color = Color(0xFFFFFFFF),
    val neutral100: Color = Color(0xFFF6F6F6),
    val neutral300: Color = Color(0xFFE5E5E5),
    val neutral500: Color = Color(0xFFB2B2B2),
    val neutral650: Color = Color(0xFF797979),
    val neutral800: Color = Color(0xFF4D4D4D),
    val neutral900: Color = Color(0xFF333333),
    val primary: Color = Color(0xFF4848F0),
    val violet50: Color = Color(0xFFEDEDFD),
    val error: Color = Color(0xFFDB0025),
)

/** mvb_spacing_* scale (dp). */
data class AppSpacing(
    val s100: Dp = 2.66.dp,
    val s150: Dp = 4.dp,
    val s200: Dp = 5.33.dp,
    val s250: Dp = 6.66.dp,
    val s300: Dp = 8.dp,
    val s400: Dp = 10.66.dp,
    val s500: Dp = 13.33.dp,
    val s600: Dp = 16.dp,
    val s800: Dp = 21.33.dp,
    val s900: Dp = 24.dp,
)

/** radius tokens (dp). */
data class AppRadius(
    val r400: Dp = 5.33.dp,
    val r600: Dp = 8.dp,
    val r800: Dp = 10.66.dp,
)

/** quiz_mvb_text_* scale (sp). */
data class AppType(
    val xs: TextUnit = 8.sp,
    val sm: TextUnit = 9.33.sp,
    val md: TextUnit = 10.67.sp,
    val lg: TextUnit = 12.sp,
    val xl: TextUnit = 13.33.sp,
)

data class AppTokens(
    val colors: AppColors = AppColors(),
    val spacing: AppSpacing = AppSpacing(),
    val radius: AppRadius = AppRadius(),
    val type: AppType = AppType(),
)

val LocalAppTokens = staticCompositionLocalOf { AppTokens() }

/**
 * Theme entry point. Provides [AppTokens] and a MaterialTheme base.
 * Access tokens inside composables via `AppTheme.tokens`.
 */
@Composable
fun AppTheme(
    tokens: AppTokens = AppTokens(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppTokens provides tokens) {
        MaterialTheme(content = content)
    }
}

object AppTheme {
    val tokens: AppTokens
        @Composable get() = LocalAppTokens.current
}
