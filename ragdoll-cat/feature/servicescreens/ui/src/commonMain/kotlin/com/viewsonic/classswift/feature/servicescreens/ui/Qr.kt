package com.viewsonic.classswift.feature.servicescreens.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/**
 * A generated QR-style matrix, deterministic from [seed] — real rendered pixels (with corner
 * finder squares) rather than a grey placeholder. Decorative for the Designer Shell previews;
 * the live app generates a scannable code from the join link.
 */
@Composable
internal fun QrMatrix(seed: String, modifier: Modifier = Modifier) {
    val n = 25
    val bits = BooleanArray(n * n)
    var h = 2166136261.toInt()
    for (c in seed) h = (h xor c.code) * 16777619
    for (i in bits.indices) {
        h = h * 1103515245 + 12345
        bits[i] = (h ushr 16 and 1) == 1
    }
    fun finder(r0: Int, c0: Int) {
        for (r in 0..6) for (c in 0..6) {
            val edge = r == 0 || r == 6 || c == 0 || c == 6
            val core = r in 2..4 && c in 2..4
            bits[(r0 + r) * n + (c0 + c)] = edge || core
        }
    }
    finder(0, 0); finder(0, n - 7); finder(n - 7, 0)
    Canvas(modifier.background(Color.White)) {
        val cell = size.minDimension / n
        for (r in 0 until n) for (c in 0 until n) {
            if (bits[r * n + c]) drawRect(Color.Black, topLeft = Offset(c * cell, r * cell), size = Size(cell, cell))
        }
    }
}
