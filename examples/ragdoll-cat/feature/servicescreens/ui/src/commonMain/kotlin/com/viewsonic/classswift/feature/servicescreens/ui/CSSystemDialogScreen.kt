package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode

private val DialogMask = Color(0x99FFFFFF) // white_a60
private val DialogDivider = Color(0xFFC3C7C7)

/** CMP port of `CSSystemDialogWindow` (service path): 372×196 confirm dialog over a 60%-white mask. */
@Composable
fun CSSystemDialogScreen(
    title: String = "End Lesson",
    message: String = "Are you sure you want to end this lesson? Students will no longer be able to respond.",
    negative: String = "Cancel",
    positive: String = "End Lesson",
    onNegative: () -> Unit = {},
    onPositive: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize().background(DialogMask), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(372.dp).height(196.dp).clip(RoundedCornerShape(5.33.dp)).background(WindowBgF5F5F5).designNode("system_dialog"),
        ) {
            Text(
                title, color = Neutral900, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().height(44.dp).padding(4.dp).designNode("dialog_title"),
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(DialogDivider))
            Box(Modifier.fillMaxWidth().height(104.dp).background(Color.White).padding(horizontal = 24.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                Text(message, color = Neutral900, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.designNode("dialog_message"))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(DialogDivider))
            Row(Modifier.fillMaxWidth().weight(1f)) {
                Box(Modifier.weight(1f).fillMaxSize().clickable(onClick = onNegative).designNode("dialog_negative"), contentAlignment = Alignment.Center) {
                    Text(negative, color = Neutral900, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.width(1.dp).fillMaxSize().background(DialogDivider))
                Box(Modifier.weight(1f).fillMaxSize().clickable(onClick = onPositive).designNode("dialog_positive"), contentAlignment = Alignment.Center) {
                    Text(positive, color = BrandBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
