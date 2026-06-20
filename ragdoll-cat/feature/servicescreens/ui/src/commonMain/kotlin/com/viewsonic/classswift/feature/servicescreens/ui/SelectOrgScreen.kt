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
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import org.jetbrains.compose.resources.painterResource

/** One organization row — name, membership plan, plan expiry; [enabled]=false means expired (greyed). */
data class OrgItem(val name: String, val plan: String, val expiry: String, val enabled: Boolean = true)

/** Sample orgs for the Designer Shell preview. */
val sampleOrgs = listOf(
    OrgItem("Riverside Elementary", "Professional", "2026/06/30"),
    OrgItem("Default organization", "Basic", "—"),
    OrgItem("Lincoln High School", "Professional", "2025/12/31"),
    OrgItem("Maplewood Academy", "Standard", "2026/03/15"),
)

/** `view_item_select_org.xml`: name (14sp bold) on top, plan (left) + expiry (right) below, 10sp #BDBDBD. */
@Composable
private fun OrgRow(item: OrgItem, selected: Boolean, onClick: () -> Unit, nodeId: String) {
    val bg = if (selected) BrandBlue else Color.White
    val nameColor = when {
        !item.enabled -> StrokeC3C7C7 // disabled/expired
        selected -> Color.White
        else -> Dark2E3133
    }
    Column(
        Modifier.fillMaxWidth()
            .background(bg)
            .clickable(enabled = item.enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .designNode(nodeId),
    ) {
        Text(item.name, color = nameColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Row(Modifier.fillMaxWidth()) {
            Text(item.plan, color = SubTextBDBDBD, fontSize = 10.sp, modifier = Modifier.weight(1f))
            Text(item.expiry, color = SubTextBDBDBD, fontSize = 10.sp)
        }
    }
}

/** CMP port of `SelectOrgWindow` (service path): pick which organization to enter. */
@Composable
fun SelectOrgScreen(
    orgs: List<OrgItem> = sampleOrgs,
    selectedIndex: Int = orgs.indexOfFirst { it.enabled }.coerceAtLeast(0),
    signOutVisible: Boolean = true,
    selectEnabled: Boolean = orgs.any { it.enabled },
    onSignOut: () -> Unit = {},
    onSelect: (Int) -> Unit = {},
    onClose: () -> Unit = {},
) {
    var selected by remember(orgs) { mutableStateOf(selectedIndex) }
    Box(
        Modifier.width(350.dp)
            .clip(RoundedCornerShape(10.66.dp))
            .background(WindowBgF5F5F5)
            .border(0.96.dp, StrokeC3C7C7, RoundedCornerShape(10.66.dp))
            .padding(horizontal = 10.dp, vertical = 16.dp)
            .designNode("select_org"),
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_close),
            contentDescription = "Close",
            modifier = Modifier.align(Alignment.TopEnd).size(30.dp).clickable(onClick = onClose).designNode("select_org_close"),
        )
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Select Organization",
                color = Dark2E3133,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(start = 25.dp, top = 4.dp).designNode("select_org_title"),
            )
            Box(
                Modifier.padding(top = 20.dp, bottom = 16.dp)
                    .width(300.dp)
                    .height(260.dp)
                    .clip(RoundedCornerShape(2.66.dp))
                    .background(Color.White)
                    .border(0.6.dp, StrokeC3C7C7, RoundedCornerShape(2.66.dp))
                    .designNode("select_org_list"),
            ) {
                if (orgs.isEmpty()) {
                    Text(
                        "There is no organization",
                        color = StrokeC3C7C7,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(orgs.size) { i ->
                            OrgRow(orgs[i], i == selected, { selected = i; onSelect(i) }, "select_org_item_$i")
                            if (i < orgs.lastIndex) {
                                Box(Modifier.fillMaxWidth().height(1.dp).background(StrokeC3C7C7))
                            }
                        }
                    }
                }
            }
            // DefaultButtonSelectWindow: 140×36 each; Sign Out on left (hidden when MVB-bound).
            Row(Modifier.width(300.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                if (signOutVisible) {
                    CSButton(
                        "Sign Out",
                        backgroundColor = Color.White,
                        textColor = BrandBlue,
                        borderColor = BrandBlue,
                        nodeId = "select_org_sign_out",
                        modifier = Modifier.width(140.dp).height(36.dp),
                        onClick = onSignOut,
                    )
                } else {
                    Spacer(Modifier.width(140.dp))
                }
                CSButton(
                    "Select",
                    backgroundColor = if (selectEnabled) BrandBlue else StrokeC3C7C7,
                    textColor = Color.White,
                    nodeId = "select_org_select",
                    modifier = Modifier.width(140.dp).height(36.dp),
                    onClick = { if (selectEnabled) onSelect(selected) },
                )
            }
        }
    }
}
