package com.lumen.messages.ui.glass

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.lumen.messages.ui.theme.LocalPalette
import kotlin.math.max

/**
 * The app backdrop: a lit gradient (Espresso, Midnight, Black, White) or a texture
 * (Grunge, Paper) with a soft glow over it. Static, so it costs nothing per frame.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(modifier.fillMaxSize()) {
        if (p.texture != 0) {
            Image(
                painter = painterResource(p.texture),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    val textured = p.texture != 0
                    if (!textured) {
                        drawRect(Brush.verticalGradient(0f to p.bgTop, 0.38f to p.bgMid, 1f to p.bg))
                    } else {
                        // Gentle darkening toward the bottom keeps text readable over the texture.
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to p.bg.copy(alpha = if (p.dark) 0.55f else 0.25f),
                            )
                        )
                    }
                    val r = max(size.width, size.height)
                    drawCircle(
                        Brush.radialGradient(
                            listOf(p.bgTop.copy(alpha = if (textured) 0.35f else if (p.dark) 0.85f else 0.7f), Color.Transparent),
                            center = Offset(size.width * 0.78f, -r * 0.05f),
                            radius = r * 0.55f,
                        ),
                        radius = r * 0.55f,
                        center = Offset(size.width * 0.78f, -r * 0.05f),
                    )
                    drawCircle(
                        Brush.radialGradient(
                            listOf(p.accentA.copy(alpha = if (p.dark) 0.07f else 0.05f), Color.Transparent),
                            center = Offset(0f, size.height * 0.92f),
                            radius = r * 0.5f,
                        ),
                        radius = r * 0.5f,
                        center = Offset(0f, size.height * 0.92f),
                    )
                }
        )
    }
}
