package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_chevron_down
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_hint
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_irs_setting
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_translate
import org.jetbrains.compose.resources.painterResource

/** A white settings sub-card (`bg_neutral0_no_border_radius400`). */
@Composable
private fun SettingCard(nodeId: String, height: androidx.compose.ui.unit.Dp, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(top = 16.dp).height(height)
            .clip(RoundedCornerShape(5.33.dp)).background(Color.White).designNode(nodeId),
    ) { content() }
}

/** Compose stand-in for the SwitchCompat translation toggle. */
@Composable
private fun CSSwitch(checked: Boolean, onToggle: () -> Unit, nodeId: String) {
    val track = if (checked) Sky100 else TrackGray
    Box(
        Modifier.width(40.dp).height(20.dp).clip(RoundedCornerShape(30.dp)).background(track).clickable(onClick = onToggle).designNode(nodeId),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.padding(horizontal = 2.dp).size(15.dp).clip(CircleShape).background(if (checked) BrandBlue else StrokeC3C7C7))
    }
}

/** CMP port of `SettingsWindow` (service path): language picker, translation toggle, tutorial.
 * (The debug-tool section is debug-build only → out of scope.) */
@Composable
fun SettingsScreen(
    language: String = "English",
    translationOn: Boolean = true,
    onClose: () -> Unit = {},
) {
    var translate by remember { mutableStateOf(translationOn) }
    Box(
        Modifier.width(400.dp)
            .clip(RoundedCornerShape(10.66.dp))
            .background(WindowBgF5F5F5)
            .border(1.33.dp, BorderC2C2C2, RoundedCornerShape(10.66.dp))
            .designNode("settings"),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 26.66.dp).padding(top = 40.dp, bottom = 16.dp)) {
            // Title
            Row(Modifier.height(32.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(Res.drawable.ic_toolbar_irs_setting), null, Modifier.padding(horizontal = 2.66.dp).size(24.dp))
                Text("Settings", color = Dark2E3133, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.designNode("settings_title"))
            }
            // Language
            SettingCard("settings_language", 95.dp) {
                Column(Modifier.fillMaxWidth().padding(10.66.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Language", color = Dark2E3133, fontSize = 13.33.sp, fontWeight = FontWeight.Bold)
                        Image(painterResource(Res.drawable.ic_hint), null, Modifier.padding(start = 2.66.dp).size(18.dp))
                    }
                    Row(
                        Modifier.padding(top = 16.dp).fillMaxWidth().height(37.33.dp)
                            .clip(RoundedCornerShape(5.33.dp)).border(1.dp, StrokeC3C7C7, RoundedCornerShape(5.33.dp))
                            .padding(horizontal = 10.dp).designNode("settings_language_spinner"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(language, color = Dark2E3133, fontSize = 13.33.sp, modifier = Modifier.weight(1f))
                        Image(painterResource(Res.drawable.ic_chevron_down), null, Modifier.size(12.dp))
                    }
                }
            }
            // Translation tool
            SettingCard("settings_translation", 78.dp) {
                Column(Modifier.fillMaxWidth().padding(10.66.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Translation tool", color = Dark2E3133, fontSize = 13.33.sp, fontWeight = FontWeight.Bold)
                        Box(Modifier.padding(start = 5.dp).clip(RoundedCornerShape(2.52.dp)).background(Cyan3AC9CC).padding(horizontal = 2.dp, vertical = 1.dp)) {
                            Text("UPGRADE", color = Color.White, fontSize = 7.53.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(Modifier.padding(top = 16.dp).fillMaxWidth().height(21.33.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(Res.drawable.ic_translate), null, Modifier.size(21.33.dp))
                        Text("Help students translate the questions", color = Dark2E3133, fontSize = 13.33.sp, modifier = Modifier.weight(1f).padding(horizontal = 10.66.dp))
                        CSSwitch(translate, { translate = !translate }, "settings_translate_switch")
                    }
                }
            }
            // In-app tutorial
            SettingCard("settings_tutorial", 45.33.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.66.dp).height(45.33.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Watch the Tutorial", color = Dark2E3133, fontSize = 13.33.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.height(24.dp).clip(RoundedCornerShape(5.33.dp)).background(BrandBlue).padding(horizontal = 10.dp).designNode("settings_watch"), contentAlignment = Alignment.Center) {
                        Text("Watch", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
        Image(
            painterResource(Res.drawable.ic_close), "Close",
            Modifier.align(Alignment.TopEnd).padding(top = 10.66.dp, end = 10.66.dp).size(21.33.dp).clickable(onClick = onClose).designNode("settings_close"),
        )
    }
}
