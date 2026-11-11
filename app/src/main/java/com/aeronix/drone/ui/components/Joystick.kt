package com.aeronix.drone.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aeronix.drone.ui.NA
import kotlin.math.roundToInt

/**
 * Virtual joystick. [onChange] gets x/y in -1..1 (y up is positive).
 * When [enabled] is false the stick is drawn locked and ignores touch.
 * In this version its output is only shown on screen; nothing is ever sent to the ESP32.
 */
@Composable
fun Joystick(
    title: String,
    axes: String,
    enabled: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
    onChange: (x: Float, y: Float) -> Unit = { _, _ -> },
) {
    val colors = MaterialTheme.colorScheme
    var norm by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }

    val spec: AnimationSpec<Float> = if (dragging) snap() else spring(dampingRatio = 0.55f, stiffness = 400f)
    val ax by animateFloatAsState(if (dragging) norm.x else 0f, spec, label = "stickX")
    val ay by animateFloatAsState(if (dragging) norm.y else 0f, spec, label = "stickY")

    val ring = if (enabled) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.35f)
    val faint = colors.outline
    val thumb = if (enabled) colors.primary else colors.surfaceContainerHighest

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .size(size)
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        detectDragGestures(
                            onDragStart = { dragging = true },
                            onDragEnd = { dragging = false; norm = Offset.Zero; onChange(0f, 0f) },
                            onDragCancel = { dragging = false; norm = Offset.Zero; onChange(0f, 0f) },
                        ) { change, _ ->
                            change.consume()
                            val center = Offset(this.size.width / 2f, this.size.height / 2f)
                            val travel = this.size.width / 2f * 0.62f
                            var v = (change.position - center) / travel
                            val len = v.getDistance()
                            if (len > 1f) v = v / len
                            norm = v
                            onChange(v.x, -v.y)
                        }
                    }
            ) {
                val c = Offset(this.size.width / 2f, this.size.height / 2f)
                val r = this.size.width / 2f - 2.dp.toPx()
                drawCircle(faint, radius = r, center = c, style = Stroke(1.5.dp.toPx()))
                drawCircle(
                    ring.copy(alpha = 0.5f), radius = r * 0.62f, center = c,
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
                )
                drawLine(faint, Offset(c.x - r, c.y), Offset(c.x + r, c.y), 1.dp.toPx(), StrokeCap.Round)
                drawLine(faint, Offset(c.x, c.y - r), Offset(c.x, c.y + r), 1.dp.toPx(), StrokeCap.Round)
                val travel = r * 0.62f
                val t = Offset(c.x + ax * travel, c.y + ay * travel)
                drawCircle(thumb.copy(alpha = 0.18f), radius = r * 0.34f, center = t)
                drawCircle(thumb, radius = r * 0.22f, center = t)
                drawCircle(colors.background, radius = r * 0.07f, center = t)
            }
            if (!enabled) {
                Icon(
                    Icons.Rounded.Lock, contentDescription = "Locked",
                    tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(size * 0.14f),
                )
            }
        }
        Text(title, style = MaterialTheme.typography.labelLarge, color = colors.onSurface, modifier = Modifier.padding(top = 10.dp), textAlign = TextAlign.Center)
        Text(axes, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** Centre-zero bar for one control channel. [value] is -1..1, or null when there is no input. */
@Composable
fun ChannelBar(label: String, value: Float?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val fill = colors.primary
    val track = colors.outline
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant, modifier = Modifier.width(34.dp))
        Canvas(Modifier.weight(1f).height(8.dp)) {
            val h = size.height
            drawRoundRect(track, size = Size(size.width, h), cornerRadius = CornerRadius(h / 2f))
            val mid = size.width / 2f
            drawLine(colors.onSurfaceVariant.copy(alpha = 0.5f), Offset(mid, 0f), Offset(mid, h), 2f)
            if (value != null && value != 0f) {
                val w = (mid * value.coerceIn(-1f, 1f))
                val left = if (w >= 0) mid else mid + w
                drawRoundRect(fill, topLeft = Offset(left, 0f), size = Size(kotlin.math.abs(w), h), cornerRadius = CornerRadius(h / 2f))
            }
        }
        Text(
            if (value == null) NA else "${(value * 100).roundToInt()}",
            style = MaterialTheme.typography.labelMedium,
            color = if (value == null) colors.onSurfaceVariant.copy(alpha = 0.6f) else colors.onSurface,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End,
        )
    }
}
