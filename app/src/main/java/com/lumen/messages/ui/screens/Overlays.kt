package com.lumen.messages.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lumen.messages.ui.components.pressScale
import com.lumen.messages.ui.glass.BackdropState
import com.lumen.messages.ui.glass.GlassSurface
import com.lumen.messages.ui.theme.LType
import com.lumen.messages.ui.theme.LocalPalette

/** Centered glass confirmation card that springs in over a dimmed, blurred screen. */
@Composable
fun GlassConfirm(
    visible: Boolean,
    backdrop: BackdropState?,
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val p = LocalPalette.current
    AnimatedVisibility(visible, enter = fadeIn(tween(180)), exit = fadeOut(tween(160))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(p.scrim)
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            GlassSurface(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth()
                    .animateEnterExit(
                        enter = scaleIn(spring(0.62f, 420f), initialScale = 0.82f) + fadeIn(),
                        exit = scaleOut(tween(140), targetScale = 0.9f) + fadeOut(tween(140)),
                    )
                    .clickable(remember { MutableInteractionSource() }, indication = null) {},
                shape = RoundedCornerShape(30.dp),
                backdrop = backdrop,
                tint = p.barTint.copy(alpha = (p.barTint.alpha + 0.2f).coerceAtMost(0.9f)),
                blur = 36.dp,
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, style = LType.title.copy(color = p.text), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(message, style = LType.snippet.copy(color = p.textDim), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PillButton("Cancel", Modifier.weight(1f), filled = false, onClick = onDismiss)
                        PillButton(confirmLabel, Modifier.weight(1f), filled = true, danger = true, onClick = onConfirm)
                    }
                }
            }
        }
    }
}

@Composable
fun PillButton(
    label: String,
    modifier: Modifier = Modifier,
    filled: Boolean,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)
    GlassSurface(
        modifier = modifier.height(50.dp).pressScale(interaction, 0.94f),
        shape = shape,
        tint = when {
            filled && danger -> p.danger
            filled -> p.accentA
            else -> p.glassTint
        },
        sheen = true,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = interaction, indication = ripple(color = Color.White), onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = LType.button.copy(color = if (filled) Color.White else p.text))
        }
    }
}

data class SheetAction(
    val label: String,
    val icon: ImageVector,
    val danger: Boolean = false,
    val onClick: () -> Unit,
)

/** Bottom glass sheet that slides up with a spring. */
@Composable
fun GlassActionSheet(
    visible: Boolean,
    backdrop: BackdropState?,
    preview: String?,
    actions: List<SheetAction>,
    onDismiss: () -> Unit,
) {
    val p = LocalPalette.current
    AnimatedVisibility(visible, enter = fadeIn(tween(160)), exit = fadeOut(tween(200))) {
        Box(
            Modifier
                .fillMaxSize()
                .background(p.scrim)
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            GlassSurface(
                modifier = Modifier
                    .padding(10.dp)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .animateEnterExit(
                        enter = slideInVertically(spring(0.78f, 420f)) { it } + fadeIn(),
                        exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(200)),
                    )
                    .clickable(remember { MutableInteractionSource() }, indication = null) {},
                shape = RoundedCornerShape(30.dp),
                backdrop = backdrop,
                tint = p.barTint.copy(alpha = (p.barTint.alpha + 0.2f).coerceAtMost(0.9f)),
                blur = 36.dp,
            ) {
                Column(Modifier.padding(vertical = 10.dp)) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(p.textFaint)
                    )
                    if (!preview.isNullOrBlank()) {
                        Text(
                            preview,
                            style = LType.snippet.copy(color = p.textDim),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                        )
                    } else Spacer(Modifier.height(10.dp))
                    actions.forEach { a ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(onClick = a.onClick)
                                .padding(horizontal = 22.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(a.icon, null, tint = if (a.danger) p.danger else p.text, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(a.label, style = LType.name.copy(color = if (a.danger) p.danger else p.text))
                        }
                    }
                }
            }
        }
    }
}
