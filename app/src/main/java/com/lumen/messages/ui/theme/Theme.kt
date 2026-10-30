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
import com.lumen.messages.data.ThemeMode

@Immutable
data class LumenPalette(
    val dark: Boolean,
    val bgTop: Color,       // warm glow at the top of the screen
    val bgMid: Color,
    val bg: Color,          // bottom / base colour
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
    val ink: Color,         // sent bubble ("ink" style)
    val onInk: Color,
    val onAccent: Color,
    val chipActive: Color,
    val onChipActive: Color,
    val badge: Color,
    val danger: Color,
    val success: Color,
    val scrim: Color,
    val divider: Color,
) {
    val accentBrush: Brush get() = Brush.linearGradient(listOf(accentA, accentB))
}

enum class LumenSurface { Espresso, Black, White }

/** Espresso (warm glow fading to black), pure Black, or White — all with glass surfaces. */
fun buildPalette(surface: LumenSurface, accent: Color): LumenPalette = when (surface) {
    LumenSurface.Espresso -> LumenPalette(
        dark = true,
        bgTop = Color(0xFF6B4A3B),
        bgMid = Color(0xFF2B1F1A),
        bg = Color(0xFF0B0908),
        bgSoft = Color(0xFF14100E),
        text = Color(0xFFF7F1EC),
        textDim = Color(0xFFF7F1EC).copy(alpha = 0.62f),
        textFaint = Color(0xFFF7F1EC).copy(alpha = 0.38f),
        glassTint = Color.White.copy(alpha = 0.08f),
        barTint = Color(0xFF1E1613).copy(alpha = 0.42f),
        glassEdgeLight = Color.White.copy(alpha = 0.16f),
        glassEdgeDark = Color.White.copy(alpha = 0.03f),
        glassSheen = Color.White.copy(alpha = 0.04f),
        accentA = accent,
        accentB = lerp(accent, Color.White, 0.25f),
        accentC = lerp(accent, Color.Black, 0.2f),
        received = Color(0xFF2A201C).copy(alpha = 0.78f),
        ink = Color(0xFF0D0A09),
        onInk = Color(0xFFF7F1EC),
        onAccent = Color.White,
        chipActive = Color.White.copy(alpha = 0.22f),
        onChipActive = Color.White,
        badge = Color(0xFFFF3B30),
        danger = Color(0xFFFF5C5C),
        success = Color(0xFF3DDC97),
        scrim = Color.Black.copy(alpha = 0.6f),
        divider = Color.White.copy(alpha = 0.07f),
    )
    LumenSurface.Black -> LumenPalette(
        dark = true,
        bgTop = Color(0xFF16161A),
        bgMid = Color(0xFF060607),
        bg = Color.Black,
        bgSoft = Color(0xFF0A0A0C),
        text = Color.White,
        textDim = Color.White.copy(alpha = 0.62f),
        textFaint = Color.White.copy(alpha = 0.38f),
        glassTint = Color.White.copy(alpha = 0.075f),
        barTint = Color.Black.copy(alpha = 0.5f),
        glassEdgeLight = Color.White.copy(alpha = 0.16f),
        glassEdgeDark = Color.White.copy(alpha = 0.03f),
        glassSheen = Color.White.copy(alpha = 0.04f),
        accentA = accent,
        accentB = lerp(accent, Color.White, 0.25f),
        accentC = lerp(accent, Color.Black, 0.2f),
        received = Color.White.copy(alpha = 0.10f),
        ink = Color(0xFF2A2A2E),
        onInk = Color.White,
        onAccent = Color.White,
        chipActive = Color.White.copy(alpha = 0.22f),
        onChipActive = Color.White,
        badge = Color(0xFFFF3B30),
        danger = Color(0xFFFF5C5C),
        success = Color(0xFF34D399),
        scrim = Color.Black.copy(alpha = 0.62f),
        divider = Color.White.copy(alpha = 0.07f),
    )
    LumenSurface.White -> LumenPalette(
        dark = false,
        bgTop = Color(0xFFF3E9E2),
        bgMid = Color(0xFFFAF7F5),
        bg = Color.White,
        bgSoft = Color(0xFFF7F5F3),
        text = Color(0xFF14100E),
        textDim = Color(0xFF14100E).copy(alpha = 0.60f),
        textFaint = Color(0xFF14100E).copy(alpha = 0.38f),
        glassTint = Color(0xFF8C7F78).copy(alpha = 0.09f),
        barTint = Color.White.copy(alpha = 0.66f),
        glassEdgeLight = Color.Black.copy(alpha = 0.08f),
        glassEdgeDark = Color.Black.copy(alpha = 0.03f),
        glassSheen = Color.White.copy(alpha = 0.5f),
        accentA = accent,
        accentB = lerp(accent, Color.White, 0.22f),
        accentC = lerp(accent, Color.Black, 0.18f),
        received = Color(0xFF8C7F78).copy(alpha = 0.13f),
        ink = Color(0xFF14100E),
        onInk = Color.White,
        onAccent = Color.White,
        chipActive = Color(0xFF14100E),
        onChipActive = Color.White,
        badge = Color(0xFFFF3B30),
        danger = Color(0xFFE5334B),
        success = Color(0xFF12A36F),
        scrim = Color.Black.copy(alpha = 0.30f),
        divider = Color.Black.copy(alpha = 0.06f),
    )
}

/** Look settings the screens need while drawing. */
@Immutable
data class LookSettings(
    val bubbleStyle: BubbleStyle = BubbleStyle.Ink,
    val bubbleShape: BubbleShape = BubbleShape.Round,
    val blur: BlurLevel = BlurLevel.Medium,
    val reduceMotion: Boolean = false,
)

val LocalPalette = staticCompositionLocalOf { buildPalette(LumenSurface.Espresso, Color(0xFFF5793B)) }
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

fun surfaceFor(mode: ThemeMode, systemDark: Boolean): LumenSurface = when (mode) {
    ThemeMode.System -> if (systemDark) LumenSurface.Espresso else LumenSurface.White
    ThemeMode.Dark -> LumenSurface.Espresso
    ThemeMode.Black -> LumenSurface.Black
    ThemeMode.Light -> LumenSurface.White
}

@Composable
fun LumenTheme(prefs: Prefs, surface: LumenSurface, content: @Composable () -> Unit) {
    val accent = Color(prefs.accent.toInt())
    val palette = remember(surface, accent) { buildPalette(surface, accent) }
    val look = LookSettings(prefs.bubbleStyle, prefs.bubbleShape, prefs.blur, prefs.reduceMotion)
    val scheme = if (palette.dark) {
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
