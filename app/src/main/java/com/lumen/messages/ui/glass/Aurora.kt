package com.lumen.messages.ui.glass

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import com.lumen.messages.ui.theme.LocalPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/** Slowly drifting colour fields that the glass panels float over. Animated in the draw phase only. */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val reduce = rememberReduceMotion()
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduce) 0f else (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(36_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduce) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(9_000), RepeatMode.Reverse),
        label = "breathe",
    )

    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(p.bgTop, p.bgBottom)))
                val w = size.width
                val h = size.height
                val r = max(w, h)
                val t = phase
                blob(p.blob1, Offset(w * (0.22f + 0.18f * cos(t)), h * (0.16f + 0.08f * sin(t * 2))), r * (0.62f + 0.05f * breathe))
                blob(p.blob2, Offset(w * (0.88f + 0.12f * sin(t + 1.3f)), h * (0.40f + 0.12f * cos(t))), r * 0.55f)
                blob(p.blob3, Offset(w * (0.18f + 0.20f * sin(t * 1.5f + 2f)), h * (0.78f + 0.08f * cos(t + 0.6f))), r * (0.52f - 0.04f * breathe))
                blob(p.blob4, Offset(w * (0.70f + 0.15f * cos(t * 0.8f + 4f)), h * (0.95f + 0.06f * sin(t))), r * 0.48f)
                // Soft vignette keeps edges calm so glass highlights read clearly.
                drawRect(
                    Brush.radialGradient(
                        listOf(Color.Transparent, p.bgBottom.copy(alpha = if (p.dark) 0.55f else 0.20f)),
                        center = Offset(w / 2, h * 0.45f),
                        radius = r * 0.85f,
                    )
                )
            }
    )
}

private fun DrawScope.blob(color: Color, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(color, color.copy(alpha = color.alpha * 0.45f), color.copy(alpha = 0f)),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
