@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)

package com.lumen.messages.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sms
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lumen.messages.data.Conversation
import com.lumen.messages.data.SearchHit
import com.lumen.messages.ui.AppViewModel
import com.lumen.messages.ui.Fmt
import com.lumen.messages.ui.components.Avatar
import com.lumen.messages.ui.components.CountBadge
import com.lumen.messages.ui.components.GlassIconButton
import com.lumen.messages.ui.components.pressScale
import com.lumen.messages.ui.excerpt
import com.lumen.messages.ui.glass.GlassSurface
import com.lumen.messages.ui.glass.backdropSource
import com.lumen.messages.ui.glass.rememberBackdropState
import com.lumen.messages.ui.highlight
import com.lumen.messages.ui.theme.LType
import com.lumen.messages.ui.theme.LocalPalette
import kotlinx.coroutines.delay

private enum class HeaderMode { Normal, Search, Select }

@Composable
fun ConversationListScreen(
    vm: AppViewModel,
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope,
    onRequestDefault: () -> Unit,
) {
    val p = LocalPalette.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val backdrop = rememberBackdropState()
    val listState = rememberLazyListState()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var searchOpen by remember { mutableStateOf(vm.query.isNotEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }

    val mode = when {
        vm.selection.isNotEmpty() -> HeaderMode.Select
        searchOpen -> HeaderMode.Search
        else -> HeaderMode.Normal
    }

    BackHandler(enabled = mode != HeaderMode.Normal) {
        if (mode == HeaderMode.Select) vm.clearSelection() else {
            vm.onQuery("")
            searchOpen = false
        }
    }

    val visible = remember(vm.conversations, vm.query, vm.unreadOnly) {
        vm.conversations.filter { c ->
            (!vm.unreadOnly || c.unread > 0) &&
                (vm.query.isBlank() ||
                    c.title.contains(vm.query, true) ||
                    c.address.contains(vm.query, true))
        }
    }
    val hits = if (vm.query.isNotBlank()) vm.searchHits else emptyList()

    // Entrance stagger only on the first appearance of the session.
    val playIntro = remember { !vm.listIntroPlayed }
    LaunchedEffect(vm.conversationsLoaded) {
        if (vm.conversationsLoaded) {
            delay(900)
            vm.listIntroPlayed = true
        }
    }

    val collapse by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 140f).coerceIn(0f, 1f)
        }
    }
    val fabExpanded by remember { derivedStateOf { !listState.canScrollBackward } }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .backdropSource(backdrop),
            contentPadding = PaddingValues(
                top = with(density) { headerHeightPx.toDp() } + 10.dp,
                bottom = navBottom + 110.dp,
                start = 14.dp,
                end = 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!vm.isDefaultSms && vm.query.isBlank()) {
                item(key = "banner") {
                    DefaultAppBanner(onRequestDefault, Modifier.animateItem())
                }
            }
            itemsIndexed(visible, key = { _, c -> c.threadId }, contentType = { _, _ -> "conv" }) { index, conv ->
                ConversationCard(
                    conv = conv,
                    query = vm.query,
                    selected = conv.threadId in vm.selection,
                    selectionMode = vm.selection.isNotEmpty(),
                    entranceDelayIndex = if (playIntro && !vm.listIntroPlayed) index else -1,
                    sharedScope = sharedScope,
                    animScope = animScope,
                    onClick = {
                        if (vm.selection.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            vm.toggleSelect(conv.threadId)
                        } else vm.openConversation(conv)
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.toggleSelect(conv.threadId)
                    },
                    modifier = Modifier.animateItem(
                        fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        placementSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                        fadeOutSpec = tween(180),
                    ),
                )
            }
            if (hits.isNotEmpty()) {
                item(key = "hits-header") {
                    Text(
                        "In messages",
                        style = LType.caption.copy(color = p.textDim),
                        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 2.dp).animateItem(),
                    )
                }
                items(hits, key = { "hit-${it.messageId}" }) { hit ->
                    SearchHitCard(hit, vm.query, onClick = { vm.openThreadById(hit.threadId) }, modifier = Modifier.animateItem())
                }
            }
        }

        val empty = vm.conversationsLoaded && visible.isEmpty() && hits.isEmpty()
        AnimatedVisibility(
            visible = empty,
            enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            EmptyState(
                title = when {
                    vm.query.isNotBlank() -> "No matches"
                    vm.unreadOnly -> "All caught up"
                    else -> "No conversations yet"
                },
                subtitle = when {
                    vm.query.isNotBlank() -> "Nothing found for “${vm.query}”"
                    vm.unreadOnly -> "You've read everything."
                    else -> "Tap New chat to send your first message."
                },
            )
        }

        // ---------------------------------------------------------- glass header
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { headerHeightPx = it.height },
            shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp),
            backdrop = backdrop,
            tint = p.barTint.copy(alpha = p.barTint.alpha * (0.55f + 0.45f * collapse)),
        ) {
            Column(
                Modifier
                    .statusBarsPadding()
                    .padding(start = 18.dp, end = 14.dp, top = 6.dp, bottom = 12.dp)
            ) {
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        (fadeIn(tween(220, delayMillis = 60)) + slideInVertically(spring(0.8f, 500f)) { it / 3 })
                            .togetherWith(fadeOut(tween(120)) + slideOutVertically(tween(160)) { -it / 3 })
                    },
                    label = "header",
                ) { m ->
                    Box(Modifier.fillMaxWidth().height(58.dp), contentAlignment = Alignment.CenterStart) {
                        when (m) {
                            HeaderMode.Normal -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Messages",
                                    style = LType.hero.copy(color = p.text),
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer {
                                            val s = 1f - 0.22f * collapse
                                            scaleX = s; scaleY = s
                                            transformOrigin = TransformOrigin(0f, 0.5f)
                                        },
                                )
                                GlassIconButton(Icons.Rounded.Search, "Search", onClick = { searchOpen = true })
                            }
                            HeaderMode.Search -> SearchField(
                                query = vm.query,
                                onQuery = vm::onQuery,
                                onClose = {
                                    vm.onQuery("")
                                    searchOpen = false
                                },
                            )
                            HeaderMode.Select -> Row(verticalAlignment = Alignment.CenterVertically) {
                                GlassIconButton(Icons.Rounded.Close, "Cancel", onClick = vm::clearSelection, size = 40.dp)
                                AnimatedContent(
                                    targetState = vm.selection.size,
                                    transitionSpec = {
                                        val up = targetState > initialState
                                        (slideInVertically { if (up) it else -it } + fadeIn())
                                            .togetherWith(slideOutVertically { if (up) -it else it } + fadeOut())
                                    },
                                    label = "count",
                                    modifier = Modifier.padding(start = 14.dp).weight(1f),
                                ) { n ->
                                    Text("$n selected", style = LType.title.copy(color = p.text))
                                }
                                GlassIconButton(Icons.Rounded.DoneAll, "Mark as read", onClick = vm::markSelectedRead, size = 40.dp)
                                Spacer(Modifier.width(8.dp))
                                GlassIconButton(
                                    Icons.Rounded.Delete, "Delete",
                                    onClick = { confirmDelete = true },
                                    size = 40.dp,
                                    tint = p.danger,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                FilterTabs(
                    unreadOnly = vm.unreadOnly,
                    unreadCount = vm.conversations.count { it.unread > 0 },
                    onChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        vm.unreadOnly = it
                    },
                )
            }
        }

        // ---------------------------------------------------------- floating new-chat button
        AnimatedVisibility(
            visible = mode != HeaderMode.Select,
            enter = scaleIn(spring(0.6f, 400f)) + fadeIn(),
            exit = scaleOut(tween(150)) + fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp),
        ) {
            NewChatButton(expanded = fabExpanded, onClick = { vm.openNewMessage() })
        }

        GlassConfirm(
            visible = confirmDelete,
            backdrop = backdrop,
            title = "Delete ${vm.selection.size} conversation${if (vm.selection.size == 1) "" else "s"}?",
            message = "These messages will be removed from this phone. This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                confirmDelete = false
                vm.deleteSelected()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, onClose: () -> Unit) {
    val p = LocalPalette.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(120)
        runCatching { focus.requestFocus() }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        GlassSurface(
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = p.textDim, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search people and messages", style = LType.body.copy(color = p.textFaint), maxLines = 1)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQuery,
                        singleLine = true,
                        textStyle = LType.body.copy(color = p.text),
                        cursorBrush = SolidColor(p.accentA),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                AnimatedVisibility(query.isNotEmpty(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                    Icon(
                        Icons.Rounded.Close, "Clear",
                        tint = p.textDim,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { onQuery("") }
                            .padding(4.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Close search", onClick = onClose)
    }
}

@Composable
private fun FilterTabs(unreadOnly: Boolean, unreadCount: Int, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    GlassSurface(
        modifier = Modifier.fillMaxWidth().height(40.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(4.dp)) {
            val half = maxWidth / 2
            val offset by animateDpAsState(
                targetValue = if (unreadOnly) half else 0.dp,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
                label = "tab",
            )
            Box(
                Modifier
                    .offset(x = offset)
                    .width(half)
                    .fillMaxSize()
                    .shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = p.accentA, spotColor = p.accentA)
                    .clip(RoundedCornerShape(16.dp))
                    .background(p.accentBrush)
            )
            Row(Modifier.fillMaxSize()) {
                listOf(false, true).forEach { tabUnread ->
                    val active = tabUnread == unreadOnly
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onChange(tabUnread) },
                        contentAlignment = Alignment.Center,
                    ) {
                        val label = if (tabUnread) (if (unreadCount > 0) "Unread · $unreadCount" else "Unread") else "All"
                        Text(
                            label,
                            style = LType.button.copy(
                                color = if (active) p.onAccent else p.textDim,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationCard(
    conv: Conversation,
    query: String,
    selected: Boolean,
    selectionMode: Boolean,
    entranceDelayIndex: Int,
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    val appear = remember { Animatable(if (entranceDelayIndex >= 0) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (appear.value < 1f) {
            delay(entranceDelayIndex.coerceAtMost(10) * 45L)
            appear.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = 260f))
        }
    }
    val unread = conv.unread > 0
    val tint by animateFloatAsState(if (selected) 1f else 0f, label = "selTint")

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val v = appear.value
                alpha = v.coerceIn(0f, 1f)
                translationY = (1f - v) * 60.dp.toPx()
                scaleX = 0.94f + 0.06f * v
                scaleY = 0.94f + 0.06f * v
            }
            .pressScale(interaction, 0.975f),
        shape = RoundedCornerShape(26.dp),
        tint = lerpColor(p.glassTint, p.accentA.copy(alpha = 0.24f), tint),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interaction,
                    indication = ripple(color = p.text),
                    onLongClick = onLongClick,
                    onClick = onClick,
                )
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            with(sharedScope) {
                Avatar(
                    name = conv.name,
                    address = conv.address,
                    photoUri = conv.photoUri,
                    size = 52.dp,
                    selected = selected,
                    modifier = Modifier.sharedElement(
                        rememberSharedContentState(key = "avatar-${conv.threadId}"),
                        animatedVisibilityScope = animScope,
                    ),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        highlight(conv.title, query, p.accentB),
                        style = LType.name.copy(
                            color = p.text,
                            fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        Fmt.listTime(context, conv.date),
                        style = LType.caption.copy(color = if (unread) p.accentB else p.textFaint),
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (conv.lastFailed) {
                        Icon(Icons.Rounded.ErrorOutline, "Not sent", tint = p.danger, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        (if (conv.lastFromMe) "You: " else "") + conv.snippet.replace('\n', ' '),
                        style = LType.snippet.copy(
                            color = if (unread) p.text.copy(alpha = 0.86f) else p.textDim,
                            fontWeight = if (unread) FontWeight.Medium else FontWeight.Normal,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    AnimatedVisibility(
                        visible = unread && !selectionMode,
                        enter = scaleIn(spring(0.45f, 500f)) + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        PulsingBadge(conv.unread, Modifier.padding(start = 10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PulsingBadge(count: Int, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val s by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "s",
    )
    CountBadge(count, modifier.graphicsLayer { scaleX = s; scaleY = s })
}

@Composable
private fun SearchHitCard(hit: SearchHit, query: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val context = LocalContext.current
    GlassSurface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Avatar(hit.name, hit.address, null, 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text(
                        hit.name ?: hit.address,
                        style = LType.name.copy(color = p.text),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(Fmt.listTime(context, hit.date), style = LType.caption.copy(color = p.textFaint))
                }
                Text(
                    highlight(excerpt(hit.body, query), query, p.accentB),
                    style = LType.snippet.copy(color = p.textDim),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DefaultAppBanner(onRequestDefault: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tint = p.accentA.copy(alpha = if (p.dark) 0.16f else 0.12f),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(p.accentBrush),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Sms, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Not your default SMS app", style = LType.name.copy(color = p.text))
                Text(
                    "New texts go to your other app until you switch.",
                    style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(p.accentBrush)
                    .clickable(onClick = onRequestDefault)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) { Text("Switch", style = LType.button.copy(color = p.onAccent)) }
        }
    }
}

@Composable
private fun NewChatButton(expanded: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .pressScale(interaction, 0.9f)
            .shadow(22.dp, RoundedCornerShape(30.dp), ambientColor = p.accentA, spotColor = p.accentA)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(listOf(p.accentC, p.accentA, p.accentB)))
            .clickable(interactionSource = interaction, indication = ripple(color = Color.White), onClick = onClick)
            .height(60.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Edit, "New chat", tint = Color.White, modifier = Modifier.size(24.dp))
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(200, 80)) + expandHorizontally(spring(0.8f, 450f)),
                exit = fadeOut(tween(100)) + shrinkHorizontally(spring(0.9f, 500f)),
            ) {
                Text(
                    "New chat",
                    style = LType.button.copy(color = Color.White, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String) {
    val p = LocalPalette.current
    val float = rememberInfiniteTransition(label = "float")
    val y by float.animateFloat(
        initialValue = -8f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "y",
    )
    val rot by float.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(3100), RepeatMode.Reverse), label = "r",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        GlassSurface(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer { translationY = y.dp.toPx(); rotationZ = rot },
            shape = RoundedCornerShape(32.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ChatBubbleOutline, null, tint = p.accentB, modifier = Modifier.size(42.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(title, style = LType.title.copy(color = p.text))
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = LType.snippet.copy(color = p.textDim))
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = p.textFaint, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(5.dp))
            Text("No internet access · messages stay on this phone", style = LType.micro.copy(color = p.textFaint))
        }
    }
}

fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)
