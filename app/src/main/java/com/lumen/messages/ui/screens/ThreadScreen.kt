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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.lumen.messages.data.Message
import com.lumen.messages.data.SimInfo
import com.lumen.messages.notify.Foreground
import com.lumen.messages.ui.AppViewModel
import com.lumen.messages.ui.Fmt
import com.lumen.messages.ui.components.Avatar
import com.lumen.messages.ui.components.GlassIconButton
import com.lumen.messages.ui.components.pressScale
import com.lumen.messages.ui.findOtp
import com.lumen.messages.ui.glass.BackdropState
import com.lumen.messages.ui.glass.GlassSurface
import com.lumen.messages.ui.glass.backdropSource
import com.lumen.messages.ui.glass.rememberBackdropState
import com.lumen.messages.ui.linkify
import com.lumen.messages.ui.theme.LType
import com.lumen.messages.ui.theme.LocalLook
import com.lumen.messages.ui.theme.LocalPalette
import com.lumen.messages.ui.ChatRow
import com.lumen.messages.ui.DayRow
import com.lumen.messages.ui.MsgRow
import com.lumen.messages.data.BubbleShape
import com.lumen.messages.data.BubbleStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

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
    // Keeps the last message shown in the sheet so its exit animation still has content.
    val sheetMemo = remember { SheetMemo() }
    sheetFor?.let { sheetMemo.message = it }

    LifecycleResumeEffect(threadId) {
        Foreground.visibleThread = threadId
        onPauseOrDispose { Foreground.visibleThread = -1L }
    }

    val messages = vm.messages
    val rows = vm.rows
    val look = LocalLook.current

    // Messages already present when the thread opened don't animate; new ones pop in.
    val seen = remember(threadId) { SeenIds() }
    if (seen.ids == null && vm.messagesLoaded) seen.ids = messages.mapTo(HashSet()) { it.id }

    val newestId = messages.lastOrNull()?.id
    LaunchedEffect(newestId) {
        if (newestId != null && listState.firstVisibleItemIndex <= 3) listState.animateScrollToItem(0)
    }
    val showJump by remember { derivedStateOf { listState.firstVisibleItemIndex > 4 } }

    // Load older messages when the user scrolls near the top of what's loaded.
    val nearOldest by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 12
        }
    }
    LaunchedEffect(nearOldest, vm.hasOlder, rows.size) {
        if (nearOldest && vm.hasOlder) vm.loadOlder()
    }

    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val maxBubble = (LocalConfiguration.current.screenWidthDp * 0.78f).dp

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier
                .fillMaxSize()
                .backdropSource(backdrop),
            contentPadding = PaddingValues(
                top = with(density) { topHeightPx.toDp() } + 8.dp,
                bottom = with(density) { composerHeightPx.toDp() } + 10.dp,
                start = 12.dp,
                end = 12.dp,
            ),
        ) {
            items(rows, key = { it.key }, contentType = { if (it is DayRow) 0 else 1 }) { row ->
                when (row) {
                    is DayRow -> DayPill(Fmt.dayLabel(row.day), Modifier.animateItem())
                    is MsgRow -> {
                        val animateIn = remember(row.msg.id) { seen.ids?.add(row.msg.id) == true && !look.reduceMotion }
                        MessageBubble(
                            row = row,
                            animateIn = animateIn,
                            maxWidth = maxBubble,
                            screenHeightPx = screenHeightPx,
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
            Box(Modifier.align(Alignment.Center)) {
                Text(
                    "Say hello 👋",
                    style = LType.title.copy(color = p.textDim),
                    modifier = Modifier.padding(bottom = 60.dp),
                )
            }
        }

        // ---------------------------------------------------------- glass top bar
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { topHeightPx = it.height },
            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            backdrop = backdrop,
            tint = p.barTint,
        ) {
            Row(
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = { vm.back() }, size = 42.dp)
                Spacer(Modifier.width(10.dp))
                if (header != null) {
                    with(sharedScope) {
                        Avatar(
                            name = header.name,
                            address = header.address,
                            photoUri = header.photoUri,
                            size = 42.dp,
                            modifier = Modifier.sharedElement(
                                rememberSharedContentState(key = "avatar-${header.threadId}"),
                                animatedVisibilityScope = animScope,
                            ),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            header.title,
                            style = LType.name.copy(color = p.text, fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            if (header.name != null) header.address else "Text message",
                            style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal),
                            maxLines = 1,
                        )
                    }
                    if (header.address.any { it.isDigit() }) {
                        GlassIconButton(
                            Icons.Rounded.Call, "Call",
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(header.address))))
                                }
                            },
                            size = 42.dp,
                        )
                    }
                } else Spacer(Modifier.weight(1f))
            }
        }

        // ---------------------------------------------------------- jump to latest
        AnimatedVisibility(
            visible = showJump,
            enter = scaleIn(spring(0.55f, 500f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = with(density) { composerHeightPx.toDp() } + 14.dp),
        ) {
            GlassIconButton(
                Icons.Rounded.KeyboardArrowDown, "Latest",
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                backdrop = backdrop,
                glassTint = p.barTint,
            )
        }

        // ---------------------------------------------------------- composer
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
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { composerHeightPx = it.height },
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
private fun DayPill(label: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
        GlassSurface(shape = RoundedCornerShape(14.dp)) {
            Text(
                label,
                style = LType.micro.copy(color = p.textDim),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

private enum class Delivery { None, Pending, Sent, Delivered, Failed }

@Composable
private fun MessageBubble(
    row: MsgRow,
    animateIn: Boolean,
    maxWidth: androidx.compose.ui.unit.Dp,
    screenHeightPx: Float,
    onLongPress: () -> Unit,
    onRetry: () -> Unit,
    onCopyCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val look = LocalLook.current
    val m = row.msg
    val mine = m.isMine
    val big = if (look.bubbleShape == BubbleShape.Round) 22.dp else 12.dp
    val small = if (look.bubbleShape == BubbleShape.Round) 6.dp else 4.dp
    // Glass style draws sent bubbles as tinted glass with normal text colour.
    val glassMine = mine && look.bubbleStyle == BubbleStyle.Glass && !m.isFailed
    val onBubble = if (mine && !glassMine) p.onAccent else p.text
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

    val appear = remember(m.id) { Animatable(if (animateIn) 0f else 1f) }
    LaunchedEffect(m.id) {
        if (appear.value < 1f) appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 320f))
    }
    var yInWindow by remember { mutableFloatStateOf(0f) }
    val text = remember(m.body, onBubble) { linkify(m.body, if (mine && !glassMine) Color.White else p.accentA) }
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier
            .fillMaxWidth()
            .padding(top = if (row.groupTop) 8.dp else 2.dp),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Box(
            Modifier
                .widthIn(max = maxWidth)
                .graphicsLayer {
                    val v = appear.value
                    alpha = v.coerceIn(0f, 1f)
                    scaleX = 0.5f + 0.5f * v
                    scaleY = 0.5f + 0.5f * v
                    translationY = (1f - v) * 36.dp.toPx()
                    transformOrigin = TransformOrigin(if (mine) 1f else 0f, 1f)
                }
                .pressScale(interaction, 0.97f)
                .then(
                    // Only the screen-pinned gradient needs the bubble's position each frame.
                    if (mine && look.bubbleStyle == BubbleStyle.Gradient) {
                        Modifier.onGloballyPositioned { yInWindow = it.positionInWindow().y }
                    } else Modifier
                )
                .clip(shape)
                .then(
                    if (glassMine) {
                        Modifier
                            .background(p.accentA.copy(alpha = if (p.dark) 0.26f else 0.16f))
                            .border(1.dp, Brush.linearGradient(listOf(p.accentB.copy(alpha = 0.6f), p.glassEdgeDark)), shape)
                    } else if (mine && look.bubbleStyle == BubbleStyle.Solid) {
                        Modifier.background(if (m.isFailed) p.danger else p.accentA)
                    } else if (mine) {
                        Modifier.drawBehind {
                            // Gradient pinned to the screen, so bubbles shift colour as they scroll.
                            val colors = if (m.isFailed) listOf(p.danger, p.danger.copy(alpha = 0.8f))
                            else listOf(p.accentB, p.accentA, p.accentC)
                            drawRect(
                                Brush.verticalGradient(colors, startY = -yInWindow, endY = screenHeightPx - yInWindow)
                            )
                        }
                    } else {
                        Modifier
                            .background(p.received)
                            .border(
                                1.dp,
                                Brush.linearGradient(listOf(p.glassEdgeLight, p.glassEdgeDark)),
                                shape,
                            )
                    }
                )
                .combinedClickable(
                    interactionSource = interaction,
                    indication = ripple(color = onBubble),
                    onLongClick = onLongPress,
                    onClick = { if (m.isFailed) onRetry() },
                )
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Text(text, style = LType.body.copy(color = onBubble))
        }

        if (row.otp != null) {
            OtpChip(row.otp, onCopyCode, Modifier.padding(top = 6.dp))
        }

        if (row.groupBottom) {
            val delivery = when {
                !mine -> Delivery.None
                m.isFailed -> Delivery.Failed
                m.isPending -> Delivery.Pending
                m.isDelivered -> Delivery.Delivered
                else -> Delivery.Sent
            }
            Row(
                Modifier.padding(top = 4.dp, start = 6.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (delivery == Delivery.Failed) "Not sent · Tap to retry" else Fmt.time(context, m.date),
                    style = LType.micro.copy(color = if (delivery == Delivery.Failed) p.danger else p.textFaint),
                )
                if (delivery != Delivery.None) {
                    Spacer(Modifier.width(5.dp))
                    AnimatedContent(
                        targetState = delivery,
                        transitionSpec = {
                            (scaleIn(spring(0.45f, 600f), initialScale = 0.3f) + fadeIn())
                                .togetherWith(scaleOut(targetScale = 0.3f) + fadeOut())
                        },
                        label = "delivery",
                    ) { d ->
                        when (d) {
                            Delivery.Pending -> CircularProgressIndicator(
                                modifier = Modifier.size(11.dp),
                                strokeWidth = 1.5.dp,
                                color = p.textFaint,
                            )
                            Delivery.Sent -> Icon(Icons.Rounded.Done, "Sent", tint = p.textFaint, modifier = Modifier.size(15.dp))
                            Delivery.Delivered -> Icon(Icons.Rounded.DoneAll, "Delivered", tint = p.accentB, modifier = Modifier.size(15.dp))
                            Delivery.Failed -> Icon(Icons.Rounded.ErrorOutline, "Failed", tint = p.danger, modifier = Modifier.size(15.dp))
                            Delivery.None -> Spacer(Modifier.size(1.dp))
                        }
                    }
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
    GlassSurface(
        modifier = modifier.pressScale(interaction, 0.92f),
        shape = RoundedCornerShape(16.dp),
        tint = p.accentA.copy(alpha = 0.18f),
    ) {
        Row(
            Modifier
                .clickable(interactionSource = interaction, indication = ripple(color = p.accentA)) {
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
                        tint = if (done) p.success else p.accentB,
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

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        backdrop = backdrop,
        tint = p.barTint,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)
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
                        style = LType.micro.copy(color = if (s[0] > 1) p.accentB else p.textFaint),
                        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                if (sims.size > 1) {
                    SimChip(sims, selectedSubId, onCycleSim)
                    Spacer(Modifier.width(8.dp))
                }
                GlassSurface(
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (text.isEmpty()) {
                            Text("Text message", style = LType.body.copy(color = p.textFaint))
                        }
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
                Spacer(Modifier.width(8.dp))
                SendButton(enabled = canSend, onSend = onSend)
            }
        }
    }
}

@Composable
private fun SendButton(enabled: Boolean, onSend: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val fly = remember { Animatable(0f) }
    val interaction = remember { MutableInteractionSource() }
    val active by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "send",
    )
    Box(
        Modifier
            .size(48.dp)
            .graphicsLayer {
                val s = 0.82f + 0.18f * active
                scaleX = s; scaleY = s
            }
            .pressScale(interaction, 0.86f)
            .clip(CircleShape)
            .background(
                if (enabled) Brush.linearGradient(listOf(p.accentC, p.accentA, p.accentB))
                else SolidColor(p.glassTint)
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Color.White),
                enabled = enabled,
            ) {
                onSend()
                scope.launch {
                    // Paper-plane launch: shoot out to the top-right, re-enter from below-left.
                    fly.snapTo(0f)
                    fly.animateTo(1f, tween(180))
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
                .size(22.dp)
                .graphicsLayer {
                    val f = fly.value
                    translationX = f * 34.dp.toPx()
                    translationY = -f * 34.dp.toPx()
                    rotationZ = -24f * active
                    alpha = 1f - kotlin.math.abs(f).coerceIn(0f, 1f) * 0.9f
                },
        )
    }
}

@Composable
private fun SimChip(sims: List<SimInfo>, selectedSubId: Int, onCycle: () -> Unit) {
    val p = LocalPalette.current
    val sim = sims.firstOrNull { it.subId == selectedSubId } ?: sims.first()
    val interaction = remember { MutableInteractionSource() }
    GlassSurface(
        modifier = Modifier.size(48.dp).pressScale(interaction, 0.88f),
        shape = CircleShape,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = interaction, indication = ripple(color = p.text), onClick = onCycle),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = sim.slot,
                transitionSpec = {
                    (slideInVertically(spring(0.6f, 500f)) { it } + fadeIn())
                        .togetherWith(slideOutVertically { -it } + fadeOut())
                },
                label = "sim",
            ) { slot ->
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.SimCard, sims.firstOrNull { it.slot == slot }?.label,
                        tint = Color(sims.firstOrNull { it.slot == slot }?.color ?: 0xFF7C5CFF.toInt()).copy(alpha = 1f),
                        modifier = Modifier.size(26.dp),
                    )
                    Text(
                        "${slot + 1}",
                        style = LType.micro.copy(color = Color.White, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
