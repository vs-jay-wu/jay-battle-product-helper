package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_quit_app
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_quiz
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_class
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_class_list
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_end_lesson
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_instant_push
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_leave_class
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_preset
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_push
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_settings
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_start_lesson
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_tools
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val toolbarIcons = listOf(
    Res.drawable.ic_toolbar_class_list to "class_list",
    Res.drawable.ic_toolbar_class to "class",
    Res.drawable.ic_quiz to "quiz",
    Res.drawable.ic_toolbar_preset to "preset",
    Res.drawable.ic_toolbar_push to "push",
    Res.drawable.ic_toolbar_tools to "tools",
    Res.drawable.ic_toolbar_instant_push to "instant_push",
    Res.drawable.ic_toolbar_settings to "settings",
    Res.drawable.ic_quit_app to "quit_app",
)

/** An action button (Leave / Start / End lesson) — icon + label, outline or filled. */
@Composable
private fun ActionButton(icon: DrawableResource, label: String, fill: Color, content: Color, border: Color, nodeId: String) {
    Row(
        Modifier.height(28.8.dp).clip(RoundedCornerShape(4.dp)).background(fill).border(1.dp, border, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp).designNode(nodeId),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(icon), null, Modifier.size(18.dp), colorFilter = ColorFilter.tint(content))
        Text(label, color = content, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp))
    }
}

/** CMP port of `ToolbarWindow` (service path), expanded state: feature icon row + lesson actions. */
@Composable
fun ToolbarScreen() {
    Row(
        Modifier.height(42.67.dp)
            .clip(RoundedCornerShape(topEnd = 4.73.dp, bottomEnd = 4.73.dp))
            .background(WindowBgF5F5F5)
            .border(1.dp, StrokeC3C7C7, RoundedCornerShape(topEnd = 4.73.dp, bottomEnd = 4.73.dp))
            .designNode("toolbar"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Feature icon buttons
        Row(Modifier.padding(horizontal = 1.19.dp), verticalAlignment = Alignment.CenterVertically) {
            toolbarIcons.forEach { (icon, id) ->
                Image(
                    painterResource(icon), id,
                    Modifier.padding(horizontal = 3.55.dp).size(28.8.dp).designNode("toolbar_$id"),
                    colorFilter = ColorFilter.tint(Dark2E3133),
                )
            }
        }
        Box(Modifier.padding(horizontal = 4.74.dp).width(1.dp).fillMaxHeight().background(StrokeC3C7C7))
        Row(Modifier.padding(end = 4.74.dp), verticalAlignment = Alignment.CenterVertically) {
            ActionButton(Res.drawable.ic_toolbar_leave_class, "Leave Class", Color.White, BrandBlue, BrandBlue, "toolbar_leave")
            Box(Modifier.width(9.48.dp))
            ActionButton(Res.drawable.ic_toolbar_start_lesson, "Start Lesson", BrandBlue, Color.White, BrandBlue, "toolbar_start")
            Box(Modifier.width(9.48.dp))
            ActionButton(Res.drawable.ic_toolbar_end_lesson, "End Lesson", Color.White, RedF02B2B, RedF02B2B, "toolbar_end")
        }
    }
}
