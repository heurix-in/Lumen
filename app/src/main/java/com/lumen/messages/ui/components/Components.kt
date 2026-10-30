package com.lumen.messages.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.messages.ui.glass.BackdropState
import com.lumen.messages.ui.glass.GlassSurface
import com.lumen.messages.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** Springy shrink while pressed — used on every tappable surface. */
fun Modifier.pressScale(interaction: MutableInteractionSource, pressed: Float = 0.96f): Modifier = composed {
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "press",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

private val avatarGradients = listOf(
    listOf(Color(0xFFC98B6B), Color(0xFF7A4A36)),
    listOf(Color(0xFF8A9BB0), Color(0xFF46566B)),
    listOf(Color(0xFFB59A6A), Color(0xFF6E5A36)),
    listOf(Color(0xFF9C7FB0), Color(0xFF574468)),
    listOf(Color(0xFF7FA89A), Color(0xFF3F6458)),
    listOf(Color(0xFFC0787A), Color(0xFF6E3B3E)),
    listOf(Color(0xFF8C8C94), Color(0xFF4A4A52)),
)

private object PhotoCache {
    val cache = LruCache<String, ImageBitmap>(96)
}

@Composable
private fun rememberContactPhoto(uri: String?): ImageBitmap? {
    val context = LocalContext.current
    val state = produceState<ImageBitmap?>(initialValue = uri?.let { PhotoCache.cache.get(it) }, uri) {
        if (uri == null || value != null) return@produceState
        val bmp = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uri))?.use {
                    BitmapFactory.decodeStream(it)?.asImageBitmap()
                }
            }.getOrNull()
        }
        if (bmp != null) {
            PhotoCache.cache.put(uri, bmp)
            value = bmp
        }
    }
    return state.value
}

private fun initialsOf(name: String?, address: String): String? {
    val source = name?.takeIf { it.isNotBlank() } ?: address.takeIf { a -> a.any { it.isLetter() } } ?: return null
    val words = source.trim().split(Regex("\\s+")).filter { w -> w.firstOrNull()?.isLetterOrDigit() == true }
    return when {
        words.isEmpty() -> null
        words.size == 1 -> words[0].take(1).uppercase()
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

/**
 * Contact avatar: photo, initials on a gradient, or a person glyph.
 * When [selected] it flips over in 3D to reveal a check mark.
 */
@Composable
fun Avatar(
    name: String?,
    address: String,
    photoUri: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    shape: Shape = CircleShape,
) {
    val p = LocalPalette.current
    val photo = rememberContactPhoto(photoUri)
    val gradient = remember(address) { avatarGradients[abs(address.hashCode()) % avatarGradients.size] }
    val initials = remember(name, address) { initialsOf(name, address) }
    val flip by animateFloatAsState(
        targetValue = if (selected) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 380f),
        label = "flip",
    )

    Box(
        modifier
            .size(size)
            .graphicsLayer {
                rotationY = flip
                cameraDistance = 14f * density
            },
        contentAlignment = Alignment.Center,
    ) {
        if (flip <= 90f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    photo != null -> Image(
                        bitmap = photo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    initials != null -> Text(
                        initials,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = (size.value * 0.38f).sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    else -> Icon(
                        Icons.Rounded.Person, null,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.55f),
                    )
                }
            }
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
                    .clip(shape)
                    .background(p.accentBrush),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(size * 0.5f))
            }
        }
    }
}

/** Round glass button with a springy press. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    backdrop: BackdropState? = null,
    tint: Color = LocalPalette.current.text,
    glassTint: Color = LocalPalette.current.glassTint,
) {
    val interaction = remember { MutableInteractionSource() }
    GlassSurface(
        modifier = modifier.size(size).pressScale(interaction, 0.88f),
        shape = CircleShape,
        backdrop = backdrop,
        tint = glassTint,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(color = tint),
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/** Gradient count badge. */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(
        modifier
            .clip(CircleShape)
            .background(p.badge)
            .size(width = if (count > 9) 26.dp else 20.dp, height = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > 99) "99+" else count.toString(),
            style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold),
        )
    }
}
