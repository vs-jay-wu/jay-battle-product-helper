package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_premium
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

/** Item states from `CSSubordinateMenuItem.ItemState` (drives tint, premium icon, soon badge). */
enum class MenuItemState { NORMAL, SELECTED, NEED_TO_UPGRADE, COMING_SOON, DISABLED }

/** One menu entry. */
data class MenuItem(
    val icon: DrawableResource,
    val label: String,
    val nodeId: String,
    val state: MenuItemState = MenuItemState.NORMAL,
    val onClick: () -> Unit = {},
)

/**
 * `item_subordinate_menu.xml` row faithful to `CSSubordinateMenuItem`: 24dp icon + 4.8dp gap +
 * 12sp title, row 24dp. State drives icon tint / title color / premium icon / "SOON" badge.
 */
@Composable
private fun MenuItemRow(item: MenuItem) {
    val disabled = item.state == MenuItemState.NEED_TO_UPGRADE ||
        item.state == MenuItemState.COMING_SOON ||
        item.state == MenuItemState.DISABLED
    val tint = when {
        item.state == MenuItemState.SELECTED -> BrandBlue
        disabled -> StrokeC3C7C7
        else -> Color.Black
    }
    val titleColor = when {
        item.state == MenuItemState.SELECTED -> BrandBlue
        disabled -> StrokeC3C7C7
        else -> Dark2E3133
    }
    Row(
        Modifier.height(24.dp).clickable(enabled = item.state != MenuItemState.DISABLED, onClick = item.onClick).designNode(item.nodeId),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(item.icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(24.dp),
        )
        Text(item.label, color = titleColor, fontSize = 12.sp, modifier = Modifier.padding(start = 4.8.dp))
        if (item.state == MenuItemState.NEED_TO_UPGRADE) {
            Image(painterResource(Res.drawable.ic_premium), null, Modifier.padding(start = 2.dp).size(16.dp).padding(1.dp))
        }
        if (item.state == MenuItemState.COMING_SOON) {
            Box(
                Modifier.padding(start = 2.52.dp).clip(RoundedCornerShape(2.52.dp)).background(SoonYellow).padding(horizontal = 1.26.dp, vertical = 1.dp),
            ) { Text("SOON", color = Color.White, fontSize = 7.53.sp, fontWeight = FontWeight.Bold) }
        }
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

/** CMP port of `ToolsMenuWindow`. Spinner row hidden when [spinnerVisible] is false. */
@Composable
fun ToolsMenu(
    onBuzzer: () -> Unit = {},
    onRandomDraw: () -> Unit = {},
    onSpinner: () -> Unit = {},
    onTimer: () -> Unit = {},
    spinnerVisible: Boolean = true,
) = SubordinateMenu("tools_menu") {
    MenuColumn(
        buildList {
            add(MenuItem(Res.drawable.ic_toolbar_irs_buzzer, "Buzzer", "menu_buzzer", onClick = onBuzzer))
            add(MenuItem(Res.drawable.ic_toolbar_irs_random_drawer, "Random Draw", "menu_random_draw", onClick = onRandomDraw))
            if (spinnerVisible) add(MenuItem(Res.drawable.ic_toolbar_irs_spinner, "Spinner", "menu_spinner", onClick = onSpinner))
            add(MenuItem(Res.drawable.ic_toolbar_irs_timer, "Timer", "menu_timer", onClick = onTimer))
        },
    )
}

/** CMP port of `ClassManagementMenuWindow`. Leaderboard shows the premium badge for non-premium users. */
@Composable
fun ClassManagementMenu(
    onStudentList: () -> Unit = {},
    onLeaderboard: () -> Unit = {},
    isPremiumUser: Boolean = true,
) = SubordinateMenu("class_management_menu") {
    MenuColumn(
        listOf(
            MenuItem(Res.drawable.ic_toolbar_irs_student_management, "Student List", "menu_student_management", onClick = onStudentList),
            MenuItem(
                Res.drawable.ic_toolbar_irs_leaderboard, "Leaderboard", "menu_leaderboard",
                state = if (isPremiumUser) MenuItemState.NORMAL else MenuItemState.NEED_TO_UPGRADE,
                onClick = onLeaderboard,
            ),
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
