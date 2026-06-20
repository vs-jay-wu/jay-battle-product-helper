package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import org.jetbrains.compose.resources.painterResource

/**
 * Shared tool-window card chrome: 348×336 white rounded card (`bg_neural0_radius800_…`) with a
 * top-right close button. Used by the Buzzer / Random Draw classroom tools.
 */
@Composable
fun ToolCard(
    nodeId: String,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 348.dp,
    height: androidx.compose.ui.unit.Dp = 336.dp,
    onClose: () -> Unit = {},
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    Box(
        modifier.size(width, height)
            .clip(RoundedCornerShape(10.66.dp))
            .background(Color.White)
            .border(1.33.dp, BorderC2C2C2, RoundedCornerShape(10.66.dp))
            .designNode(nodeId),
    ) {
        content()
        Image(
            painter = painterResource(Res.drawable.ic_close),
            contentDescription = "Close",
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(top = 10.66.dp, end = 10.66.dp)
                .size(24.dp)
                .clickable(onClick = onClose)
                .designNode("${nodeId}_close"),
        )
    }
}
