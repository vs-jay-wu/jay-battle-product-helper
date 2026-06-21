package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_timer_down
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_timer_up
import org.jetbrains.compose.resources.painterResource

private val TimesUpRed = Color(0xFFBD0020)

/** A single digit stepper — `view_timer_number_picker.xml`: up arrow / value box / down arrow. */
@Composable
private fun DigitPicker(value: Int, onUp: () -> Unit, onDown: () -> Unit, nodeId: String) {
    Column(Modifier.padding(2.dp).designNode(nodeId), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(Res.drawable.ic_timer_up), "+", Modifier.width(39.dp).height(24.dp).clickable(onClick = onUp))
        Box(
            Modifier.padding(vertical = 2.dp).width(39.dp).height(66.33.dp).clip(RoundedCornerShape(4.dp)).border(1.dp, Color.Black, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("$value", color = Neutral900, fontSize = 40.sp) }
        Image(painterResource(Res.drawable.ic_timer_down), "-", Modifier.width(39.dp).height(24.dp).clickable(onClick = onDown))
    }
}

@Composable
private fun TimerRadio(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit, nodeId: String) {
    val color = if (enabled) Neutral900 else BorderC2C2C2
    Row(
        Modifier.then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 5.dp).designNode(nodeId),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(20.dp).clip(CircleShape).border(1.dp, if (selected) BrandBlue else BorderC2C2C2, CircleShape), contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(BrandBlue))
        }
        Text(label, color = color, fontSize = 16.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun TimerButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier, nodeId: String) {
    Box(
        modifier.height(36.33.dp).clip(RoundedCornerShape(4.dp)).background(if (enabled) BrandBlue else BorderC2C2C2)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier).designNode(nodeId),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color.White, fontSize = 14.sp) }
}

/**
 * CMP port of `TimerToolWindow` (service path): Timer (MM:SS stepper picker → countdown → Time's up)
 * and Stopwatch modes. Stateless — the window owns the timer/stopwatch state machine + sounds.
 */
@Composable
fun TimerToolScreen(
    title: String = "Timer",
    showPicker: Boolean = true,
    digits: List<Int> = listOf(0, 0, 0, 0),
    display: String = "00:00",
    timesUp: Boolean = false,
    timerSelected: Boolean = true,
    timerRadioEnabled: Boolean = true,
    stopwatchRadioEnabled: Boolean = true,
    startText: String = "Start",
    startEnabled: Boolean = false,
    showStopwatchControls: Boolean = false,
    onUp: (Int) -> Unit = {},
    onDown: (Int) -> Unit = {},
    onStart: () -> Unit = {},
    onContinue: () -> Unit = {},
    onStopwatchTryAgain: () -> Unit = {},
    onSelectTimer: () -> Unit = {},
    onSelectStopwatch: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    ToolCard("timer", width = 346.66.dp, onClose = onClose) {
        Column(
            Modifier.fillMaxWidth().padding(top = 10.66.dp).padding(horizontal = 10.66.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = Dark2E3133, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.designNode("timer_title"))
            Box(Modifier.padding(top = 10.dp).height(135.33.dp), contentAlignment = Alignment.Center) {
                if (showPicker) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DigitPicker(digits[0], { onUp(0) }, { onDown(0) }, "timer_mt")
                        DigitPicker(digits[1], { onUp(1) }, { onDown(1) }, "timer_mu")
                        Text(":", color = Neutral900, fontSize = 40.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                        DigitPicker(digits[2], { onUp(2) }, { onDown(2) }, "timer_st")
                        DigitPicker(digits[3], { onUp(3) }, { onDown(3) }, "timer_su")
                    }
                } else {
                    Text(display, color = if (timesUp) TimesUpRed else Neutral900, fontSize = 40.sp, modifier = Modifier.designNode("timer_time"))
                }
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimerRadio("Timer", timerSelected, timerRadioEnabled, onSelectTimer, "timer_radio_timer")
                TimerRadio("Stopwatch", !timerSelected, stopwatchRadioEnabled, onSelectStopwatch, "timer_radio_stopwatch")
            }
            if (showStopwatchControls) {
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TimerButton("Try again", true, onStopwatchTryAgain, Modifier.width(138.66.dp), "timer_try_again")
                    TimerButton("continue", true, onContinue, Modifier.width(138.66.dp), "timer_continue")
                }
            } else {
                TimerButton(startText, startEnabled, onStart, Modifier.padding(top = 8.dp).width(293.33.dp), "timer_start")
            }
        }
    }
}
