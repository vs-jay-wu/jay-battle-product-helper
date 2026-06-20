package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viewsonic.classswift.core.ui.designNode
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.Res
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_cancel_crop_image_24dp
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_screenshot_drag
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toast_failed
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_toast_success
import org.jetbrains.compose.resources.painterResource

/** CMP port of `ToastWindow`/CSToast (service path): a success or error toast pill. */
@Composable
fun ToastScreen(message: String = "That's an error. Please try again.", success: Boolean = false) {
    val bg = if (success) ToastSuccessBg else ToastFailBg
    val border = if (success) Green500 else Red400
    val icon = if (success) Res.drawable.ic_toast_success else Res.drawable.ic_toast_failed
    Box(Modifier.fillMaxSize().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
        Row(
            Modifier.height(32.dp).clip(RoundedCornerShape(4.8.dp)).background(bg).border(1.dp, border, RoundedCornerShape(4.8.dp))
                .padding(horizontal = 9.6.dp).designNode("toast"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(icon), null, Modifier.size(20.dp))
            Text(message, color = Dark2E3133, fontSize = 12.sp, modifier = Modifier.padding(start = 7.2.dp))
        }
    }
}

/** CMP port of `CropImageWindow`/`MvbCropImageWindow` (service path): full-screen screenshot
 * crop with a drag-hint pill and a floating Cancel button. */
@Composable
fun CropImageScreen(onCancel: () -> Unit = {}) {
    Box(Modifier.fillMaxSize().background(Color(0xFF222222)).designNode("crop_image")) {
        // Drag hint pill (centered)
        Row(
            Modifier.align(Alignment.Center).height(48.dp).clip(RoundedCornerShape(5.33.dp)).background(Color(0x66000000)).padding(horizontal = 13.33.dp).designNode("crop_hint"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(Res.drawable.ic_screenshot_drag), null, Modifier.size(32.dp))
            Text("Drag and select the area to create a question", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
        }
        // Cancel button (bottom-center)
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).height(54.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(start = 24.dp, end = 10.dp).designNode("crop_cancel"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(Res.drawable.ic_cancel_crop_image_24dp), null, Modifier.size(24.dp))
            Text("Cancel", color = Dark2E3133, fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
