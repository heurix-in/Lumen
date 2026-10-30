package com.lumen.messages.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.lumen.messages.R
import com.lumen.messages.data.AppFont
import com.lumen.messages.data.AvatarStyle
import com.lumen.messages.data.BlurLevel
import com.lumen.messages.data.BubbleShape
import com.lumen.messages.data.BubbleStyle
import com.lumen.messages.data.Prefs
import com.lumen.messages.data.ThemeMode

@Immutable
data class LumenPalette(
    val dark: Boolean,
    val bgTop: Color,       // glow at the top of the screen
    val bgMid: Color,
    val bg: Color,          // bottom / base colour
    val bgSoft: Color,
    val texture: Int,       // drawable for textured backgrounds, 0 = none
    val text: Color,
    val textDim: Color,
    val textFaint: Color,
    val glassTint: Color,
    val barTint: Color,
    val tabBar: Color,      // denser glass for the bottom tab bar so it never mixes with chat rows
    val glassEdgeLight: Color,
    val glassEdgeDark: Color,
    val glassSheen: Color,
    val accentA: Color,
    val accentB: Color,
    val accentC: Color,
    val received: Color,
    val ink: Color,
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

enum class LumenSurface { Espresso, Midnight, Grunge, Black, Paper, White }

private fun darkPalette(
    accent: Color, top: Color, mid: Color, bg: Color, texture: Int = 0,
    received: Color, ink: Color, bar: Color, tab: Color,
) = LumenPalette(
    dark = true,
    bgTop = top, bgMid = mid, bg = bg, bgSoft = lerp(bg, Color.White, 0.03f), texture = texture,
    text = Color(0xFFF7F3F0),
    textDim = Color(0xFFF7F3F0).copy(alpha = 0.62f),
    textFaint = Color(0xFFF7F3F0).copy(alpha = 0.38f),
    glassTint = Color.White.copy(alpha = 0.08f),
    barTint = bar,
    tabBar = tab,
    glassEdgeLight = Color.White.copy(alpha = 0.16f),
    glassEdgeDark = Color.White.copy(alpha = 0.035f),
    glassSheen = Color.White.copy(alpha = 0.04f),
    accentA = accent,
    accentB = lerp(accent, Color.White, 0.25f),
    accentC = lerp(accent, Color.Black, 0.2f),
    received = received,
    ink = ink,
    onInk = Color(0xFFF7F3F0),
    onAccent = Color.White,
    chipActive = Color.White.copy(alpha = 0.22f),
    onChipActive = Color.White,
    badge = Color(0xFFFF3B30),
    danger = Color(0xFFFF5C5C),
    success = Color(0xFF3DDC97),
    scrim = Color.Black.copy(alpha = 0.6f),
    divider = Color.White.copy(alpha = 0.07f),
)

private fun lightPalette(accent: Color, top: Color, mid: Color, bg: Color, texture: Int = 0, tint: Color) = LumenPalette(
    dark = false,
    bgTop = top, bgMid = mid, bg = bg, bgSoft = lerp(bg, Color.Black, 0.03f), texture = texture,
    text = Color(0xFF14100E),
    textDim = Color(0xFF14100E).copy(alpha = 0.60f),
    textFaint = Color(0xFF14100E).copy(alpha = 0.38f),
    glassTint = tint.copy(alpha = 0.09f),
    barTint = Color.White.copy(alpha = 0.66f),
    tabBar = Color.White.copy(alpha = 0.90f),
    glassEdgeLight = Color.Black.copy(alpha = 0.08f),
    glassEdgeDark = Color.Black.copy(alpha = 0.03f),
    glassSheen = Color.White.copy(alpha = 0.5f),
    accentA = accent,
    accentB = lerp(accent, Color.White, 0.22f),
    accentC = lerp(accent, Color.Black, 0.18f),
    received = tint.copy(alpha = 0.14f),
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

fun buildPalette(surface: LumenSurface, accent: Color): LumenPalette = when (surface) {
    LumenSurface.Espresso -> darkPalette(
        accent, top = Color(0xFF6B4A3B), mid = Color(0xFF2B1F1A), bg = Color(0xFF0B0908),
        received = Color(0xFF2A201C).copy(alpha = 0.78f), ink = Color(0xFF0D0A09),
        bar = Color(0xFF1E1613).copy(alpha = 0.42f), tab = Color(0xFF17110F).copy(alpha = 0.86f),
    )
    LumenSurface.Midnight -> darkPalette(
        accent, top = Color(0xFF173A6B), mid = Color(0xFF0A1830), bg = Color(0xFF02060E),
        received = Color(0xFF13233D).copy(alpha = 0.80f), ink = Color(0xFF050A14),
        bar = Color(0xFF0A1428).copy(alpha = 0.45f), tab = Color(0xFF07101F).copy(alpha = 0.88f),
    )
    LumenSurface.Grunge -> darkPalette(
        accent, top = Color(0xFF2A2827), mid = Color(0xFF141313), bg = Color(0xFF0A0A0A), texture = R.drawable.bg_grunge,
        received = Color(0xFF242221).copy(alpha = 0.82f), ink = Color(0xFF050505),
        bar = Color(0xFF111010).copy(alpha = 0.50f), tab = Color(0xFF0E0D0D).copy(alpha = 0.90f),
    )
    LumenSurface.Black -> darkPalette(
        accent, top = Color(0xFF16161A), mid = Color(0xFF060607), bg = Color.Black,
        received = Color.White.copy(alpha = 0.10f), ink = Color(0xFF2A2A2E),
        bar = Color.Black.copy(alpha = 0.5f), tab = Color(0xFF0E0E10).copy(alpha = 0.90f),
    )
    LumenSurface.Paper -> lightPalette(
        accent, top = Color(0xFFEDE3D6), mid = Color(0xFFF6F1EA), bg = Color(0xFFFBF8F3),
        texture = R.drawable.bg_paper, tint = Color(0xFF8A7560),
    )
    LumenSurface.White -> lightPalette(
        accent, top = Color(0xFFF3E9E2), mid = Color(0xFFFAF7F5), bg = Color.White, tint = Color(0xFF8C7F78),
    )
}

/** Look settings the screens need while drawing. */
@Immutable
data class LookSettings(
    val bubbleStyle: BubbleStyle = BubbleStyle.Ink,
    val bubbleShape: BubbleShape = BubbleShape.Round,
    val blur: BlurLevel = BlurLevel.Medium,
    val reduceMotion: Boolean = false,
    val avatarStyle: AvatarStyle = AvatarStyle.Orb,
)

val LocalPalette = staticCompositionLocalOf { buildPalette(LumenSurface.Espresso, Color(0xFFF5793B)) }
val LocalLook = staticCompositionLocalOf { LookSettings() }

private fun variableFamily(res: Int): FontFamily =
    FontFamily(listOf(300, 400, 500, 600, 700, 800).map { Font(res, FontWeight(it)) })

fun fontFamilyFor(font: AppFont): FontFamily = when (font) {
    AppFont.System -> FontFamily.Default
    AppFont.Manrope -> variableFamily(R.font.manrope)
    AppFont.Outfit -> variableFamily(R.font.outfit)
    AppFont.Jakarta -> variableFamily(R.font.jakarta)
    AppFont.Sora -> variableFamily(R.font.sora)
    AppFont.SpaceGrotesk -> variableFamily(R.font.spacegrotesk)
    AppFont.Lexend -> variableFamily(R.font.lexend)
    AppFont.Serif -> FontFamily.Serif
    AppFont.Mono -> FontFamily.Monospace
}

/**
 * App text styles. The font family and weight follow Settings → Text style; they are
 * snapshot state, so every screen re-styles instantly when the user picks a new font.
 */
object LType {
    var family by mutableStateOf<FontFamily>(FontFamily.Default)
    var weightShift by mutableIntStateOf(0)

    private fun s(
        size: TextUnit,
        weight: Int,
        letter: TextUnit = 0.sp,
        line: TextUnit = TextUnit.Unspecified,
    ) = TextStyle(
        fontFamily = family,
        fontSize = size,
        fontWeight = FontWeight((weight + weightShift).coerceIn(200, 900)),
        letterSpacing = letter,
        lineHeight = line,
    )

    val hero: TextStyle get() = s(32.sp, 700, (-0.8).sp)
    val title: TextStyle get() = s(20.sp, 600, (-0.3).sp)
    val name: TextStyle get() = s(16.sp, 600, (-0.1).sp)
    val body: TextStyle get() = s(15.5.sp, 400, 0.05.sp, 21.sp)
    val snippet: TextStyle get() = s(14.sp, 400, 0.sp, 19.sp)
    val caption: TextStyle get() = s(12.sp, 500, 0.2.sp)
    val micro: TextStyle get() = s(11.sp, 500, 0.3.sp)
    val button: TextStyle get() = s(15.sp, 600, 0.1.sp)
    val section: TextStyle get() = s(13.sp, 600, 0.6.sp)
}

fun surfaceFor(mode: ThemeMode, systemDark: Boolean): LumenSurface = when (mode) {
    ThemeMode.System -> if (systemDark) LumenSurface.Espresso else LumenSurface.White
    ThemeMode.Dark -> LumenSurface.Espresso
    ThemeMode.Midnight -> LumenSurface.Midnight
    ThemeMode.Grunge -> LumenSurface.Grunge
    ThemeMode.Black -> LumenSurface.Black
    ThemeMode.Paper -> LumenSurface.Paper
    ThemeMode.Light -> LumenSurface.White
}

@Composable
fun LumenTheme(prefs: Prefs, surface: LumenSurface, content: @Composable () -> Unit) {
    val accent = Color(prefs.accent.toInt())
    val palette = remember(surface, accent) { buildPalette(surface, accent) }
    val look = LookSettings(prefs.bubbleStyle, prefs.bubbleShape, prefs.blur, prefs.reduceMotion, prefs.avatarStyle)
    val family = remember(prefs.font) { fontFamilyFor(prefs.font) }
    if (LType.family != family) LType.family = family
    if (LType.weightShift != prefs.textWeight.shift) LType.weightShift = prefs.textWeight.shift
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
