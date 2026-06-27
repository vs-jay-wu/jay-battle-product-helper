package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode

/**
 * Compose stand-in for `ClassSwiftLoadingButton` in its idle state — rounded rect with a fill,
 * optional border, and centered label. (Loading spinner state is out of scope for the previews.)
 */
@Composable
fun CSButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    textSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    radius: androidx.compose.ui.unit.Dp = 5.33.dp,
    nodeId: String? = null,
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(radius)
    var m = modifier.clip(shape).background(backgroundColor)
    if (borderColor != null) m = m.border(1.dp, borderColor, shape)
    if (nodeId != null) m = m.designNode(nodeId)
    Box(m.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = textColor, fontSize = textSize, fontWeight = FontWeight.Medium)
    }
}
