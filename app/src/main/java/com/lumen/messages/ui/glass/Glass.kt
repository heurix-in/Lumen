package com.lumen.messages.ui.glass

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lumen.messages.ui.theme.LocalLook
import com.lumen.messages.ui.theme.LocalPalette

/** Real backdrop blur needs Android 12+. Older phones get a denser frosted tint instead. */
val CanBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Holds a recording of whatever is drawn "behind the glass" (e.g. the scrolling message list),
 * so glass surfaces on top can draw a blurred copy of exactly the part they cover.
 */
@Stable
class BackdropState internal constructor(internal val layer: GraphicsLayer) {
    internal var origin by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberBackdropState(): BackdropState {
    val layer = rememberGraphicsLayer()
    return remember(layer) { BackdropState(layer) }
}

/** Marks content as the source that glass surfaces blur. Glass must NOT be placed inside it. */
fun Modifier.backdropSource(state: BackdropState): Modifier = this
    .onGloballyPositioned { state.origin = it.positionInRoot() }
    .drawWithContent {
        state.layer.record { this@drawWithContent.drawContent() }
        drawLayer(state.layer)
    }

/**
 * A frosted-glass panel. With a [backdrop] it blurs the content behind it in real time;
 * without one it is a translucent tinted pane (cheap enough for list items).
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backdrop: BackdropState? = null,
    tint: Color = LocalPalette.current.glassTint,
    blur: Dp = 28.dp,
    border: Boolean = true,
    sheen: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val p = LocalPalette.current
    // Only blurring surfaces need their own layer; plain tinted panes stay cheap.
    val blurLayer = if (backdrop != null && CanBlur) rememberGraphicsLayer() else null
    var selfOrigin by remember { mutableStateOf(Offset.Zero) }
    val useBlur = backdrop != null && CanBlur
    val blurPx = with(LocalDensity.current) { blur.toPx() } * LocalLook.current.blur.scale
    val fill = if (backdrop != null && !CanBlur) {
        tint.copy(alpha = (tint.alpha + 0.38f).coerceAtMost(0.95f))
    } else tint
    val edge = remember(p) {
        Brush.linearGradient(
            listOf(p.glassEdgeLight, p.glassEdgeDark, p.glassEdgeLight.copy(alpha = p.glassEdgeLight.alpha * 0.45f))
        )
    }

    Box(
        modifier = modifier
            .then(if (useBlur) Modifier.onGloballyPositioned { selfOrigin = it.positionInRoot() } else Modifier)
            .clip(shape)
            .drawBehind {
                if (useBlur && backdrop != null && blurLayer != null) {
                    blurLayer.renderEffect = BlurEffect(blurPx, blurPx, TileMode.Clamp)
                    val dx = selfOrigin.x - backdrop.origin.x
                    val dy = selfOrigin.y - backdrop.origin.y
                    blurLayer.record {
                        translate(-dx, -dy) { drawLayer(backdrop.layer) }
                    }
                    drawLayer(blurLayer)
                }
                drawRect(fill)
                if (sheen) {
                    drawRect(
                        Brush.linearGradient(
                            listOf(p.glassSheen, Color.Transparent),
                            start = Offset.Zero,
                            end = Offset(size.width * 0.75f, size.height),
                        )
                    )
                }
            }
            .then(if (border) Modifier.border(1.dp, edge, shape) else Modifier),
        content = content,
    )
}
