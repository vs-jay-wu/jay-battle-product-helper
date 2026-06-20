package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_broadcast
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_calendar_clock
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_chevron_down
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_org_building
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_pen_straight_line
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_plan_badge_check
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_plus
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_select_class_header
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_trash_can
import org.jetbrains.compose.resources.painterResource

/** A class the teacher can enter; `ongoing` shows the red live-lesson pill. */
data class ClassItem(val name: String, val ongoing: Boolean = false)

val sampleClasses = listOf(
    ClassItem("Grade 5 — Mathematics"),
    ClassItem("Grade 5 — Science", ongoing = true),
    ClassItem("Homeroom 5A"),
    ClassItem("After-school Robotics"),
)

@Composable
private fun OrgDropdown(orgName: String, modifier: Modifier = Modifier) {
    Row(
        modifier.height(26.67.dp)
            .clip(RoundedCornerShape(5.33.dp))
            .border(1.dp, Neutral800, RoundedCornerShape(5.33.dp))
            .padding(horizontal = 8.dp)
            .designNode("soc_org_dropdown"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(Res.drawable.ic_org_building), null, Modifier.size(13.33.dp))
        Text(orgName, color = Neutral900, fontSize = 9.33.sp, maxLines = 1, modifier = Modifier.padding(start = 4.dp))
        Image(painterResource(Res.drawable.ic_chevron_down), null, Modifier.padding(start = 2.66.dp).size(10.67.dp))
    }
}

@Composable
private fun PlanBadge(plan: String, expiry: String, modifier: Modifier = Modifier) {
    Row(
        modifier.height(26.67.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Neutral100)
            .padding(horizontal = 10.66.dp)
            .designNode("soc_plan_badge"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(Res.drawable.ic_plan_badge_check), null, Modifier.size(13.33.dp))
        Text(plan, color = Gray999, fontSize = 8.sp, modifier = Modifier.padding(start = 2.66.dp))
        Image(painterResource(Res.drawable.ic_calendar_clock), null, Modifier.padding(start = 10.66.dp).size(13.33.dp))
        Text(expiry, color = Gray999, fontSize = 8.sp, modifier = Modifier.padding(start = 2.66.dp))
    }
}

@Composable
private fun OngoingBadge() {
    Row(
        Modifier.clip(RoundedCornerShape(100.dp)).background(RedDB0025).padding(horizontal = 5.33.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(Res.drawable.ic_broadcast), null,
            Modifier.padding(end = 2.67.dp).size(9.33.dp),
            colorFilter = ColorFilter.tint(Color.White),
        )
        Text("Ongoing", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CreateClassRow() {
    Row(
        Modifier.fillMaxWidth().height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Neutral300, RoundedCornerShape(8.dp))
            .designNode("soc_create_class"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(Res.drawable.ic_plus), null, Modifier.size(13.33.dp))
        Text("Create a new class", color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 5.33.dp))
    }
}

@Composable
private fun ClassRow(item: ClassItem, selected: Boolean, onClick: () -> Unit, nodeId: String) {
    val shape = RoundedCornerShape(5.33.dp)
    var m = Modifier.fillMaxWidth().height(45.33.dp).clip(shape).background(Neutral100)
    if (selected) m = m.border(1.33.dp, Violet4848F0, shape)
    Row(
        m.clickable(onClick = onClick).padding(10.67.dp).designNode(nodeId),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            item.name,
            color = Neutral900,
            fontSize = 10.67.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (item.ongoing) {
            OngoingBadge()
        } else {
            Image(painterResource(Res.drawable.ic_pen_straight_line), "Rename", Modifier.size(26.67.dp).padding(8.dp))
            Image(painterResource(Res.drawable.ic_trash_can), "Delete", Modifier.size(26.67.dp).padding(8.dp))
        }
    }
}

/** CMP port of `SelectOrgAndSelectClassWindow` (service path): pick org + class, then enter. */
@Composable
fun SelectOrgAndClassScreen(
    orgName: String = "Riverside Elementary",
    plan: String = "Professional",
    expiry: String = "2026/06/30",
    classes: List<ClassItem> = sampleClasses,
    onExit: () -> Unit = {},
    onEnter: (ClassItem) -> Unit = {},
    onClose: () -> Unit = {},
) {
    var selected by remember { mutableStateOf(0) }
    Box(Modifier.width(333.33.dp).height(453.33.dp)) {
        Column(
            Modifier.fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(10.66.dp))
                .background(Color.White)
                .border(0.66.dp, Neutral300, RoundedCornerShape(10.66.dp))
                .padding(start = 16.dp, top = 26.66.dp, end = 16.dp, bottom = 26.66.dp)
                .designNode("select_org_and_class"),
        ) {
            Text("Start Interactive Quizzing", color = Neutral900, fontSize = 18.67.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.designNode("soc_title"))
            Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(Res.drawable.ic_select_class_header), null, Modifier.size(13.33.dp))
                Text("Select or create a class to join", color = Neutral900, fontSize = 9.33.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 4.dp))
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(5.33.dp), verticalAlignment = Alignment.CenterVertically) {
                OrgDropdown(orgName)
                PlanBadge(plan, expiry)
            }
            Box(Modifier.fillMaxWidth().padding(top = 16.dp).height(0.67.dp).background(Neutral300))
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(top = 10.66.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { CreateClassRow() }
                itemsIndexed(classes) { i, c ->
                    ClassRow(c, i == selected, { selected = i; onEnter(c) }, "soc_class_$i")
                }
            }
            Row(
                Modifier.fillMaxWidth().height(32.dp).padding(top = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(10.66.dp),
            ) {
                CSButton(
                    "Exit ClassSwift",
                    backgroundColor = Color.White,
                    textColor = Neutral900,
                    borderColor = Neutral300,
                    textSize = 10.67.sp,
                    nodeId = "soc_exit",
                    modifier = Modifier.weight(1f).height(32.dp),
                    onClick = onExit,
                )
                CSButton(
                    "Enter Class",
                    backgroundColor = Violet4848F0,
                    textColor = Color.White,
                    textSize = 10.67.sp,
                    nodeId = "soc_enter",
                    modifier = Modifier.weight(1f).height(32.dp),
                    onClick = { onEnter(classes[selected]) },
                )
            }
        }
        Image(
            painter = painterResource(Res.drawable.ic_close),
            contentDescription = "Close",
            colorFilter = ColorFilter.tint(Neutral900),
            modifier = Modifier.align(Alignment.TopEnd).padding(24.dp).size(26.67.dp).clickable(onClick = onClose).designNode("soc_close"),
        )
    }
}
