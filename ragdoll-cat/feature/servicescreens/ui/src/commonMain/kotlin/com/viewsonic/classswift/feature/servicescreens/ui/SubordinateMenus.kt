package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_audio
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_buzzer
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_leaderboard
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_multiple_selection
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_poll
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_quit_classswift
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_quiz_generator
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_random_drawer
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_setting
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_short_answer
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_sign_out
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_spinner
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_student_management
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_timer
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_true_false
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** One menu entry — `CSSubordinateMenuItem` in its NORMAL state (icon tinted black, 12sp title). */
data class MenuItem(val icon: DrawableResource, val label: String, val nodeId: String)

/** `item_subordinate_menu.xml`: 24dp icon + 4.8dp gap + 12sp title, row height 24dp. */
@Composable
private fun MenuItemRow(item: MenuItem) {
    Row(
        Modifier.height(24.dp).designNode(item.nodeId),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(item.icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.Black),
            modifier = Modifier.size(24.dp),
        )
        Text(
            item.label,
            color = Dark2E3133,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.8.dp),
        )
    }
}

/** Vertical list of menu rows spaced 4.66dp — used directly or as a column inside the quiz menu. */
@Composable
private fun MenuColumn(items: List<MenuItem>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.66.dp)) {
        items.forEach { MenuItemRow(it) }
    }
}

/** `bg_window_subordinate_menu` surface (F5F5F5, 0.33dp BDBDBD border, radius 4.66dp). */
@Composable
private fun SubordinateMenu(nodeId: String, content: @Composable () -> Unit) {
    Row(
        Modifier.designNode(nodeId)
            .clip(RoundedCornerShape(4.66.dp))
            .background(MenuBgF5F5F5)
            .border(0.33.dp, MenuBorderBDBDBD, RoundedCornerShape(4.66.dp))
            .padding(horizontal = 12.dp, vertical = 9.33.dp),
    ) { content() }
}

/** CMP port of `SettingMenuWindow`. */
@Composable
fun SettingMenu() = SubordinateMenu("setting_menu") {
    MenuColumn(
        listOf(
            MenuItem(Res.drawable.ic_toolbar_irs_setting, "Settings", "menu_settings"),
            MenuItem(Res.drawable.ic_toolbar_irs_sign_out, "Sign Out", "menu_sign_out"),
            MenuItem(Res.drawable.ic_toolbar_irs_quit_classswift, "Quit ClassSwift", "menu_quit"),
        ),
    )
}

/** CMP port of `ToolsMenuWindow`. */
@Composable
fun ToolsMenu() = SubordinateMenu("tools_menu") {
    MenuColumn(
        listOf(
            MenuItem(Res.drawable.ic_toolbar_irs_buzzer, "Buzzer", "menu_buzzer"),
            MenuItem(Res.drawable.ic_toolbar_irs_random_drawer, "Random Draw", "menu_random_draw"),
            MenuItem(Res.drawable.ic_toolbar_irs_spinner, "Spinner", "menu_spinner"),
            MenuItem(Res.drawable.ic_toolbar_irs_timer, "Timer", "menu_timer"),
        ),
    )
}

/** CMP port of `ClassManagementMenuWindow`. */
@Composable
fun ClassManagementMenu() = SubordinateMenu("class_management_menu") {
    MenuColumn(
        listOf(
            MenuItem(Res.drawable.ic_toolbar_irs_student_management, "Student List", "menu_student_management"),
            MenuItem(Res.drawable.ic_toolbar_irs_leaderboard, "Leaderboard", "menu_leaderboard"),
        ),
    )
}

/** CMP port of `QuizMenuWindow` — two columns of quiz types, 6.67dp apart. */
@Composable
fun QuizMenu() = SubordinateMenu("quiz_menu") {
    MenuColumn(
        listOf(
            MenuItem(Res.drawable.ic_toolbar_irs_true_false, "True/False", "menu_true_false"),
            MenuItem(Res.drawable.ic_toolbar_irs_multiple_selection, "Multiple Selection", "menu_multiple_selection"),
            MenuItem(Res.drawable.ic_toolbar_irs_poll, "Poll", "menu_poll"),
        ),
    )
    MenuColumn(
        listOf(
            MenuItem(Res.drawable.ic_toolbar_irs_short_answer, "Short Answer", "menu_short_answer"),
            MenuItem(Res.drawable.ic_toolbar_irs_audio, "Audio", "menu_audio"),
            MenuItem(Res.drawable.ic_toolbar_irs_quiz_generator, "Quiz Generator", "menu_quiz_generator"),
        ),
        modifier = Modifier.padding(start = 6.67.dp),
    )
}
