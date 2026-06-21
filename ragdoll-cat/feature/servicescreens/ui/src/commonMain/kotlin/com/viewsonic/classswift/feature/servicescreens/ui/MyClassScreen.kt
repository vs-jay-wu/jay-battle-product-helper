package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_classlink
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_google_classroom
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_hint
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_my_class_delete
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_nav_class_swift
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_previous_arrow
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_bring_to_front_64dp
import org.jetbrains.compose.resources.painterResource

/** Which body to show in the class-list card. */
enum class MyClassPhase { LOADING, LIST, REFRESH }

/** A class in the list. [roster] = roster-sourced (shows a source icon); [ongoing] = lesson live. */
data class MyClassItem(
    val id: String,
    val name: String,
    val roster: Boolean = false,
    val rosterIsClassLink: Boolean = false,
    val ongoing: Boolean = false,
)

@Composable
private fun ClassListRow(item: MyClassItem, selected: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    val bg = if (selected) BrandBlue else Color.White
    val nameColor = if (selected) Color.White else Dark2E3133
    Row(
        Modifier.fillMaxWidth().height(45.dp).background(bg).clickable(onClick = onClick)
            .padding(start = 14.9.dp, end = 12.dp).designNode("mc_class_${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.roster) {
            Image(
                painterResource(if (item.rosterIsClassLink) Res.drawable.ic_classlink else Res.drawable.ic_google_classroom),
                null, Modifier.padding(end = 4.dp).size(16.7.dp),
            )
        }
        Text(item.name, color = nameColor, fontSize = 14.3.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (item.ongoing) {
            Box(
                Modifier.clip(RoundedCornerShape(2.4.dp)).background(if (selected) Color.White else WindowBgF5F5F5).padding(horizontal = 3.6.dp, vertical = 1.dp),
            ) { Text("Ongoing", color = BrandBlue, fontSize = 8.39.sp, fontWeight = FontWeight.Bold) }
        } else {
            Image(
                painterResource(Res.drawable.ic_my_class_delete), "Delete",
                Modifier.size(28.dp).clickable(onClick = onDelete),
                colorFilter = if (selected) ColorFilter.tint(Color.White) else null,
            )
        }
    }
}

/** A labelled info row: bold label (+ optional hint icon) + value, value right-aligned. */
@Composable
private fun InfoRow(label: String, value: String, hint: Boolean = false, valueBold: Boolean = false, onHint: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Dark2E3133, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (hint) Image(painterResource(Res.drawable.ic_hint), null, Modifier.padding(start = 4.dp).size(20.dp).clickable(onClick = onHint))
        Spacer(Modifier.weight(1f))
        Text(value, color = if (valueBold) Dark2E3133 else Neutral500, fontSize = 16.sp, fontWeight = if (valueBold) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
    }
}

/**
 * CMP port of `MyClassWindow` (service path) — STEP 1: main view + primary path (select / delete /
 * Enter Class / Back / Hub / refresh-loading). Edit + New Class are rendered disabled (wired in step 2).
 */
private val sampleMyClasses = listOf(
    MyClassItem("c1", "Grade 5 — Mathematics"),
    MyClassItem("c2", "Grade 5 — Science", ongoing = true),
    MyClassItem("c3", "Homeroom 5A"),
    MyClassItem("c4", "After-school Robotics"),
)

@Composable
fun MyClassScreen(
    orgName: String = "Riverside Elementary",
    plan: String = "Basic-35",
    classes: List<MyClassItem> = sampleMyClasses,
    selectedId: String? = classes.firstOrNull()?.id,
    studentCount: String = "35",
    phase: MyClassPhase = MyClassPhase.LIST,
    enterEnabled: Boolean = true,
    newClassEnabled: Boolean = true,
    onSelect: (String) -> Unit = {},
    onDelete: (String) -> Unit = {},
    onEnter: () -> Unit = {},
    onNewClass: () -> Unit = {},
    onBack: () -> Unit = {},
    onHub: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onPlanHint: () -> Unit = {},
    onNumberHint: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    Box(
        Modifier.width(680.dp).height(393.dp)
            .clip(RoundedCornerShape(10.66.dp))
            .background(WindowBgF5F5F5)
            .border(0.96.dp, StrokeC3C7C7, RoundedCornerShape(10.66.dp))
            .designNode("my_class"),
    ) {
        Row(Modifier.fillMaxSize()) {
            // ---- Left panel: class list ----
            Column(Modifier.weight(1f).fillMaxSize().padding(horizontal = 24.dp).padding(top = 36.dp, bottom = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(Res.drawable.ic_previous_arrow), "Back", Modifier.size(28.dp).clickable(onClick = onBack).designNode("mc_back"))
                    Text("My Class", color = Dark2E3133, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp).designNode("mc_title"))
                    Spacer(Modifier.weight(1f))
                    Text("ClassSwift Hub", color = Dark2E3133, fontSize = 16.sp, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline, modifier = Modifier.clickable(onClick = onHub).designNode("mc_hub"))
                    Image(painterResource(Res.drawable.ic_nav_class_swift), null, Modifier.padding(start = 4.dp).size(16.dp))
                }
                Column(
                    Modifier.padding(top = 16.dp).weight(1f).fillMaxWidth()
                        .clip(RoundedCornerShape(2.4.dp)).background(Color.White).border(0.6.dp, StrokeC3C7C7, RoundedCornerShape(2.4.dp))
                        .designNode("mc_class_list"),
                ) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        when (phase) {
                            MyClassPhase.LOADING -> CircularProgressIndicator(Modifier.size(28.dp), color = BrandBlue, strokeWidth = 2.dp)
                            MyClassPhase.REFRESH -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Failed to find class list", color = Dark2E3133, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("Try to refresh this page again", color = Dark2E3133, fontSize = 12.sp, modifier = Modifier.padding(top = 5.33.dp))
                                Box(Modifier.padding(top = 16.dp).width(120.dp).height(32.dp).clip(RoundedCornerShape(5.33.dp)).background(BrandBlue).clickable(onClick = onRefresh).designNode("mc_refresh"), contentAlignment = Alignment.Center) {
                                    Text("Refresh Page", color = Color.White, fontSize = 13.33.sp)
                                }
                            }
                            MyClassPhase.LIST -> LazyColumn(Modifier.fillMaxSize()) {
                                items(classes, key = { it.id }) { c ->
                                    ClassListRow(c, c.id == selectedId, { onSelect(c.id) }, { onDelete(c.id) })
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(StrokeC3C7C7))
                                }
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(StrokeC3C7C7))
                    // +New Class (window_my_class_new_class: green #78CB3D enabled / C3C7C7 disabled)
                    Box(
                        Modifier.fillMaxWidth().height(50.dp)
                            .then(if (newClassEnabled) Modifier.clickable(onClick = onNewClass) else Modifier)
                            .designNode("mc_add_class"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+New Class", color = if (newClassEnabled) Color(0xFF78CB3D) else StrokeC3C7C7, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            // ---- Right panel: detail + actions ----
            Column(Modifier.weight(1f).fillMaxSize().padding(horizontal = 24.dp).padding(top = 36.dp, bottom = 24.dp)) {
                InfoRow("Organization", orgName)
                Box(Modifier.padding(top = 16.dp)) { InfoRow("Plan", plan, hint = true, onHint = onPlanHint) }
                Box(Modifier.fillMaxWidth().padding(top = 16.dp).height(1.dp).background(Color(0xFFD3D3D3)))
                Text("Class Name", color = Dark2E3133, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                Box(
                    Modifier.fillMaxWidth().padding(top = 8.dp).height(36.dp)
                        .clip(RoundedCornerShape(5.33.dp)).border(1.dp, StrokeC3C7C7, RoundedCornerShape(5.33.dp)).padding(horizontal = 9.6.dp)
                        .designNode("mc_class_name"),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(classes.firstOrNull { it.id == selectedId }?.name ?: "-", color = Color.Black, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.padding(top = 16.dp)) { InfoRow("Number of Students", studentCount, hint = true, valueBold = true, onHint = onNumberHint) }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Edit — disabled in step 1 (wired in step 2)
                    CSButton("Edit", backgroundColor = Color.White, textColor = StrokeC3C7C7, borderColor = StrokeC3C7C7, nodeId = "mc_edit", modifier = Modifier.weight(1f).height(36.dp))
                    CSButton(
                        "Enter Class",
                        backgroundColor = if (enterEnabled) BrandBlue else StrokeC3C7C7,
                        textColor = Color.White,
                        nodeId = "mc_enter",
                        modifier = Modifier.weight(1f).height(36.dp),
                        onClick = { if (enterEnabled) onEnter() },
                    )
                }
            }
        }
        // ---- Window controls (close + bring-to-front, tint neutral_900) ----
        Row(Modifier.align(Alignment.TopEnd).padding(top = 10.6.dp, end = 10.6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(painterResource(Res.drawable.ic_toolbar_bring_to_front_64dp), "Bring to front", Modifier.size(21.3.dp), colorFilter = ColorFilter.tint(Neutral900))
            Image(painterResource(Res.drawable.ic_close), "Close", Modifier.size(21.3.dp).clickable(onClick = onClose).designNode("mc_close"), colorFilter = ColorFilter.tint(Neutral900))
        }
    }
}
