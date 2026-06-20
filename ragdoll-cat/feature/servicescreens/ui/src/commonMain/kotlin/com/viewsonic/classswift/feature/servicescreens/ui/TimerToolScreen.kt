package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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

@Composable
private fun RadioOption(label: String, selected: Boolean, onClick: () -> Unit, nodeId: String) {
    Row(
        Modifier.clickable(onClick = onClick).padding(horizontal = 5.dp).designNode(nodeId),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).border(1.dp, if (selected) BrandBlue else Neutral500, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(BrandBlue))
        }
        Text(label, color = Neutral900, fontSize = 16.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

/** CMP port of `TimerToolWindow` (service path): title, MM:SS picker, Timer/Stopwatch radios, Start. */
@Composable
fun TimerToolScreen(onClose: () -> Unit = {}) {
    var stopwatch by remember { mutableStateOf(false) }
    ToolCard("timer", width = 346.66.dp, onClose = onClose) {
        Column(
            Modifier.fillMaxWidth().padding(top = 10.66.dp).padding(horizontal = 10.66.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Timer", color = Dark2E3133, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.designNode("timer_title"))
            // Time picker: 4 digit columns + colon (idle state shows 00:00).
            Row(
                Modifier.padding(top = 10.dp).height(135.33.dp).designNode("timer_picker"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DigitColumn('0'); DigitColumn('0')
                Text(":", color = Neutral900, fontSize = 40.sp, modifier = Modifier.width(30.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                DigitColumn('0'); DigitColumn('0')
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RadioOption("Timer", !stopwatch, { stopwatch = false }, "timer_radio_timer")
                RadioOption("Stopwatch", stopwatch, { stopwatch = true }, "timer_radio_stopwatch")
            }
            Box(
                Modifier.padding(top = 8.dp).width(293.33.dp).height(36.33.dp)
                    .clip(RoundedCornerShape(4.dp)).background(BrandBlue).designNode("timer_start"),
                contentAlignment = Alignment.Center,
            ) {
                Text("Start", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

/** A single large time digit, styled like the `CSTimerNumberPicker` selected value. */
@Composable
private fun DigitColumn(ch: Char) {
    Text(ch.toString(), color = Neutral900, fontSize = 40.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(40.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}
