@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)

package com.lumen.messages.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.messages.data.Category
import com.lumen.messages.data.Conversation
import com.lumen.messages.data.SearchHit
import com.lumen.messages.ui.AppViewModel
import com.lumen.messages.ui.Fmt
import com.lumen.messages.ui.ListFilter
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

/** Space reserved at the bottom for the floating tab bar. */
val BottomBarSpace = 96.dp

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
    // Lives in the view model so the list keeps its scroll position when you come back from a chat.
    val listState = vm.chatListState
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

    val filters = if (vm.prefs.smartTabs) ListFilter.values().toList() else listOf(ListFilter.All, ListFilter.Unread)
    val visible = remember(vm.conversations, vm.query, vm.filter, vm.prefs.smartTabs) {
        vm.conversations.filter { c ->
            (vm.filter !in filters || vm.matchesFilter(c)) &&
                (vm.query.isBlank() || c.title.contains(vm.query, true) || c.address.contains(vm.query, true))
        }
    }
    val people = remember(vm.conversations) {
        vm.conversations.filter { it.category == Category.Personal }.take(12)
    }
    val hits = if (vm.query.isNotBlank()) vm.searchHits else emptyList()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 8 } }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scope = rememberCoroutineScope()

    // Tap "Chats" (title or tab) to jump to the top — instantly from far down, smoothly when close.
    val handledSignal = remember { mutableIntStateOf(vm.scrollTopSignal) }
    LaunchedEffect(vm.scrollTopSignal) {
        if (vm.scrollTopSignal != handledSignal.intValue) {
            handledSignal.intValue = vm.scrollTopSignal
            if (listState.firstVisibleItemIndex > 15) listState.scrollToItem(0)
            else listState.animateScrollToItem(0)
        }
    }
    val showTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 6 } }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().backdropSource(backdrop),
            contentPadding = PaddingValues(
                top = with(density) { headerHeightPx.toDp() } + 4.dp,
                bottom = navBottom + BottomBarSpace,
            ),
        ) {
            if (people.size >= 2 && mode == HeaderMode.Normal && vm.filter == ListFilter.All) {
                item(key = "people") {
                    PeopleRow(
                        people = people,
                        onNew = { vm.openNewMessage() },
                        onOpen = { vm.openConversation(it) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            if (!vm.isDefaultSms && vm.query.isBlank()) {
                item(key = "banner") {
                    DefaultAppBanner(onRequestDefault, Modifier.padding(horizontal = 14.dp, vertical = 6.dp).animateItem())
                }
            }
            if (vm.filter == ListFilter.Transactions && vm.query.isBlank() && vm.transactions.isNotEmpty()) {
                item(key = "money-teaser") {
                    MoneyTeaser(vm, Modifier.padding(horizontal = 14.dp, vertical = 6.dp).animateItem())
                }
            }
            itemsIndexed(visible, key = { _, c -> c.threadId }, contentType = { _, _ -> "conv" }) { _, conv ->
                ConversationRow(
                    conv = conv,
                    query = vm.query,
                    selected = conv.threadId in vm.selection,
                    selectionMode = vm.selection.isNotEmpty(),
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
                        placementSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                        fadeOutSpec = tween(160),
                    ),
                )
            }
            if (hits.isNotEmpty()) {
                item(key = "hits-header") {
                    Text(
                        "IN MESSAGES",
                        style = LType.section.copy(color = p.textFaint),
                        modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp).animateItem(),
                    )
                }
                items(hits, key = { "hit-${it.messageId}" }) { hit ->
                    SearchHitRow(hit, vm.query, onClick = { vm.openThreadById(hit.threadId) }, modifier = Modifier.animateItem())
                }
            }
        }

        val empty = vm.conversationsLoaded && visible.isEmpty() && hits.isEmpty()
        AnimatedVisibility(
            visible = empty,
            enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.92f),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            EmptyState(
                title = when {
                    vm.query.isNotBlank() -> "No matches"
                    vm.filter == ListFilter.Unread -> "All caught up"
                    vm.filter != ListFilter.All -> "No ${vm.filter.label.lowercase()} yet"
                    else -> "No conversations yet"
                },
                subtitle = when {
                    vm.query.isNotBlank() -> "Nothing found for “${vm.query}”"
                    vm.filter == ListFilter.Unread -> "You've read everything."
                    vm.filter == ListFilter.Otp -> "Not getting OTPs? Open Settings → \u201cNot getting OTP messages?\u201d"
                    else -> "Tap + to send a message."
                },
            )
        }

        // ---------------------------------------------------------- glass header
        val headerTint by animateColorAsState(
            if (scrolled) p.barTint else p.barTint.copy(alpha = 0f), tween(250), label = "hdr",
        )
        GlassSurface(
            modifier = Modifier.fillMaxWidth().onSizeChanged { headerHeightPx = it.height },
            shape = RoundedCornerShape(0.dp),
            backdrop = backdrop,
            tint = headerTint,
            border = false,
            sheen = false,
        ) {
            Column(Modifier.statusBarsPadding().padding(top = 6.dp, bottom = 10.dp)) {
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        (fadeIn(tween(220, delayMillis = 60)) + slideInVertically(spring(0.85f, 500f)) { it / 3 })
                            .togetherWith(fadeOut(tween(120)) + slideOutVertically(tween(160)) { -it / 3 })
                    },
                    label = "header",
                ) { m ->
                    Box(
                        Modifier.fillMaxWidth().height(56.dp).padding(start = 20.dp, end = 14.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        when (m) {
                            HeaderMode.Normal -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f)) {
                                    Text(
                                        "Chats",
                                        style = LType.hero.copy(color = p.text, fontSize = 30.sp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(onClick = vm::scrollChatsToTop)
                                            .padding(horizontal = 2.dp),
                                    )
                                }
                                GlassIconButton(Icons.Rounded.Search, "Search", onClick = { searchOpen = true }, size = 42.dp)
                                Spacer(Modifier.width(10.dp))
                                GlassIconButton(Icons.Rounded.DoneAll, "Mark all as read", onClick = vm::markAllRead, size = 42.dp)
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
                Spacer(Modifier.height(8.dp))
                FilterChips(
                    filters = filters,
                    selected = vm.filter,
                    countFor = vm::countFor,
                    onSelect = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        vm.filter = it
                    },
                )
            }
        }

        // Back-to-top button appears once you've scrolled down a way.
        AnimatedVisibility(
            visible = showTop && mode == HeaderMode.Normal,
            enter = scaleIn(spring(0.55f, 500f)) + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBottom + BottomBarSpace + 6.dp),
        ) {
            GlassSurface(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = vm::scrollChatsToTop),
                shape = RoundedCornerShape(20.dp),
                backdrop = backdrop,
                tint = p.tabBar,
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.KeyboardArrowUp, null, tint = p.text, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Top", style = LType.caption.copy(color = p.text))
                }
            }
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
        GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Close search", onClick = onClose, size = 42.dp)
        Spacer(Modifier.width(10.dp))
        GlassSurface(modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(23.dp)) {
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
                        modifier = Modifier.size(28.dp).clip(CircleShape).clickable { onQuery("") }.padding(4.dp),
                    )
                }
            }
        }
    }
}

/** Pill filters like the reference: light pill when selected, dark glass otherwise. */
@Composable
private fun FilterChips(
    filters: List<ListFilter>,
    selected: ListFilter,
    countFor: (ListFilter) -> Int,
    onSelect: (ListFilter) -> Unit,
) {
    val p = LocalPalette.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(filters, key = { it.name }) { f ->
            val active = f == selected
            val fill by animateColorAsState(if (active) p.chipActive else p.glassTint, spring(stiffness = 600f), label = "chip")
            val scale by animateFloatAsState(if (active) 1f else 0.97f, spring(0.5f, 600f), label = "chipScale")
            val count = countFor(f)
            Row(
                Modifier
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(17.dp))
                    .background(fill)
                    .border(1.dp, if (active) Color.Transparent else p.glassEdgeDark, RoundedCornerShape(17.dp))
                    .clickable { onSelect(f) }
                    .padding(horizontal = 15.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    f.label,
                    style = LType.button.copy(
                        fontSize = 13.5.sp,
                        color = if (active) p.onChipActive else p.textDim,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                )
                if (count > 0) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.clip(CircleShape).background(p.badge).padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            if (count > 99) "99+" else "$count",
                            style = LType.micro.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        )
                    }
                }
            }
        }
    }
}

/** Contact tiles row, like the stories row in the reference. */
@Composable
private fun PeopleRow(
    people: List<Conversation>,
    onNew: () -> Unit,
    onOpen: (Conversation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val tile = RoundedCornerShape(18.dp)
    LazyRow(
        modifier = modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "new") {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(62.dp)) {
                val interaction = remember { MutableInteractionSource() }
                GlassSurface(
                    modifier = Modifier.size(62.dp).pressScale(interaction, 0.9f),
                    shape = tile,
                ) {
                    Box(
                        Modifier.fillMaxSize().clickable(interaction, ripple(color = p.text), onClick = onNew),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Add, "New message", tint = p.text, modifier = Modifier.size(26.dp)) }
                }
                Spacer(Modifier.height(6.dp))
                Text("New", style = LType.micro.copy(color = p.textDim))
            }
        }
        items(people, key = { "pp-${it.threadId}" }) { c ->
            val interaction = remember { MutableInteractionSource() }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(62.dp)) {
                Box(
                    Modifier
                        .size(62.dp)
                        .pressScale(interaction, 0.9f)
                        .clickable(interaction, null) { onOpen(c) }
                ) {
                    Avatar(
                        name = c.name, address = c.address, photoUri = c.photoUri,
                        size = 62.dp, shape = tile,
                        modifier = Modifier.border(
                            1.5.dp,
                            if (c.unread > 0) p.accentA else p.glassEdgeLight,
                            tile,
                        ),
                    )
                    if (c.unread > 0) {
                        CountBadge(c.unread, Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    (c.name ?: c.address).substringBefore(' '),
                    style = LType.micro.copy(color = p.textDim),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conv: Conversation,
    query: String,
    selected: Boolean,
    selectionMode: Boolean,
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val interaction = remember { MutableInteractionSource() }
    val unread = conv.unread > 0
    val bg by animateColorAsState(if (selected) p.chipActive.copy(alpha = 0.18f) else Color.Transparent, label = "selBg")

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(color = p.text),
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        with(sharedScope) {
            Avatar(
                name = conv.name,
                address = conv.address,
                photoUri = conv.photoUri,
                size = 50.dp,
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
                    highlight(conv.title, query, p.accentA),
                    style = LType.name.copy(color = p.text, fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    Fmt.listTime(context, conv.date),
                    style = LType.caption.copy(
                        color = if (unread) p.text else p.textFaint,
                        fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    conv.lastFailed -> {
                        Icon(Icons.Rounded.ErrorOutline, "Not sent", tint = p.danger, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    conv.lastFromMe -> {
                        Icon(Icons.Rounded.DoneAll, null, tint = p.textFaint, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                }
                Text(
                    conv.snippet.replace('\n', ' '),
                    style = LType.snippet.copy(
                        color = if (unread) p.text.copy(alpha = 0.85f) else p.textDim,
                        fontWeight = if (unread) FontWeight.Medium else FontWeight.Normal,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                AnimatedVisibility(
                    visible = unread && !selectionMode,
                    enter = scaleIn(spring(0.45f, 500f)) + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    CountBadge(conv.unread, Modifier.padding(start = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchHitRow(hit: SearchHit, query: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val context = LocalContext.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Avatar(hit.name, hit.address, null, 40.dp)
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
                highlight(excerpt(hit.body, query), query, p.accentA),
                style = LType.snippet.copy(color = p.textDim),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DefaultAppBanner(onRequestDefault: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    GlassSurface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(p.accentA),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Sms, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Not your default SMS app", style = LType.name.copy(color = p.text))
                Text(
                    "OTPs and new texts won't arrive here until you switch.",
                    style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(p.chipActive)
                    .clickable(onClick = onRequestDefault)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) { Text("Switch", style = LType.button.copy(color = p.onChipActive)) }
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String) {
    val p = LocalPalette.current
    val float = rememberInfiniteTransition(label = "float")
    val y by float.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "y",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        GlassSurface(
            modifier = Modifier.size(84.dp).graphicsLayer { translationY = y.dp.toPx() },
            shape = RoundedCornerShape(28.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ChatBubbleOutline, null, tint = p.text, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = LType.title.copy(color = p.text))
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = LType.snippet.copy(color = p.textDim), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = p.textFaint, modifier = Modifier.size(12.dp))
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
