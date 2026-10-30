package com.lumen.messages.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class LumenPalette(
    val dark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val blob1: Color,
    val blob2: Color,
    val blob3: Color,
    val blob4: Color,
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
) {
    val accentBrush: Brush get() = Brush.linearGradient(listOf(accentA, accentB))
}

val DarkPalette = LumenPalette(
    dark = true,
    bgTop = Color(0xFF0B0B1A),
    bgBottom = Color(0xFF070712),
    blob1 = Color(0xFF6D4AFF).copy(alpha = 0.55f),
    blob2 = Color(0xFF00B8FF).copy(alpha = 0.40f),
    blob3 = Color(0xFFFF4FA3).copy(alpha = 0.32f),
    blob4 = Color(0xFF22E3B5).copy(alpha = 0.22f),
    text = Color(0xFFF4F3FF),
    textDim = Color(0xFFF4F3FF).copy(alpha = 0.64f),
    textFaint = Color(0xFFF4F3FF).copy(alpha = 0.40f),
    glassTint = Color.White.copy(alpha = 0.075f),
    barTint = Color(0xFF12122A).copy(alpha = 0.42f),
    glassEdgeLight = Color.White.copy(alpha = 0.32f),
    glassEdgeDark = Color.White.copy(alpha = 0.05f),
    glassSheen = Color.White.copy(alpha = 0.07f),
    accentA = Color(0xFF7C5CFF),
    accentB = Color(0xFF00B8FF),
    accentC = Color(0xFFD45CFF),
    received = Color.White.copy(alpha = 0.11f),
    onAccent = Color.White,
    danger = Color(0xFFFF5C7A),
    success = Color(0xFF3DDC97),
    scrim = Color(0xFF05050C).copy(alpha = 0.55f),
)

val LightPalette = LumenPalette(
    dark = false,
    bgTop = Color(0xFFF4F1FF),
    bgBottom = Color(0xFFEAF4FF),
    blob1 = Color(0xFF9B87FF).copy(alpha = 0.50f),
    blob2 = Color(0xFF6FD6FF).copy(alpha = 0.48f),
    blob3 = Color(0xFFFF9BCB).copy(alpha = 0.42f),
    blob4 = Color(0xFF8CF0D2).copy(alpha = 0.38f),
    text = Color(0xFF15122E),
    textDim = Color(0xFF15122E).copy(alpha = 0.62f),
    textFaint = Color(0xFF15122E).copy(alpha = 0.40f),
    glassTint = Color.White.copy(alpha = 0.46f),
    barTint = Color.White.copy(alpha = 0.50f),
    glassEdgeLight = Color.White.copy(alpha = 0.95f),
    glassEdgeDark = Color(0xFF15122E).copy(alpha = 0.06f),
    glassSheen = Color.White.copy(alpha = 0.35f),
    accentA = Color(0xFF6E4BFF),
    accentB = Color(0xFF0AA6F0),
    accentC = Color(0xFFC04BFF),
    received = Color.White.copy(alpha = 0.66f),
    onAccent = Color.White,
    danger = Color(0xFFE5395B),
    success = Color(0xFF1DB57A),
    scrim = Color(0xFF15122E).copy(alpha = 0.28f),
)

val LocalPalette = staticCompositionLocalOf { DarkPalette }

object LType {
    val hero = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
    val title = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
    val name = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp)
    val body = TextStyle(fontSize = 15.5.sp, lineHeight = 21.sp, letterSpacing = 0.05.sp)
    val snippet = TextStyle(fontSize = 14.sp, lineHeight = 19.sp)
    val caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp)
    val micro = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
    val button = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp)
}

@Composable
fun LumenTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val palette = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(primary = palette.accentA, secondary = palette.accentB, error = palette.danger)
    } else {
        lightColorScheme(primary = palette.accentA, secondary = palette.accentB, error = palette.danger)
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalPalette provides palette, content = content)
    }
}
