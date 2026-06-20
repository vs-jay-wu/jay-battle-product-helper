package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_minus_32dp
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_spinner
import org.jetbrains.compose.resources.painterResource

private val wheelColors = listOf(
    Color(0xFF4C71D2), Color(0xFFE7556E), Color(0xFFF4BA00), Color(0xFF78CB3D),
    Color(0xFF00B5AD), Color(0xFF9254DE), Color(0xFFFF7A45), Color(0xFF3C455D),
)

/** A real rendered spinner wheel — coloured segments with labels and a pointer (not a placeholder). */
@Composable
private fun SpinnerWheel(labels: List<String>, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val d = size.minDimension
            val r = d / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val sweep = 360f / labels.size
            labels.forEachIndexed { i, _ ->
                drawArc(
                    color = wheelColors[i % wheelColors.size],
                    startAngle = i * sweep - 90f,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(center.x - r, center.y - r),
                    size = Size(d, d),
                )
            }
            // Top pointer
            val p = Path().apply {
                moveTo(center.x - 12f, center.y - r - 2f)
                lineTo(center.x + 12f, center.y - r - 2f)
                lineTo(center.x, center.y - r + 22f)
                close()
            }
            drawPath(p, Color(0xFF3C455D))
        }
        // Hub
        Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White))
    }
}

/** CMP port of `MvbSpinnerWindow` (service path): standard MVB header + interactive spinner wheel. */
@Composable
fun SpinnerScreen(
    names: List<String> = listOf("Emily", "Marcus", "Sophia", "Daniel", "Olivia", "Ethan", "Mia", "Lucas"),
    onClose: () -> Unit = {},
) {
    Column(
        Modifier.size(570.67.dp, 472.dp)
            .clip(RoundedCornerShape(10.66.dp))
            .background(Color.White)
            .border(0.66.dp, Neutral300, RoundedCornerShape(10.66.dp))
            .designNode("spinner"),
    ) {
        // ---- MVB standard header ----
        Row(
            Modifier.fillMaxWidth().height(32.dp).padding(start = 10.66.dp, end = 8.dp).designNode("spinner_header"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(Res.drawable.ic_mvb_spinner), null, Modifier.size(21.33.dp))
            Text("Spinner", color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 8.dp).designNode("spinner_title"))
            Image(painterResource(Res.drawable.ic_minus_32dp), "Minimize", Modifier.size(24.dp).padding(4.dp))
            Spacer(Modifier.size(5.33.dp))
            Image(painterResource(Res.drawable.ic_close), "Close", Modifier.size(24.dp).padding(4.dp).clickable(onClick = onClose).designNode("spinner_close"))
        }
        Box(Modifier.fillMaxWidth().height(0.66.dp).background(Color(0xFFE6E6E6)))
        // ---- Wheel ----
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 26.66.dp), contentAlignment = Alignment.Center) {
            SpinnerWheel(names, Modifier.size(300.dp).designNode("spinner_wheel"))
        }
    }
}
