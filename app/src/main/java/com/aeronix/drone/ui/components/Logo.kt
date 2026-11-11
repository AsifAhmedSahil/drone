package com.aeronix.drone.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Delta-wing "A" mark, same shape as the launcher icon (source box 56 x 54). */
@Composable
fun AeronixLogo(color: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Canvas(modifier.size(size)) {
        val k = this.size.width / 56f
        val yOffset = (this.size.height - 54f * k) / 2f
        fun x(v: Float) = (v - 26f) * k
        fun y(v: Float) = (v - 26f) * k + yOffset
        val path = Path().apply {
            moveTo(x(54f), y(26f))
            lineTo(x(82f), y(80f))
            lineTo(x(54f), y(66f))
            lineTo(x(26f), y(80f))
            close()
        }
        drawPath(path, color)
    }
}
