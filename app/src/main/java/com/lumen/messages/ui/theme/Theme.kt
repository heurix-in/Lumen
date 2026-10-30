package com.lumen.messages.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.lumen.messages.data.BlurLevel
import com.lumen.messages.data.BubbleShape
import com.lumen.messages.data.BubbleStyle
import com.lumen.messages.data.Prefs

@Immutable
data class LumenPalette(
    val dark: Boolean,
    val bg: Color,
    val bgSoft: Color,
    val text: Color,
    val textDim: Color,
    val textFaint: Color,
    val glassTint: Color,
    val barTint: Color,
    val glassEdgeLight: Color,
    val glassEdgeDark: Color,
    val glassSheen: Color,
    val accentA: Color,
    val accentB: Color,
    val accentC: Color,
    val received: Color,
    val onAccent: Color,
    val danger: Color,
    val success: Color,
    val scrim: Color,
    val divider: Color,
) {
    val accentBrush: Brush get() = Brush.linearGradient(listOf(accentA, accentB))
}

/** Pure black or pure white base, glass surfaces, and one accent colour. */
fun buildPalette(dark: Boolean, accent: Color): LumenPalette = if (dark) {
    LumenPalette(
        dark = true,
        bg = Color.Black,
        bgSoft = Color(0xFF0A0A0C),
        text = Color.White,
        textDim = Color.White.copy(alpha = 0.62f),
        textFaint = Color.White.copy(alpha = 0.38f),
        glassTint = Color.White.copy(alpha = 0.075f),
        barTint = Color.Black.copy(alpha = 0.55f),
        glassEdgeLight = Color.White.copy(alpha = 0.20f),
        glassEdgeDark = Color.White.copy(alpha = 0.04f),
        glassSheen = Color.White.copy(alpha = 0.05f),
        accentA = accent,
        accentB = lerp(accent, Color.White, 0.28f),
        accentC = lerp(accent, Color.Black, 0.22f),
        received = Color.White.copy(alpha = 0.11f),
        onAccent = Color.White,
        danger = Color(0xFFFF5C6C),
        success = Color(0xFF34D399),
        scrim = Color.Black.copy(alpha = 0.62f),
        divider = Color.White.copy(alpha = 0.08f),
    )
} else {
    LumenPalette(
        dark = false,
        bg = Color.White,
        bgSoft = Color(0xFFF4F5F8),
        text = Color(0xFF0B0B10),
        textDim = Color(0xFF0B0B10).copy(alpha = 0.60f),
        textFaint = Color(0xFF0B0B10).copy(alpha = 0.38f),
        glassTint = Color(0xFF8A8FA3).copy(alpha = 0.085f),
        barTint = Color.White.copy(alpha = 0.72f),
        glassEdgeLight = Color.Black.copy(alpha = 0.09f),
        glassEdgeDark = Color.Black.copy(alpha = 0.03f),
        glassSheen = Color.White.copy(alpha = 0.55f),
        accentA = accent,
        accentB = lerp(accent, Color.White, 0.22f),
        accentC = lerp(accent, Color.Black, 0.18f),
        received = Color(0xFF8A8FA3).copy(alpha = 0.13f),
        onAccent = Color.White,
        danger = Color(0xFFE5334B),
        success = Color(0xFF12A36F),
        scrim = Color.Black.copy(alpha = 0.30f),
        divider = Color.Black.copy(alpha = 0.07f),
    )
}

/** Look settings the screens need while drawing. */
@Immutable
data class LookSettings(
    val bubbleStyle: BubbleStyle = BubbleStyle.Solid,
    val bubbleShape: BubbleShape = BubbleShape.Round,
    val blur: BlurLevel = BlurLevel.Medium,
    val reduceMotion: Boolean = false,
)

val LocalPalette = staticCompositionLocalOf { buildPalette(true, Color(0xFF3D7BFF)) }
val LocalLook = staticCompositionLocalOf { LookSettings() }

object LType {
    val hero = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
    val title = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
    val name = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp)
    val body = TextStyle(fontSize = 15.5.sp, lineHeight = 21.sp, letterSpacing = 0.05.sp)
    val snippet = TextStyle(fontSize = 14.sp, lineHeight = 19.sp)
    val caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp)
    val micro = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
    val button = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp)
    val section = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
}

@Composable
fun LumenTheme(prefs: Prefs, dark: Boolean, content: @Composable () -> Unit) {
    val accent = Color(prefs.accent.toInt())
    val palette = remember(dark, accent) { buildPalette(dark, accent) }
    val look = LookSettings(prefs.bubbleStyle, prefs.bubbleShape, prefs.blur, prefs.reduceMotion)
    val scheme = if (dark) {
        darkColorScheme(primary = accent, secondary = palette.accentB, error = palette.danger, background = palette.bg)
    } else {
        lightColorScheme(primary = accent, secondary = palette.accentB, error = palette.danger, background = palette.bg)
    }
    val base = LocalDensity.current
    val scaled = Density(base.density, base.fontScale * prefs.textScale)
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalPalette provides palette,
            LocalLook provides look,
            LocalDensity provides scaled,
            content = content,
        )
    }
}
