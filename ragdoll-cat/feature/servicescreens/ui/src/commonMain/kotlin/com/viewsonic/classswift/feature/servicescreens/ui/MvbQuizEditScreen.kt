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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
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
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_add
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_arrow_clockwise
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_camera_viewfinder
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_chevron_down
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_cross
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_image_corners
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_minus_32dp
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_quiz_v2
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_trash_can
import org.jetbrains.compose.resources.painterResource

/** Image-upload lifecycle of the quiz editor (mirrors MvbImageUploadView's progress/uploaded/failed states). */
enum class EditImageState { UPLOADING, UPLOADED, FAILED }

/** The question-image upload area (`MvbImageUploadView`): 277.33dp white framed box that shows the
 *  upload progress, the uploaded screenshot (+ "Capture again"), or a failed state (+ "Try again"). */
@Composable
private fun EditImageArea(
    state: EditImageState,
    progress: Int,
    image: @Composable (Modifier) -> Unit,
    onCaptureAgain: () -> Unit,
    onTryAgain: () -> Unit,
) {
    val shape = RoundedCornerShape(5.33.dp)
    Box(
        Modifier.fillMaxWidth().height(277.33.dp).clip(shape).background(Color.White).border(0.66.dp, Neutral300, shape).designNode("edit_image"),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            EditImageState.UPLOADED -> {
                image(Modifier.fillMaxSize().padding(2.66.dp).clip(shape))
                Row(
                    Modifier.align(Alignment.BottomEnd).padding(6.66.dp).height(32.dp).clip(RoundedCornerShape(5.33.dp))
                        .background(Neutral100).clickable(onClick = onCaptureAgain).padding(horizontal = 16.dp).designNode("edit_capture_again"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(painterResource(Res.drawable.ic_camera_viewfinder), null, Modifier.size(13.3.dp), colorFilter = ColorFilter.tint(Neutral900))
                    Text("Capture again", color = Neutral900, fontSize = 10.66.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 5.33.dp))
                }
            }
            EditImageState.UPLOADING -> {
                // The captured screenshot shows under a white_a80 mask while it uploads.
                image(Modifier.fillMaxSize().padding(2.66.dp).clip(shape))
                Box(Modifier.fillMaxSize().padding(2.66.dp).clip(shape).background(WhiteA80))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(Modifier.size(32.dp), color = Violet4848F0, strokeWidth = 2.dp)
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.padding(top = 10.66.dp).width(200.dp).height(4.dp).clip(RoundedCornerShape(50)),
                        color = Violet4848F0,
                        trackColor = Neutral100,
                    )
                    Text("Preparing question $progress%", color = Neutral900, fontSize = 10.66.sp, modifier = Modifier.padding(top = 10.66.dp))
                }
            }
            EditImageState.FAILED -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(Res.drawable.ic_image_corners), null, Modifier.size(21.33.dp))
                Text("Failed to show image", color = Neutral900, fontSize = 10.66.sp, modifier = Modifier.padding(top = 2.66.dp))
                Row(
                    Modifier.padding(top = 10.66.dp).height(37.33.dp).clip(RoundedCornerShape(5.33.dp)).background(Violet4848F0)
                        .clickable(onClick = onTryAgain).padding(horizontal = 16.dp).designNode("edit_try_again"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(painterResource(Res.drawable.ic_arrow_clockwise), null, Modifier.size(16.dp), colorFilter = ColorFilter.tint(Color.White))
                    Text("Try again", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 5.33.dp))
                }
            }
        }
    }
}

/** One editor option box (`view_option_box`): a 106.67dp neutral100 tile (radius 8, neutral300 border)
 *  with a big centered label and a trash icon (removable only above the 2-option minimum). */
@Composable
private fun OptionBox(label: String, removable: Boolean, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Box(modifier.height(106.67.dp).clip(shape).background(Neutral100).border(1.33.dp, Neutral300, shape).designNode("edit_opt_$label")) {
        Text(label, color = Neutral900, fontSize = 26.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
        if (removable) {
            Image(
                painterResource(Res.drawable.ic_trash_can), "Remove option",
                Modifier.align(Alignment.TopEnd).padding(5.33.dp).size(20.dp).clip(RoundedCornerShape(50)).clickable(onClick = onRemove).padding(2.66.dp),
                colorFilter = ColorFilter.tint(Neutral900),
            )
        }
    }
}

/** A labelled settings dropdown (`cl_answer_types` / `cl_answer_options`): value + chevron over a
 *  neutral0 input, opening a menu of [options]. */
@Composable
private fun EditDropdown(label: String, value: String, options: List<String>, nodeId: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label, color = Neutral900, fontSize = 9.33.sp, modifier = Modifier.padding(start = 2.66.dp))
        Box {
            Row(
                Modifier.padding(top = 2.66.dp).fillMaxWidth().height(37.33.dp).clip(RoundedCornerShape(5.33.dp))
                    .background(Color.White).border(0.66.dp, if (expanded) Violet4848F0 else Neutral300, RoundedCornerShape(5.33.dp))
                    .clickable { expanded = true }.padding(horizontal = 10.66.dp).designNode(nodeId),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(value, color = Neutral900, fontSize = 10.67.sp, modifier = Modifier.weight(1f))
                Image(painterResource(Res.drawable.ic_chevron_down), null, Modifier.size(10.66.dp), colorFilter = ColorFilter.tint(Neutral900))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt, fontSize = 10.67.sp) }, onClick = { onSelect(opt); expanded = false })
                }
            }
        }
    }
}

/** Editor option panel (`MvbOptionPanel`, MC / Poll): a row of option boxes (+ add, 2–6) over a
 *  neutral100 settings card with the Answer-types (ABC/123) and Answer-options dropdowns. */
@Composable
private fun EditOptionPanel(
    count: Int,
    letters: Boolean,
    answerOptionsValue: String,
    answerOptions: List<String>,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onLettersChange: (Boolean) -> Unit,
    onAnswerOptionsChange: (String) -> Unit,
) {
    Column(Modifier.padding(top = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            repeat(count) { i ->
                val label = if (letters) ('A' + i).toString() else (i + 1).toString()
                OptionBox(label, removable = count > 2, onRemove = onRemove, modifier = Modifier.weight(1f).padding(end = 10.67.dp))
            }
            if (count < 6) {
                Box(
                    Modifier.size(24.dp).clip(RoundedCornerShape(5.33.dp)).background(Neutral100).clickable(onClick = onAdd).designNode("edit_add_option"),
                    contentAlignment = Alignment.Center,
                ) { Image(painterResource(Res.drawable.ic_add), "Add option", Modifier.size(16.dp), colorFilter = ColorFilter.tint(Neutral900)) }
            }
        }
        Row(
            Modifier.padding(top = 10.66.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Neutral100).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.66.dp),
        ) {
            EditDropdown("Answer types", if (letters) "ABC" else "123", listOf("ABC", "123"), "edit_answer_type", { onLettersChange(it == "ABC") }, Modifier.width(133.33.dp))
            EditDropdown("Answer options", answerOptionsValue, answerOptions, "edit_answer_options", onAnswerOptionsChange, Modifier.width(133.33.dp))
        }
    }
}

/**
 * CMP port of the `Mvb*EditWindow` quiz editors (service path): a 541.33×426.66 card — header,
 * the question-image upload area, and a Cancel / Start-question action bar. This image-only variant
 * covers True/False, Short Answer, Audio and Sketch; Multiple-Choice / Poll add an option panel (step 2).
 * The uploaded screenshot is drawn by the window (Coil) via the [image] slot.
 */
@Composable
fun MvbQuizEditScreen(
    type: MvbQuizType = MvbQuizType.TRUE_FALSE,
    imageState: EditImageState = EditImageState.UPLOADING,
    progress: Int = 0,
    startEnabled: Boolean = false,
    image: @Composable (Modifier) -> Unit = {},
    initialOptionCount: Int = 4,
    initialLetters: Boolean = true,
    initialSingle: Boolean = true,
    onOptionConfigChanged: (count: Int, letters: Boolean, single: Boolean) -> Unit = { _, _, _ -> },
    onClose: () -> Unit = {},
    onMinimize: () -> Unit = {},
    onCaptureAgain: () -> Unit = {},
    onTryAgain: () -> Unit = {},
    onCancel: () -> Unit = {},
    onStart: () -> Unit = {},
) {
    val hasOptions = type == MvbQuizType.MULTIPLE_CHOICE || type == MvbQuizType.POLL
    val isPoll = type == MvbQuizType.POLL
    val singleLabel = if (isPoll) "Single vote" else "Single-select"
    val multiLabel = if (isPoll) "Multiple votes" else "Multi-select"
    var optionCount by remember { mutableStateOf(initialOptionCount) }
    var letters by remember { mutableStateOf(initialLetters) }
    var single by remember { mutableStateOf(initialSingle) }
    val cardShape = RoundedCornerShape(5.33.dp)
    Box(Modifier.size(541.33.dp, if (hasOptions) 626.dp else 426.66.dp).padding(8.dp)) {
        Column(
            Modifier.fillMaxSize().clip(cardShape).background(Color.White).border(0.66.dp, Neutral300, cardShape).designNode("mvb_quiz_edit"),
        ) {
            // Header
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.66.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(Res.drawable.ic_quiz_v2), null, Modifier.size(21.33.dp))
                Text("Question", color = Neutral900, fontSize = 10.66.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 5.33.dp).weight(1f).designNode("edit_title"))
                Image(painterResource(Res.drawable.ic_minus_32dp), "Minimize", Modifier.size(24.dp).clickable(onClick = onMinimize).padding(4.dp), colorFilter = ColorFilter.tint(Neutral900))
                Image(painterResource(Res.drawable.ic_cross), "Close", Modifier.size(24.dp).clickable(onClick = onClose).padding(4.dp).designNode("edit_close"))
            }
            Box(Modifier.fillMaxWidth().height(0.66.dp).background(Color(0xFFE6E6E6)))
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                EditImageArea(imageState, progress, image, onCaptureAgain, onTryAgain)
                if (hasOptions) {
                    EditOptionPanel(
                        count = optionCount,
                        letters = letters,
                        answerOptionsValue = if (single) singleLabel else multiLabel,
                        answerOptions = listOf(singleLabel, multiLabel),
                        onAdd = { if (optionCount < 6) { optionCount++; onOptionConfigChanged(optionCount, letters, single) } },
                        onRemove = { if (optionCount > 2) { optionCount--; onOptionConfigChanged(optionCount, letters, single) } },
                        onLettersChange = { letters = it; onOptionConfigChanged(optionCount, it, single) },
                        onAnswerOptionsChange = { single = it == singleLabel; onOptionConfigChanged(optionCount, letters, single) },
                    )
                }
                Spacer(Modifier.weight(1f))
                // Action bar
                Row(Modifier.padding(top = 16.dp).fillMaxWidth().height(37.33.dp), horizontalArrangement = Arrangement.spacedBy(10.66.dp)) {
                    CSButton("Cancel question", backgroundColor = Color.White, textColor = Neutral900, borderColor = Neutral300, textSize = 12.sp, nodeId = "edit_cancel", onClick = onCancel, modifier = Modifier.weight(1f).fillMaxSize())
                    CSButton(
                        "Start question",
                        backgroundColor = if (startEnabled) Violet4848F0 else Neutral200,
                        textColor = if (startEnabled) Color.White else Neutral500,
                        textSize = 12.sp, nodeId = "edit_start",
                        onClick = { if (startEnabled) onStart() },
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    )
                }
            }
        }
    }
}
