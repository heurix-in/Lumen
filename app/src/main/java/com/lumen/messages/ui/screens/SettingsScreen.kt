package com.lumen.messages.ui.screens

import com.lumen.messages.ui.theme.surfaceFor
import com.lumen.messages.ui.theme.fontFamilyFor
import com.lumen.messages.ui.theme.buildPalette
import com.lumen.messages.ui.theme.LumenSurface
import com.lumen.messages.ui.components.Avatar
import com.lumen.messages.data.TextWeight
import com.lumen.messages.data.AvatarStyle
import com.lumen.messages.data.AppFont
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Image
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.AnimatedVisibility
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.messages.data.AccentChoices
import com.lumen.messages.data.BlurLevel
import com.lumen.messages.data.BubbleShape
import com.lumen.messages.data.BubbleStyle
import com.lumen.messages.data.ThemeMode
import com.lumen.messages.notify.Notifier
import com.lumen.messages.ui.AppViewModel
import com.lumen.messages.ui.components.GlassIconButton
import com.lumen.messages.ui.glass.GlassSurface
import com.lumen.messages.ui.glass.backdropSource
import com.lumen.messages.ui.glass.rememberBackdropState
import com.lumen.messages.ui.theme.LType
import com.lumen.messages.ui.theme.LocalLook
import com.lumen.messages.ui.theme.LocalPalette
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: AppViewModel, onRequestDefault: () -> Unit) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val prefs = vm.prefs
    val backdrop = rememberBackdropState()
    var headerPx by remember { mutableIntStateOf(0) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().backdropSource(backdrop),
            contentPadding = PaddingValues(
                top = with(density) { headerPx.toDp() } + 12.dp,
                bottom = navBottom + BottomBarSpace,
                start = 14.dp, end = 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "preview") { ChatPreview() }

            item(key = "look-h") { SectionHeader("Customize look") }
            item(key = "look") {
                Group {
                    LabeledControl("Background") {
                        BackgroundPicker(prefs.themeMode) { prefs.themeMode = it }
                    }
                    Divider()
                    LabeledControl("Theme colour") { Swatches(prefs.accent) { prefs.accent = it } }
                    Divider()
                    LabeledControl("Sent bubbles") {
                        Segmented(BubbleStyle.values().toList(), prefs.bubbleStyle, { it.label }) { prefs.bubbleStyle = it }
                    }
                    Divider()
                    LabeledControl("Bubble shape") {
                        Segmented(BubbleShape.values().toList(), prefs.bubbleShape, { it.label }) { prefs.bubbleShape = it }
                    }
                    Divider()
                    LabeledControl("Glass blur") {
                        Segmented(BlurLevel.values().toList(), prefs.blur, { it.label }) { prefs.blur = it }
                    }
                    Divider()
                    LabeledControl("Text size  ·  ${(prefs.textScale * 100).roundToInt()}%") {
                        Slider(
                            value = prefs.textScale,
                            onValueChange = { prefs.textScale = (it * 20).roundToInt() / 20f },
                            valueRange = 0.85f..1.30f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = p.accentA,
                                activeTrackColor = p.accentA,
                                inactiveTrackColor = p.glassTint,
                                activeTickColor = Color.White.copy(alpha = 0.5f),
                                inactiveTickColor = p.textFaint,
                            ),
                        )
                    }
                    Divider()
                    ToggleRow("Reduce motion", "Fewer animations", prefs.reduceMotion) { prefs.reduceMotion = it }
                }
            }

            item(key = "type-h") { SectionHeader("Text style") }
            item(key = "type") {
                Group {
                    LabeledControl("Font") { FontPicker(prefs.font) { prefs.font = it } }
                    Divider()
                    LabeledControl("Weight") {
                        Segmented(TextWeight.values().toList(), prefs.textWeight, { it.label }) { prefs.textWeight = it }
                    }
                    Divider()
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("The quick brown fox", style = LType.title.copy(color = p.text))
                        Text(
                            "Your code is 482913 · Rs 2,500 credited to A/c XX1234",
                            style = LType.body.copy(color = p.textDim),
                        )
                    }
                }
            }

            item(key = "avatar-h") { SectionHeader("Contact icons") }
            item(key = "avatar") {
                Group {
                    LabeledControl("Style for contacts without a photo") {
                        AvatarPicker(prefs.avatarStyle) { prefs.avatarStyle = it }
                    }
                }
            }

            item(key = "msg-h") { SectionHeader("Messages") }
            item(key = "msg") {
                Group {
                    ToggleRow("Smart tabs", "Personal, Transactions, OTPs and Updates", prefs.smartTabs) { prefs.smartTabs = it }
                    Divider()
                    ToggleRow("Delivery reports", "Show when a text reaches the other phone", prefs.deliveryReports) {
                        prefs.deliveryReports = it
                    }
                }
            }

            item(key = "otp-help") { OtpHelp(isDefault = vm.isDefaultSms, onRequestDefault = onRequestDefault) }

            item(key = "notif-h") { SectionHeader("Customize notifications") }
            item(key = "notif") {
                Group {
                    ToggleRow("Reply button", null, prefs.notifReply) { prefs.notifReply = it }
                    Divider()
                    ToggleRow("Mark as read button", null, prefs.notifMarkRead) { prefs.notifMarkRead = it }
                    Divider()
                    ToggleRow("Copy code button", "For OTP / verification messages", prefs.notifCopyCode) { prefs.notifCopyCode = it }
                    Divider()
                    ToggleRow("Show contact", "Sender name in notifications", prefs.notifShowContact) { prefs.notifShowContact = it }
                    Divider()
                    ToggleRow("Show message text", "Turn off to hide texts on the lock screen", prefs.notifShowText) {
                        prefs.notifShowText = it
                    }
                    Divider()
                    ToggleRow("Sound in open chat", null, prefs.inChatSound) { prefs.inChatSound = it }
                    Divider()
                    ToggleRow("Vibrate in open chat", null, prefs.inChatVibrate) { prefs.inChatVibrate = it }
                    Divider()
                    LinkRow("Sound, vibration & pop-up", "Android notification settings") {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    .putExtra(Settings.EXTRA_CHANNEL_ID, Notifier.CHANNEL)
                            )
                        }
                    }
                }
            }

            item(key = "default-h") { SectionHeader("Default SMS app") }
            item(key = "default") {
                Group {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(38.dp).clip(CircleShape).background(if (vm.isDefaultSms) p.success else p.accentA),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (vm.isDefaultSms) Icons.Rounded.Check else Icons.Rounded.Sms, null,
                                tint = Color.White, modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (vm.isDefaultSms) "Lumen is your SMS app" else "Lumen is not your SMS app",
                                style = LType.name.copy(color = p.text),
                            )
                            Text(
                                if (vm.isDefaultSms) "Switch back anytime in Android Settings → Default apps."
                                else "New texts go to your other app until you switch.",
                                style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal),
                            )
                        }
                        if (!vm.isDefaultSms) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(p.accentA)
                                    .clickable(onClick = onRequestDefault)
                                    .padding(horizontal = 14.dp, vertical = 9.dp)
                            ) { Text("Switch", style = LType.button.copy(color = Color.White)) }
                        }
                    }
                }
            }

            item(key = "privacy") {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(22.dp),
                    tint = p.success.copy(alpha = if (p.dark) 0.10f else 0.08f),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Shield, null, tint = p.success, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Private by design", style = LType.name.copy(color = p.text))
                            Text(
                                "No internet permission. Messages, contacts and bank summaries never leave this phone.",
                                style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal),
                            )
                        }
                    }
                }
            }
            item(key = "about") {
                Text(
                    "Lumen 1.3",
                    style = LType.micro.copy(color = p.textFaint),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        GlassSurface(
            modifier = Modifier.fillMaxWidth().onSizeChanged { headerPx = it.height },
            shape = RoundedCornerShape(0.dp),
            backdrop = backdrop,
            tint = p.barTint,
            border = false,
            sheen = false,
        ) {
            Row(
                Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(8.dp))
                Text("Settings", style = LType.hero.copy(color = p.text, fontSize = 30.sp))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    val p = LocalPalette.current
    Text(
        text.uppercase(),
        style = LType.section.copy(color = p.accentA),
        modifier = Modifier.padding(start = 10.dp, top = 14.dp, bottom = 2.dp),
    )
}

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(content = content)
    }
}

@Composable
private fun Divider() {
    val p = LocalPalette.current
    Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp).background(p.divider))
}

@Composable
private fun LabeledControl(label: String, control: @Composable () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, style = LType.name.copy(color = p.text))
        Spacer(Modifier.height(10.dp))
        control()
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = LType.name.copy(color = p.text, fontWeight = FontWeight.Medium))
            if (subtitle != null) {
                Text(subtitle, style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal))
            }
        }
        Spacer(Modifier.width(12.dp))
        GlassToggle(checked)
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = LType.name.copy(color = p.text, fontWeight = FontWeight.Medium))
            Text(subtitle, style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal))
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = p.textDim)
    }
}

/** Glass switch with a springy thumb. */
@Composable
fun GlassToggle(checked: Boolean) {
    val p = LocalPalette.current
    val x by animateDpAsState(if (checked) 22.dp else 2.dp, spring(dampingRatio = 0.6f, stiffness = 700f), label = "thumb")
    val track by animateColorAsState(if (checked) p.accentA else p.glassTint, label = "track")
    Box(
        Modifier
            .size(width = 50.dp, height = 30.dp)
            .clip(CircleShape)
            .background(track)
            .border(1.dp, if (checked) Color.Transparent else p.glassEdgeLight, CircleShape)
    ) {
        Box(
            Modifier
                .offset(x = x, y = 2.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun Swatches(selected: Long, onSelect: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        AccentChoices.forEach { c ->
            val active = c == selected
            val scale by animateFloatAsState(if (active) 1.12f else 1f, spring(0.45f, 500f), label = "sw")
            Box(
                Modifier
                    .size(34.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(CircleShape)
                    .background(Color(c.toInt()))
                    .clickable { onSelect(c) },
                contentAlignment = Alignment.Center,
            ) {
                if (active) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Live preview of the current look, like the "Hi! / Yo" sample in other SMS apps. */
@Composable
private fun ChatPreview() {
    val p = LocalPalette.current
    val look = LocalLook.current
    val big = if (look.bubbleShape == BubbleShape.Round) 20.dp else 11.dp
    val small = if (look.bubbleShape == BubbleShape.Round) 6.dp else 4.dp
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(topStart = big, topEnd = big, bottomEnd = big, bottomStart = small))
                    .background(p.received)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) { Text("Hi! Is the meeting still at 4?", style = LType.body.copy(color = p.text)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                val shape = RoundedCornerShape(topStart = big, topEnd = big, bottomEnd = small, bottomStart = big)
                val m = Modifier.widthIn(max = 260.dp).clip(shape)
                Box(
                    when (look.bubbleStyle) {
                        BubbleStyle.Ink -> m.background(p.ink).border(1.dp, p.glassEdgeDark, shape)
                        BubbleStyle.Accent -> m.background(p.accentA)
                        BubbleStyle.Glass -> m
                            .background(p.accentA.copy(alpha = if (p.dark) 0.26f else 0.16f))
                            .border(1.dp, p.accentB.copy(alpha = 0.6f), shape)
                    }.padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        "Yes, see you there 👍",
                        style = LType.body.copy(
                            color = when (look.bubbleStyle) {
                                BubbleStyle.Ink -> p.onInk
                                BubbleStyle.Accent -> Color.White
                                BubbleStyle.Glass -> p.text
                            }
                        ),
                    )
                }
            }
        }
    }
}

/** Explains the real reasons OTP texts can skip a third-party SMS app, with a fix for each. */
@Composable
private fun OtpHelp(isDefault: Boolean, onRequestDefault: () -> Unit) {
    val p = LocalPalette.current
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (open) 90f else 0f, label = "otpChev")
    GlassSurface(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(24.dp)) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Not getting OTP messages?", style = LType.name.copy(color = p.text, fontWeight = FontWeight.Medium))
                    Text("Three things to check", style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal))
                }
                Icon(
                    Icons.Rounded.ChevronRight, null, tint = p.textDim,
                    modifier = Modifier.graphicsLayer { rotationZ = rotation },
                )
            }
            AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HelpStep(
                        1,
                        if (isDefault) "Lumen is your default SMS app ✓"
                        else "Make Lumen your default SMS app. Android 15+ hides OTP texts from other apps for 3 hours.",
                    )
                    if (!isDefault) {
                        Box(
                            Modifier
                                .padding(start = 30.dp, top = 4.dp, bottom = 6.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(p.chipActive)
                                .clickable(onClick = onRequestDefault)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) { Text("Set default", style = LType.button.copy(color = p.onChipActive, fontSize = 14.sp)) }
                    }
                    HelpStep(
                        2,
                        "Turn off RCS chats in Google Messages (Settings → RCS chats → off). Many banks now send OTPs as RCS, " +
                            "which only Google Messages can receive. With RCS off they arrive as normal SMS.",
                    )
                    val gm = remember {
                        context.packageManager.getLaunchIntentForPackage("com.google.android.apps.messaging")
                    }
                    if (gm != null) {
                        Box(
                            Modifier
                                .padding(start = 30.dp, top = 4.dp, bottom = 6.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(p.chipActive)
                                .clickable { runCatching { context.startActivity(gm) } }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) { Text("Open Google Messages", style = LType.button.copy(color = p.onChipActive, fontSize = 14.sp)) }
                    }
                    HelpStep(
                        3,
                        "On OnePlus, Oppo, Realme, Vivo or Xiaomi phones, turn off \u201cVerification code protection\u201d " +
                            "(Settings → Privacy, or Security → SMS) so it doesn't block OTPs for other SMS apps.",
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpStep(n: Int, text: String) {
    val p = LocalPalette.current
    Row(Modifier.padding(vertical = 5.dp)) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).background(p.accentA),
            contentAlignment = Alignment.Center,
        ) { Text("$n", style = LType.micro.copy(color = Color.White, fontWeight = FontWeight.Bold)) }
        Spacer(Modifier.width(10.dp))
        Text(text, style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal))
    }
}

private data class BgSample(val mode: ThemeMode, val surface: LumenSurface?)

/** Visual tiles for each background, with a springy selection ring. */
@Composable
private fun BackgroundPicker(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val p = LocalPalette.current
    val systemDark = isSystemInDarkTheme()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(ThemeMode.values().toList(), key = { it.name }) { mode ->
            val surface = surfaceFor(mode, systemDark)
            val sp = remember(surface) { buildPalette(surface, p.accentA) }
            val active = mode == selected
            val ring by animateDpAsState(if (active) 2.5.dp else 1.dp, spring(0.5f, 500f), label = "ring")
            val scale by animateFloatAsState(if (active) 1.04f else 1f, spring(0.5f, 500f), label = "bgScale")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(width = 62.dp, height = 96.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(16.dp))
                        .border(ring, if (active) p.accentA else p.glassEdgeLight, RoundedCornerShape(16.dp))
                        .clickable { onSelect(mode) }
                ) {
                    if (sp.texture != 0) {
                        Image(
                            painterResource(sp.texture), null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(0f to sp.bgTop, 0.45f to sp.bgMid, 1f to sp.bg)
                            )
                        )
                    }
                    // Mini chat preview inside the tile
                    Column(Modifier.padding(8.dp).align(Alignment.BottomCenter), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(width = 30.dp, height = 9.dp).clip(CircleShape).background(sp.received))
                        Box(
                            Modifier.align(Alignment.End).size(width = 26.dp, height = 9.dp).clip(CircleShape)
                                .background(if (sp.dark) Color.Black.copy(alpha = 0.7f) else sp.ink)
                        )
                    }
                    if (mode == ThemeMode.System) {
                        Text(
                            "A", style = LType.caption.copy(color = sp.text, fontWeight = FontWeight.Bold),
                            modifier = Modifier.align(Alignment.TopStart).padding(7.dp),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(mode.label, style = LType.micro.copy(color = if (active) p.text else p.textDim))
            }
        }
    }
}

@Composable
private fun FontPicker(selected: AppFont, onSelect: (AppFont) -> Unit) {
    val p = LocalPalette.current
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(AppFont.values().toList(), key = { it.name }) { f ->
            val family = remember(f) { fontFamilyFor(f) }
            val active = f == selected
            val bg by animateColorAsState(if (active) p.accentA else p.glassTint, label = "fontBg")
            Column(
                Modifier
                    .size(width = 74.dp, height = 70.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg)
                    .border(1.dp, if (active) Color.Transparent else p.glassEdgeLight, RoundedCornerShape(16.dp))
                    .clickable { onSelect(f) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Aa",
                    style = TextStyle(fontFamily = family, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                        color = if (active) Color.White else p.text),
                )
                Text(
                    f.label,
                    style = TextStyle(fontFamily = family, fontSize = 11.sp,
                        color = if (active) Color.White.copy(alpha = 0.85f) else p.textDim),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun AvatarPicker(selected: AvatarStyle, onSelect: (AvatarStyle) -> Unit) {
    val p = LocalPalette.current
    val look = LocalLook.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        AvatarStyle.values().forEach { style ->
            val active = style == selected
            val scale by animateFloatAsState(if (active) 1.08f else 1f, spring(0.45f, 500f), label = "av")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable { onSelect(style) }.padding(4.dp),
            ) {
                CompositionLocalProvider(LocalLook provides look.copy(avatarStyle = style)) {
                    Box(
                        Modifier
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .border(if (active) 2.dp else 0.dp, if (active) p.accentA else Color.Transparent, CircleShape)
                            .padding(3.dp)
                    ) {
                        Avatar(name = "Sample Name", address = "+965 5555 ${style.ordinal}21", photoUri = null, size = 46.dp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(style.label, style = LType.micro.copy(color = if (active) p.text else p.textDim))
            }
        }
    }
}
