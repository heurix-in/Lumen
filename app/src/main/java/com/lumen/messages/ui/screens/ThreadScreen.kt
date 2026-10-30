@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)

package com.lumen.messages.ui.screens

import android.content.Intent
import android.net.Uri
import android.telephony.SmsMessage
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.lumen.messages.data.BubbleShape
import com.lumen.messages.data.BubbleStyle
import com.lumen.messages.data.Message
import com.lumen.messages.data.SimInfo
import com.lumen.messages.notify.Foreground
import com.lumen.messages.ui.AppViewModel
import com.lumen.messages.ui.DayRow
import com.lumen.messages.ui.Fmt
import com.lumen.messages.ui.MsgRow
import com.lumen.messages.ui.TimeRow
import com.lumen.messages.ui.components.Avatar
import com.lumen.messages.ui.components.GlassIconButton
import com.lumen.messages.ui.components.pressScale
import com.lumen.messages.ui.glass.BackdropState
import com.lumen.messages.ui.glass.GlassSurface
import com.lumen.messages.ui.glass.backdropSource
import com.lumen.messages.ui.glass.rememberBackdropState
import com.lumen.messages.ui.linkify
import com.lumen.messages.ui.theme.LType
import com.lumen.messages.ui.theme.LocalLook
import com.lumen.messages.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class SheetMemo {
    var message: Message? = null
}

private class SeenIds {
    var ids: HashSet<Long>? = null
}

@Composable
fun ThreadScreen(
    vm: AppViewModel,
    threadId: Long,
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope,
) {
    val p = LocalPalette.current
    val look = LocalLook.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val backdrop = rememberBackdropState()
    val listState = rememberLazyListState()
    val header = vm.header

    var topHeightPx by remember { mutableIntStateOf(0) }
    var composerHeightPx by remember { mutableIntStateOf(0) }
    var sheetFor by remember { mutableStateOf<Message?>(null) }
    val sheetMemo = remember { SheetMemo() }
    sheetFor?.let { sheetMemo.message = it }

    LifecycleResumeEffect(threadId) {
        Foreground.visibleThread = threadId
        onPauseOrDispose { Foreground.visibleThread = -1L }
    }

    val messages = vm.messages
    val rows = vm.rows

    // Messages already present when the thread opened don't animate; new ones pop in.
    val seen = remember(threadId) { SeenIds() }
    if (seen.ids == null && vm.messagesLoaded) seen.ids = messages.mapTo(HashSet()) { it.id }

    val newestId = messages.lastOrNull()?.id
    LaunchedEffect(newestId) {
        if (newestId != null && listState.firstVisibleItemIndex <= 3) listState.animateScrollToItem(0)
    }
    val showJump by remember { derivedStateOf { listState.firstVisibleItemIndex > 4 } }
    val nearOldest by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 12
        }
    }
    LaunchedEffect(nearOldest, vm.hasOlder, rows.size) {
        if (nearOldest && vm.hasOlder) vm.loadOlder()
    }

    val maxBubble = (LocalConfiguration.current.screenWidthDp * 0.76f).dp
    val lastMineId = remember(messages) { messages.lastOrNull { it.isMine }?.id }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize().backdropSource(backdrop),
            contentPadding = PaddingValues(
                top = with(density) { topHeightPx.toDp() } + 8.dp,
                bottom = with(density) { composerHeightPx.toDp() } + 8.dp,
                start = 14.dp,
                end = 14.dp,
            ),
        ) {
            items(rows, key = { it.key }, contentType = { r -> if (r is MsgRow) 1 else 0 }) { row ->
                when (row) {
                    is DayRow -> CenterLabel(Fmt.dayLabel(row.day), pill = true, modifier = Modifier.animateItem())
                    is TimeRow -> CenterLabel(Fmt.time(context, row.time), pill = false, modifier = Modifier.animateItem())
                    is MsgRow -> {
                        val animateIn = remember(row.msg.id) { seen.ids?.add(row.msg.id) == true && !look.reduceMotion }
                        MessageBubble(
                            row = row,
                            animateIn = animateIn,
                            maxWidth = maxBubble,
                            showStatus = row.msg.id == lastMineId || row.msg.isFailed,
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                sheetFor = row.msg
                            },
                            onRetry = { vm.retry(row.msg) },
                            onCopyCode = { code ->
                                clipboard.setText(AnnotatedString(code))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            modifier = Modifier.animateItem(
                                fadeInSpec = null,
                                placementSpec = spring(dampingRatio = 0.85f, stiffness = 500f),
                                fadeOutSpec = tween(160),
                            ),
                        )
                    }
                }
            }
        }

        if (vm.messagesLoaded && messages.isEmpty()) {
            Text(
                "Say hello 👋",
                style = LType.title.copy(color = p.textDim),
                modifier = Modifier.align(Alignment.Center).padding(bottom = 60.dp),
            )
        }

        // ---------------------------------------------------------- glass top bar (borderless, like the reference)
        GlassSurface(
            modifier = Modifier.fillMaxWidth().onSizeChanged { topHeightPx = it.height },
            shape = RoundedCornerShape(0.dp),
            backdrop = backdrop,
            tint = p.barTint,
            border = false,
            sheen = false,
        ) {
            Row(
                Modifier.statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).clickable { vm.back() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = p.text, modifier = Modifier.size(22.dp)) }
                if (header != null) {
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { vm.openContact() }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        with(sharedScope) {
                            Avatar(
                                name = header.name,
                                address = header.address,
                                photoUri = header.photoUri,
                                size = 38.dp,
                                modifier = Modifier.sharedElement(
                                    rememberSharedContentState(key = "avatar-${header.threadId}"),
                                    animatedVisibilityScope = animScope,
                                ),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                header.title,
                                style = LType.name.copy(color = p.text, fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (header.name != null) header.address else "Tap for info",
                                style = LType.micro.copy(color = p.textDim, fontWeight = FontWeight.Normal),
                                maxLines = 1,
                            )
                        }
                    }
                    if (header.address.any { it.isDigit() }) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).clickable {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(header.address))))
                                }
                            },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Call, "Call", tint = p.text, modifier = Modifier.size(21.dp)) }
                    }
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).clickable { vm.openContact() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.MoreVert, "Details", tint = p.text, modifier = Modifier.size(21.dp)) }
                } else Spacer(Modifier.weight(1f))
            }
        }

        // ---------------------------------------------------------- jump to latest
        AnimatedVisibility(
            visible = showJump,
            enter = scaleIn(spring(0.55f, 500f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = with(density) { composerHeightPx.toDp() } + 10.dp),
        ) {
            GlassIconButton(
                Icons.Rounded.KeyboardArrowDown, "Latest",
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                backdrop = backdrop,
                glassTint = p.barTint,
                size = 40.dp,
            )
        }

        // ---------------------------------------------------------- floating composer
        Composer(
            text = vm.draft,
            onText = { vm.draft = it },
            onSend = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                vm.send()
            },
            sims = vm.sims,
            selectedSubId = vm.selectedSubId,
            onCycleSim = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                vm.cycleSim()
            },
            backdrop = backdrop,
            modifier = Modifier.align(Alignment.BottomCenter).onSizeChanged { composerHeightPx = it.height },
        )

        // ---------------------------------------------------------- long-press sheet
        val target = sheetMemo.message
        GlassActionSheet(
            visible = sheetFor != null,
            backdrop = backdrop,
            preview = target?.body,
            actions = buildList {
                if (target != null) {
                    add(SheetAction("Copy text", Icons.Rounded.ContentCopy) {
                        clipboard.setText(AnnotatedString(target.body))
                        sheetFor = null
                    })
                    if (target.isFailed) add(SheetAction("Try again", Icons.Rounded.Refresh) {
                        vm.retry(target)
                        sheetFor = null
                    })
                    add(SheetAction("Delete message", Icons.Rounded.Delete, danger = true) {
                        vm.deleteMessage(target)
                        sheetFor = null
                    })
                }
            },
            onDismiss = { sheetFor = null },
        )
    }
}

@Composable
private fun CenterLabel(label: String, pill: Boolean, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        if (pill) {
            GlassSurface(shape = RoundedCornerShape(12.dp), border = false) {
                Text(
                    label,
                    style = LType.micro.copy(color = p.textDim),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        } else {
            Text(label, style = LType.micro.copy(color = p.textFaint))
        }
    }
}

private enum class Delivery { Pending, Sent, Delivered, Failed }

@Composable
private fun MessageBubble(
    row: MsgRow,
    animateIn: Boolean,
    maxWidth: Dp,
    showStatus: Boolean,
    onLongPress: () -> Unit,
    onRetry: () -> Unit,
    onCopyCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val look = LocalLook.current
    val m = row.msg
    val mine = m.isMine
    val round = look.bubbleShape == BubbleShape.Round
    val big = if (round) 20.dp else 12.dp
    val small = if (round) 6.dp else 4.dp
    val shape = if (mine) {
        RoundedCornerShape(
            topStart = big, topEnd = if (row.groupTop) big else small,
            bottomEnd = if (row.groupBottom) big else small, bottomStart = big,
        )
    } else {
        RoundedCornerShape(
            topStart = if (row.groupTop) big else small, topEnd = big,
            bottomEnd = big, bottomStart = if (row.groupBottom) big else small,
        )
    }

    // Colours per style. Ink: near-black pill (reference). Colour: accent. Glass: tinted glass.
    val fill: Color
    val textColor: Color
    val edge: Color?
    when {
        mine && m.isFailed -> { fill = p.danger; textColor = Color.White; edge = null }
        mine && look.bubbleStyle == BubbleStyle.Ink -> { fill = p.ink; textColor = p.onInk; edge = p.glassEdgeDark }
        mine && look.bubbleStyle == BubbleStyle.Accent -> { fill = p.accentA; textColor = p.onAccent; edge = null }
        mine -> { fill = p.accentA.copy(alpha = if (p.dark) 0.28f else 0.16f); textColor = p.text; edge = p.accentB.copy(alpha = 0.5f) }
        else -> { fill = p.received; textColor = p.text; edge = p.glassEdgeDark }
    }

    val appear = remember(m.id) { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(m.id) {
        if (appear.value < 1f) appear.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 340f))
    }
    val linkColor = if (mine && look.bubbleStyle == BubbleStyle.Accent) Color.White else p.accentB
    val text = remember(m.body, linkColor) { linkify(m.body, linkColor) }
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier.fillMaxWidth().padding(top = if (row.groupTop) 8.dp else 2.dp),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Box(
            Modifier
                .widthIn(max = maxWidth)
                .graphicsLayer {
                    val v = appear.value
                    alpha = v.coerceIn(0f, 1f)
                    scaleX = 0.6f + 0.4f * v
                    scaleY = 0.6f + 0.4f * v
                    translationY = (1f - v) * 30.dp.toPx()
                    transformOrigin = TransformOrigin(if (mine) 1f else 0f, 1f)
                }
                .pressScale(interaction, 0.97f)
                .clip(shape)
                .background(fill)
                .then(if (edge != null) Modifier.border(1.dp, edge, shape) else Modifier)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = ripple(color = textColor),
                    onLongClick = onLongPress,
                    onClick = { if (m.isFailed) onRetry() },
                )
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Text(text, style = LType.body.copy(color = textColor))
        }

        if (row.otp != null) {
            OtpChip(row.otp, onCopyCode, Modifier.padding(top = 6.dp))
        }

        if (mine && showStatus && row.groupBottom) {
            val delivery = when {
                m.isFailed -> Delivery.Failed
                m.isPending -> Delivery.Pending
                m.isDelivered -> Delivery.Delivered
                else -> Delivery.Sent
            }
            AnimatedContent(
                targetState = delivery,
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically { it / 2 }).togetherWith(fadeOut(tween(120)))
                },
                label = "delivery",
                modifier = Modifier.padding(top = 4.dp, end = 4.dp),
            ) { d ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (d == Delivery.Pending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = p.textFaint,
                        )
                        Spacer(Modifier.width(5.dp))
                    }
                    if (d == Delivery.Failed) {
                        Icon(Icons.Rounded.ErrorOutline, null, tint = p.danger, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        when (d) {
                            Delivery.Pending -> "Sending"
                            Delivery.Sent -> "Sent"
                            Delivery.Delivered -> "Delivered"
                            Delivery.Failed -> "Not sent · tap to retry"
                        },
                        style = LType.micro.copy(color = if (d == Delivery.Failed) p.danger else p.textFaint),
                    )
                }
            }
        }
    }
}

@Composable
private fun OtpChip(code: String, onCopy: (String) -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }
    val interaction = remember { MutableInteractionSource() }
    GlassSurface(modifier = modifier.pressScale(interaction, 0.92f), shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier
                .clickable(interactionSource = interaction, indication = ripple(color = p.text)) {
                    onCopy(code)
                    copied = true
                }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(
                targetState = copied,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                },
                label = "otp",
            ) { done ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (done) Icons.Rounded.Check else Icons.Rounded.ContentCopy, null,
                        tint = if (done) p.success else p.accentA,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (done) "Copied" else "Copy code  $code",
                        style = LType.caption.copy(color = p.text, fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}

@Composable
private fun Composer(
    text: String,
    onText: (String) -> Unit,
    onSend: () -> Unit,
    sims: List<SimInfo>,
    selectedSubId: Int,
    onCycleSim: () -> Unit,
    backdrop: BackdropState,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val canSend = text.isNotBlank()
    val segments = remember(text) {
        if (text.length < 120) null else runCatching { SmsMessage.calculateLength(text, false) }.getOrNull()
    }
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp, top = 6.dp)
    ) {
        AnimatedVisibility(
            visible = segments != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            val s = segments
            if (s != null) {
                Text(
                    "${s[2]} characters left · ${s[0]} SMS",
                    style = LType.micro.copy(color = if (s[0] > 1) p.accentA else p.textFaint),
                    modifier = Modifier.padding(start = 18.dp, bottom = 6.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            GlassSurface(
                modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                shape = RoundedCornerShape(25.dp),
                backdrop = backdrop,
                tint = p.barTint,
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(start = 6.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (sims.size > 1) {
                        SimChip(sims, selectedSubId, onCycleSim)
                    } else {
                        Spacer(Modifier.width(10.dp))
                    }
                    Box(Modifier.weight(1f).padding(vertical = 13.dp), contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) Text("Type here", style = LType.body.copy(color = p.textFaint))
                        BasicTextField(
                            value = text,
                            onValueChange = onText,
                            textStyle = LType.body.copy(color = p.text),
                            cursorBrush = SolidColor(p.accentA),
                            maxLines = 6,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            SendButton(enabled = canSend, onSend = onSend, backdrop = backdrop)
        }
    }
}

@Composable
private fun SendButton(enabled: Boolean, onSend: () -> Unit, backdrop: BackdropState) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val fly = remember { Animatable(0f) }
    val interaction = remember { MutableInteractionSource() }
    val active by animateFloatAsState(if (enabled) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 420f), label = "send")
    val fill by animateColorAsState(if (enabled) p.accentA else p.barTint, label = "sendFill")
    GlassSurface(
        modifier = Modifier
            .size(50.dp)
            .graphicsLayer {
                val s = 0.9f + 0.1f * active
                scaleX = s; scaleY = s
            }
            .pressScale(interaction, 0.86f),
        shape = CircleShape,
        backdrop = backdrop,
        tint = fill,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = interaction, indication = ripple(color = Color.White), enabled = enabled) {
                    onSend()
                    scope.launch {
                        fly.snapTo(0f)
                        fly.animateTo(1f, tween(170))
                        fly.snapTo(-1f)
                        fly.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 500f))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.Send,
                "Send",
                tint = if (enabled) Color.White else p.textFaint,
                modifier = Modifier
                    .size(21.dp)
                    .graphicsLayer {
                        val f = fly.value
                        translationX = f * 30.dp.toPx()
                        translationY = -f * 30.dp.toPx()
                        rotationZ = -20f * active
                        alpha = 1f - kotlin.math.abs(f).coerceIn(0f, 1f) * 0.9f
                    },
            )
        }
    }
}

@Composable
private fun SimChip(sims: List<SimInfo>, selectedSubId: Int, onCycle: () -> Unit) {
    val p = LocalPalette.current
    val sim = sims.firstOrNull { it.subId == selectedSubId } ?: sims.first()
    Box(
        Modifier.size(38.dp).clip(CircleShape).clickable(onClick = onCycle),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = sim.slot,
            transitionSpec = {
                (slideInVertically(spring(0.6f, 500f)) { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
            },
            label = "sim",
        ) { slot ->
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SimCard, "SIM ${slot + 1}", tint = p.textDim, modifier = Modifier.size(24.dp))
                Text(
                    "${slot + 1}",
                    style = LType.micro.copy(color = p.bg, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
