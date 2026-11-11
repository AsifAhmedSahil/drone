package com.aeronix.drone.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeronix.drone.data.AccentTheme
import com.aeronix.drone.data.BackgroundTheme

/** Fixed brand palette. */
object Aero {
    val Black = Color(0xFF000000)
    val Charcoal = Color(0xFF101010)
    val Card = Color(0xFF171717)
    val Border = Color(0xFF2A2A2A)
    val Blue = Color(0xFF168BFF)
    val Silver = Color(0xFFBFC5CE)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA0A0A0)
    val Green = Color(0xFF22C55E)
    val Red = Color(0xFFEF4444)
    val Amber = Color(0xFFF5A524)
}

private val AeroTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.4.sp),
)

private val AeroShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
fun AeronixTheme(
    background: BackgroundTheme,
    accent: AccentTheme,
    content: @Composable () -> Unit,
) {
    val bg = if (background == BackgroundTheme.PURE_BLACK) Aero.Black else Aero.Charcoal
    val primary = if (accent == AccentTheme.BLUE) Aero.Blue else Aero.Silver
    val scheme = darkColorScheme(
        primary = primary,
        onPrimary = Aero.Black,
        primaryContainer = primary.copy(alpha = 0.16f),
        onPrimaryContainer = primary,
        secondary = Aero.Silver,
        onSecondary = Aero.Black,
        background = bg,
        onBackground = Aero.TextPrimary,
        surface = bg,
        onSurface = Aero.TextPrimary,
        surfaceVariant = Aero.Card,
        onSurfaceVariant = Aero.TextSecondary,
        surfaceContainerLowest = Aero.Black,
        surfaceContainerLow = Aero.Charcoal,
        surfaceContainer = Aero.Card,
        surfaceContainerHigh = Color(0xFF1E1E1E),
        surfaceContainerHighest = Color(0xFF262626),
        outline = Aero.Border,
        outlineVariant = Color(0xFF222222),
        error = Aero.Red,
    )
    MaterialTheme(colorScheme = scheme, typography = AeroTypography, shapes = AeroShapes, content = content)
}
