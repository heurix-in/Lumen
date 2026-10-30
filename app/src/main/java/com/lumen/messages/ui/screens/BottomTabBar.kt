package com.lumen.messages.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.messages.ui.Screen
import com.lumen.messages.ui.components.pressScale
import com.lumen.messages.ui.glass.BackdropState
import com.lumen.messages.ui.glass.GlassSurface
import com.lumen.messages.ui.theme.LType
import com.lumen.messages.ui.theme.LocalPalette

private data class Tab(val screen: Screen, val icon: ImageVector, val label: String)

private val tabs = listOf(
    Tab(Screen.List, Icons.Rounded.ChatBubble, "Chats"),
    Tab(Screen.Money, Icons.Rounded.AccountBalanceWallet, "Money"),
    Tab(Screen.Settings, Icons.Rounded.Settings, "Settings"),
)

/** Floating glass capsule with a sliding highlight, plus a separate round compose button (as in the reference). */
@Composable
fun BottomTabBar(
    current: Screen,
    onSelect: (Screen) -> Unit,
    onCompose: () -> Unit,
    backdrop: BackdropState?,
) {
    val p = LocalPalette.current
    val index = tabs.indexOfFirst { it.screen == current }.coerceAtLeast(0)
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassSurface(
            modifier = Modifier.weight(1f).height(64.dp),
            shape = RoundedCornerShape(32.dp),
            backdrop = backdrop,
            tint = p.barTint.copy(alpha = (p.barTint.alpha + 0.15f).coerceAtMost(0.9f)),
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().padding(5.dp)) {
                val w = maxWidth / tabs.size
                val x by animateDpAsState(w * index, spring(dampingRatio = 0.72f, stiffness = 480f), label = "tabX")
                Box(
                    Modifier
                        .offset(x = x)
                        .width(w)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(27.dp))
                        .background(p.chipActive.copy(alpha = if (p.dark) 0.18f else 0.08f))
                )
                Row(Modifier.fillMaxSize()) {
                    tabs.forEachIndexed { i, tab ->
                        val active = i == index
                        val tint by animateColorAsState(if (active) p.text else p.textFaint, label = "tabTint")
                        val bump by animateFloatAsState(if (active) 1f else 0.9f, spring(0.42f, 500f), label = "tabBump")
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(27.dp))
                                .clickable(remember { MutableInteractionSource() }, null) { onSelect(tab.screen) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                        ) {
                            Icon(
                                tab.icon, null, tint = tint,
                                modifier = Modifier.size(22.dp).graphicsLayer { scaleX = bump; scaleY = bump },
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                tab.label,
                                style = LType.micro.copy(
                                    color = tint, fontSize = 10.5.sp,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                ),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        val interaction = remember { MutableInteractionSource() }
        GlassSurface(
            modifier = Modifier.size(64.dp).pressScale(interaction, 0.88f),
            shape = CircleShape,
            backdrop = backdrop,
            tint = p.barTint.copy(alpha = (p.barTint.alpha + 0.15f).coerceAtMost(0.9f)),
        ) {
            Box(
                Modifier.fillMaxSize().clickable(interaction, ripple(color = p.text), onClick = onCompose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, "New message", tint = p.text, modifier = Modifier.size(28.dp))
            }
        }
    }
}
