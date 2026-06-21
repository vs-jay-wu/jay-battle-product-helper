package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_close
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_minus_32dp
import com.viewsonic.classswift.feature.servicescreens.ui.generated.resources.ic_mvb_spinner
import org.jetbrains.compose.resources.painterResource

/**
 * CMP port of `MvbSpinnerWindow` (service path) — faithful hybrid: the window chrome (MVB header +
 * divider + loading) is Compose; the spinner wheel itself is web content, injected via [web] (an
 * Android WebView through AndroidView from the app). The offline overlay ([networkDisconnected]) is
 * the shared `NetworkDisconnectView` custom widget, injected via [disconnectMask] (also an
 * AndroidView). [loading] shows the Loading overlay until the page is ready.
 */
@Composable
fun SpinnerScreen(
    loading: Boolean = true,
    networkDisconnected: Boolean = false,
    onMinimize: () -> Unit = {},
    onClose: () -> Unit = {},
    web: @Composable (Modifier) -> Unit = { Box(it) },
    disconnectMask: @Composable (Modifier) -> Unit = { Box(it) },
) {
    // bg_neutral0_radius400_line_neutral300_border400: radius mvb_radius_400 = 10.66dp, border border_400 = 1.33dp.
    Box(Modifier.size(570.67.dp, 472.dp).clip(RoundedCornerShape(10.66.dp)).background(Color.White).border(1.33.dp, Neutral300, RoundedCornerShape(10.66.dp)).designNode("spinner")) {
        Column(Modifier.fillMaxSize()) {
            // ── MVB standard header ── (ll_header: height 32, paddingStart 10.66, paddingEnd 8) ──
            Row(
                Modifier.fillMaxWidth().height(32.dp).padding(start = 10.66.dp, end = 8.dp).designNode("spinner_header"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource(Res.drawable.ic_mvb_spinner), null, Modifier.size(21.33.dp))
                Text("Spinner", color = Neutral900, fontSize = 10.67.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 8.dp).designNode("spinner_title"))
                Image(painterResource(Res.drawable.ic_minus_32dp), "Minimize", Modifier.size(24.dp).padding(4.dp).clickable(onClick = onMinimize).designNode("spinner_minimize"))
                Spacer(Modifier.size(5.33.dp))
                Image(painterResource(Res.drawable.ic_close), "Close", Modifier.size(24.dp).padding(4.dp).clickable(onClick = onClose).designNode("spinner_close"))
            }
            Box(Modifier.fillMaxWidth().height(0.66.dp).background(Color(0xFFE6E6E6)))
            // ── Web wheel + loading overlay (wv_web_view: marginHorizontal 16, marginBottom 26.66) ──
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 26.66.dp), contentAlignment = Alignment.Center) {
                web(Modifier.fillMaxSize().designNode("spinner_web"))
                if (loading) {
                    Text("Loading...", color = Dark2E3133, fontSize = 16.sp, modifier = Modifier.designNode("spinner_loading"))
                }
            }
        }
        // ── Offline mask (ndv_network_disconnect_mask: full-bleed, cardRadius mvb_radius_400) ──
        if (networkDisconnected) {
            disconnectMask(Modifier.fillMaxSize().clip(RoundedCornerShape(10.66.dp)).designNode("spinner_offline"))
        }
    }
}
