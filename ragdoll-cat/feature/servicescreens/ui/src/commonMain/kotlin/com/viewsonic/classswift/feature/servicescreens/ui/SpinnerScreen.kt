package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Faithful CMP port of the `MvbSpinnerWindow` chrome — the VSDS standard window header (icon +
 * "Spinner" title + minimize + close) plus the divider, matching `window_mvb_spinner.xml` exactly.
 *
 * Only the chrome is Compose: the spinner wheel (WebView), the Loading label and the offline mask
 * stay native children of the window so they render identically to the original (the overlay window
 * is not hardware-accelerated, so a WebView only paints when it is a direct native child).
 */
@Composable
fun SpinnerHeader(
    onMinimize: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    Column(Modifier.fillMaxWidth().designNode("spinner_header_chrome")) {
        // ll_header: height quiz_mvb_qc_window_header_height = 32dp, paddingStart 10.66, paddingEnd 8
        Row(
            Modifier.fillMaxWidth().height(32.dp).padding(start = 10.66.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // icon quiz_mvb_qc_icon_lg = 21.33dp
            Image(painterResource(Res.drawable.ic_mvb_spinner), null, Modifier.size(21.33.dp))
            // tv_title: marginStart mvb_spacing_300 = 8dp, textSize quiz_mvb_text_md = 10.67sp, bold, neutral_900
            Text(
                "Spinner",
                color = Neutral900,
                fontSize = 10.67.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 8.dp).designNode("spinner_title"),
            )
            // iv_btn_minimize: 24dp, padding 4dp, marginEnd mvb_spacing_200 = 5.33dp
            Image(painterResource(Res.drawable.ic_minus_32dp), "Minimize", Modifier.size(24.dp).padding(4.dp).clickable(onClick = onMinimize).designNode("spinner_minimize"))
            Spacer(Modifier.size(5.33.dp))
            // iv_btn_close: 24dp, padding 4dp
            Image(painterResource(Res.drawable.ic_close), "Close", Modifier.size(24.dp).padding(4.dp).clickable(onClick = onClose).designNode("spinner_close"))
        }
        // v_header_divider: height mvb_border_100 = 0.66dp, color_E6E6E6
        Box(Modifier.fillMaxWidth().height(0.66.dp).background(Color(0xFFE6E6E6)))
    }
}
