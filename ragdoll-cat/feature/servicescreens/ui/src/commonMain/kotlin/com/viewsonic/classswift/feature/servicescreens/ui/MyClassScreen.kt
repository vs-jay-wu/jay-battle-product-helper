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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_hint
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_my_class_delete
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_nav_class_swift
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_previous_arrow
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toolbar_bring_to_front_64dp
import org.jetbrains.compose.resources.painterResource

@Composable
private fun ClassListRow(name: String, selected: Boolean, onClick: () -> Unit, nodeId: String) {
    val bg = if (selected) BrandBlue else Color.White
    val nameColor = if (selected) Color.White else Dark2E3133
    Row(
        Modifier.fillMaxWidth().height(45.dp).background(bg).clickable(onClick = onClick)
            .padding(start = 14.9.dp, end = 12.dp).designNode(nodeId),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, color = nameColor, fontSize = 14.3.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
        Image(
            painterResource(Res.drawable.ic_my_class_delete), "Delete",
            Modifier.size(28.dp),
            colorFilter = if (selected) ColorFilter.tint(Color.White) else null,
        )
    }
}

/** A labelled info row in the right panel: bold label + value. */
@Composable
private fun InfoRow(label: String, value: String, hint: Boolean = false, valueBold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Dark2E3133, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (hint) Image(painterResource(Res.drawable.ic_hint), null, Modifier.padding(start = 4.dp).size(20.dp))
        Spacer(Modifier.weight(1f))
        Text(
            value,
            color = if (valueBold) Dark2E3133 else Neutral500,
            fontSize = 16.sp,
            fontWeight = if (valueBold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

/** CMP port of `MyClassWindow` (service path): class list (left) + class detail/actions (right). */
@Composable
fun MyClassScreen(
    orgName: String = "Riverside Elementary",
    plan: String = "Basic-35",
    classes: List<String> = listOf("Grade 5 — Mathematics", "Grade 5 — Science", "Homeroom 5A", "After-school Robotics"),
    studentCount: String = "35",
    onClose: () -> Unit = {},
) {
    var selected by remember { mutableStateOf(0) }
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
                    Image(painterResource(Res.drawable.ic_previous_arrow), "Back", Modifier.size(28.dp).designNode("mc_back"))
                    Text("My Class", color = Dark2E3133, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp).designNode("mc_title"))
                    Spacer(Modifier.weight(1f))
                    Text("ClassSwift Hub", color = Dark2E3133, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.designNode("mc_hub"))
                    Image(painterResource(Res.drawable.ic_nav_class_swift), null, Modifier.padding(start = 4.dp).size(16.dp))
                }
                Column(
                    Modifier.padding(top = 16.dp).weight(1f).fillMaxWidth()
                        .clip(RoundedCornerShape(2.4.dp))
                        .background(Color.White)
                        .border(0.6.dp, StrokeC3C7C7, RoundedCornerShape(2.4.dp))
                        .designNode("mc_class_list"),
                ) {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                        itemsIndexed(classes) { i, c ->
                            ClassListRow(c, i == selected, { selected = i }, "mc_class_$i")
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(StrokeC3C7C7))
                    Box(Modifier.fillMaxWidth().height(50.dp).clickable {}.designNode("mc_add_class"), contentAlignment = Alignment.Center) {
                        Text("+New Class", color = Green78CB3D, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            // ---- Right panel: detail + actions ----
            Column(Modifier.weight(1f).fillMaxSize().padding(horizontal = 24.dp).padding(top = 36.dp, bottom = 24.dp)) {
                InfoRow("Organization", orgName)
                Box(Modifier.padding(top = 16.dp)) { InfoRow("Plan", plan, hint = true) }
                Box(Modifier.fillMaxWidth().padding(top = 16.dp).height(1.dp).background(Color(0xFFD3D3D3)))
                Text("Class Name", color = Dark2E3133, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                Box(
                    Modifier.fillMaxWidth().padding(top = 8.dp).height(36.dp)
                        .clip(RoundedCornerShape(5.33.dp))
                        .border(1.dp, StrokeC3C7C7, RoundedCornerShape(5.33.dp))
                        .padding(horizontal = 9.6.dp)
                        .designNode("mc_class_name"),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(classes.getOrElse(selected) { "-" }, color = Color.Black, fontSize = 16.sp, maxLines = 1)
                }
                Box(Modifier.padding(top = 16.dp)) { InfoRow("Number of Students", studentCount, hint = true, valueBold = true) }
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth().height(36.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CSButton("Edit", backgroundColor = Color.White, textColor = BrandBlue, borderColor = BrandBlue, nodeId = "mc_edit", modifier = Modifier.weight(1f).height(36.dp))
                    CSButton("Enter Class", backgroundColor = BrandBlue, textColor = Color.White, nodeId = "mc_enter", modifier = Modifier.weight(1f).height(36.dp))
                }
            }
        }
        // ---- Window controls ----
        Row(Modifier.align(Alignment.TopEnd).padding(top = 10.6.dp, end = 10.6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(painterResource(Res.drawable.ic_toolbar_bring_to_front_64dp), "Bring to front", Modifier.size(21.3.dp), colorFilter = ColorFilter.tint(CloseGray))
            Image(painterResource(Res.drawable.ic_close), "Close", Modifier.size(21.3.dp).clickable(onClick = onClose).designNode("mc_close"), colorFilter = ColorFilter.tint(CloseGray))
        }
    }
}
