package com.lumen.messages.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lumen.messages.ui.components.pressScale
import com.lumen.messages.ui.glass.GlassSurface
import com.lumen.messages.ui.theme.LType
import com.lumen.messages.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingScreen(
    isDefault: Boolean,
    hasSms: Boolean,
    extrasGranted: Boolean,
    onMakeDefault: () -> Unit,
    onGrant: () -> Unit,
    onContinue: () -> Unit,
) {
    val p = LocalPalette.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        Stagger(0) { LumenLogo(Modifier.size(150.dp)) }
        Spacer(Modifier.height(18.dp))
        Stagger(1) { Text("Lumen", style = LType.hero.copy(color = p.text)) }
        Spacer(Modifier.height(6.dp))
        Stagger(2) {
            Text(
                "Your SIM texts, beautifully.",
                style = LType.body.copy(color = p.textDim),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(28.dp))

        Stagger(3) {
            GlassSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    StepRow(
                        icon = Icons.Rounded.Sms,
                        title = "Set as default SMS app",
                        body = "Lets Lumen receive, send and store your texts.",
                        done = isDefault,
                        action = "Set default",
                        onAction = onMakeDefault,
                    )
                    StepRow(
                        icon = Icons.Rounded.Notifications,
                        title = "Contacts & notifications",
                        body = "Show names, photos and new-message alerts.",
                        done = extrasGranted,
                        action = "Allow",
                        onAction = onGrant,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Stagger(4) {
            GlassSurface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                tint = p.success.copy(alpha = if (p.dark) 0.10f else 0.14f),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shield, null, tint = p.success, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Private by design", style = LType.name.copy(color = p.text))
                        Text(
                            "Lumen has no internet permission. Your messages never leave this phone.",
                            style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(26.dp))
        Stagger(5) {
            val ready = hasSms
            val interaction = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .pressScale(interaction, 0.95f)
                    .then(
                        if (ready) Modifier.shadow(20.dp, RoundedCornerShape(29.dp), ambientColor = p.accentA, spotColor = p.accentA)
                        else Modifier
                    )
                    .clip(RoundedCornerShape(29.dp))
                    .background(
                        if (ready) Brush.linearGradient(listOf(p.accentC, p.accentA, p.accentB))
                        else Brush.linearGradient(listOf(p.glassTint, p.glassTint))
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = ripple(color = Color.White),
                        enabled = ready,
                        onClick = onContinue,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (ready) "Open Lumen" else "Complete the steps above",
                    style = LType.button.copy(color = if (ready) Color.White else p.textFaint, fontWeight = FontWeight.Bold),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = p.textFaint, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                "You can switch back to your old SMS app anytime in Settings.",
                style = LType.micro.copy(color = p.textFaint),
            )
        }
    }
}

/** Rises and fades children in one after another. */
@Composable
private fun Stagger(index: Int, content: @Composable () -> Unit) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(120L + index * 90L)
        a.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 180f))
    }
    Box(
        Modifier.graphicsLayer {
            alpha = a.value.coerceIn(0f, 1f)
            translationY = (1f - a.value) * 40.dp.toPx()
            val s = 0.92f + 0.08f * a.value
            scaleX = s; scaleY = s
        }
    ) { content() }
}

@Composable
private fun StepRow(
    icon: ImageVector,
    title: String,
    body: String,
    done: Boolean,
    action: String,
    onAction: () -> Unit,
) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (done) Brush.linearGradient(listOf(p.success, p.accentB)) else p.accentBrush),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = done,
                transitionSpec = {
                    (scaleIn(spring(0.45f, 500f), initialScale = 0.2f) + fadeIn())
                        .togetherWith(scaleOut(tween(120)) + fadeOut(tween(120)))
                },
                label = "step",
            ) { d ->
                Icon(if (d) Icons.Rounded.Check else icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = LType.name.copy(color = p.text))
            Text(body, style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal))
        }
        Spacer(Modifier.width(10.dp))
        AnimatedContent(targetState = done, label = "stepAction") { d ->
            if (d) {
                Text("Done", style = LType.button.copy(color = p.success))
            } else {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(p.accentBrush)
                        .clickable(onClick = onAction)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) { Text(action, style = LType.button.copy(color = p.onAccent)) }
            }
        }
    }
}

/** Animated logo: two chat bubbles breathing, with light orbiting around them. */
@Composable
fun LumenLogo(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val t = rememberInfiniteTransition(label = "logo")
    val orbit by t.animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "orbit",
    )
    val breathe by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "breathe")

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2, h / 2)

        // Glow
        drawCircle(
            Brush.radialGradient(
                listOf(p.accentA.copy(alpha = 0.45f + 0.15f * breathe), Color.Transparent),
                center = c, radius = w * 0.55f,
            ),
            radius = w * 0.55f, center = c,
        )
        // Orbit ring and comets
        drawCircle(p.glassEdgeLight.copy(alpha = 0.25f), radius = w * 0.44f, center = c, style = Stroke(1.dp.toPx()))
        for (i in 0 until 3) {
            val a = orbit + i * (2 * PI / 3).toFloat()
            val pos = Offset(c.x + cos(a) * w * 0.44f, c.y + sin(a) * w * 0.44f)
            drawCircle(listOf(p.accentB, p.accentC, p.success)[i], radius = 3.5.dp.toPx(), center = pos)
        }

        // Back bubble (gradient)
        val s = 1f + 0.03f * breathe
        val bw = w * 0.46f * s
        val bh = h * 0.34f * s
        val bTopLeft = Offset(c.x - bw * 0.72f, c.y - bh * 0.78f)
        drawRoundRect(
            Brush.linearGradient(listOf(p.accentC, p.accentA, p.accentB), start = bTopLeft, end = bTopLeft + Offset(bw, bh)),
            topLeft = bTopLeft, size = Size(bw, bh), cornerRadius = CornerRadius(bh * 0.42f),
        )
        val tail = Path().apply {
            moveTo(bTopLeft.x + bw * 0.18f, bTopLeft.y + bh * 0.9f)
            lineTo(bTopLeft.x + bw * 0.08f, bTopLeft.y + bh * 1.22f)
            lineTo(bTopLeft.x + bw * 0.42f, bTopLeft.y + bh * 0.95f)
            close()
        }
        drawPath(tail, p.accentA)

        // Front bubble (glass)
        val fw = w * 0.40f
        val fh = h * 0.30f
        val fTopLeft = Offset(c.x - fw * 0.15f, c.y - fh * 0.05f)
        drawRoundRect(Color.White.copy(alpha = 0.28f), topLeft = fTopLeft, size = Size(fw, fh), cornerRadius = CornerRadius(fh * 0.45f))
        drawRoundRect(
            Color.White.copy(alpha = 0.75f), topLeft = fTopLeft, size = Size(fw, fh),
            cornerRadius = CornerRadius(fh * 0.45f), style = Stroke(1.5.dp.toPx()),
        )
        // Typing dots
        for (i in 0 until 3) {
            val phase = ((orbit * 1.5f) - i * 0.7f)
            val lift = (sin(phase) * 0.5f + 0.5f) * fh * 0.10f
            drawCircle(
                Color.White,
                radius = fh * 0.075f,
                center = Offset(fTopLeft.x + fw * (0.30f + i * 0.20f), fTopLeft.y + fh * 0.5f - lift),
            )
        }
    }
}
