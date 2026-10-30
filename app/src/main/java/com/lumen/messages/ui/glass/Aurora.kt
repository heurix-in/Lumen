package com.lumen.messages.ui.glass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.lumen.messages.ui.theme.LocalPalette
import kotlin.math.max

/**
 * Plain white or pure black backdrop. A barely-visible accent glow in two corners
 * gives the glass edges something to catch, without colouring the screen.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(p.bg, p.bgSoft)))
                val r = max(size.width, size.height)
                val glow = if (p.dark) 0.10f else 0.06f
                drawCircle(
                    Brush.radialGradient(
                        listOf(p.accentA.copy(alpha = glow), Color.Transparent),
                        center = Offset(size.width * 1.0f, 0f),
                        radius = r * 0.7f,
                    ),
                    radius = r * 0.7f,
                    center = Offset(size.width * 1.0f, 0f),
                )
                drawCircle(
                    Brush.radialGradient(
                        listOf(p.accentA.copy(alpha = glow * 0.7f), Color.Transparent),
                        center = Offset(0f, size.height),
                        radius = r * 0.6f,
                    ),
                    radius = r * 0.6f,
                    center = Offset(0f, size.height),
                )
            }
    )
}
