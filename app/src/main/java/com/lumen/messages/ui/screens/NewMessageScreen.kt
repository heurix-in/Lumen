package com.lumen.messages.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lumen.messages.data.ContactEntry
import com.lumen.messages.ui.AppViewModel
import com.lumen.messages.ui.components.Avatar
import com.lumen.messages.ui.components.GlassIconButton
import com.lumen.messages.ui.glass.GlassSurface
import com.lumen.messages.ui.glass.backdropSource
import com.lumen.messages.ui.glass.rememberBackdropState
import com.lumen.messages.ui.highlight
import com.lumen.messages.ui.theme.LType
import com.lumen.messages.ui.theme.LocalPalette
import kotlinx.coroutines.delay

private val phoneLike = Regex("^[+0-9 ()\\-]{3,}$")

@Composable
fun NewMessageScreen(vm: AppViewModel, onRequestContacts: () -> Unit) {
    val p = LocalPalette.current
    val density = LocalDensity.current
    val backdrop = rememberBackdropState()
    val focus = remember { FocusRequester() }
    var to by remember { mutableStateOf("") }
    var headerPx by remember { mutableIntStateOf(0) }
    var numericKeyboard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(280)
        runCatching { focus.requestFocus() }
    }

    val filtered = remember(to, vm.contacts) {
        val q = to.trim()
        if (q.isEmpty()) vm.contacts
        else {
            val digits = q.filter { it.isDigit() }
            vm.contacts.filter {
                it.name.contains(q, ignoreCase = true) ||
                    (digits.length >= 2 && it.number.filter { c -> c.isDigit() }.contains(digits))
            }
        }
    }
    val typedNumber = to.trim().takeIf { phoneLike.matches(it) }
    val bottomPad = maxOf(
        WindowInsets.ime.asPaddingValues().calculateBottomPadding(),
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
    )

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().backdropSource(backdrop),
            contentPadding = PaddingValues(
                top = with(density) { headerPx.toDp() } + 12.dp,
                bottom = bottomPad + 24.dp,
                start = 14.dp, end = 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (typedNumber != null) {
                item(key = "typed") {
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth().animateItem(),
                        shape = RoundedCornerShape(22.dp),
                        tint = p.accentA.copy(alpha = 0.2f),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { vm.startConversationWith(typedNumber) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape).background(p.accentBrush),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("Send to $typedNumber", style = LType.name.copy(color = p.text))
                                Text("New conversation", style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal))
                            }
                        }
                    }
                }
            }
            if (!vm.hasContacts) {
                item(key = "contacts-perm") {
                    GlassSurface(modifier = Modifier.fillMaxWidth().animateItem(), shape = RoundedCornerShape(22.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clickable(onClick = onRequestContacts).padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Contacts, null, tint = p.accentB)
                            Spacer(Modifier.width(14.dp))
                            Text(
                                "Allow contacts to see names and pick people",
                                style = LType.snippet.copy(color = p.text),
                            )
                        }
                    }
                }
            }
            itemsIndexed(filtered, key = { _, c -> c.name + "|" + c.number }) { i, c ->
                val prev = filtered.getOrNull(i - 1)
                val letter = c.name.firstOrNull()?.uppercaseChar() ?: '#'
                val showLetter = to.isBlank() && (prev == null || prev.name.firstOrNull()?.uppercaseChar() != letter)
                Column(Modifier.animateItem()) {
                    if (showLetter) {
                        Text(
                            letter.toString(),
                            style = LType.caption.copy(color = p.accentB, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(start = 10.dp, top = 12.dp, bottom = 4.dp),
                        )
                    }
                    ContactRow(c, to.trim(), entranceIndex = i) { vm.startConversationWith(c.number) }
                }
            }
        }

        GlassSurface(
            modifier = Modifier.fillMaxWidth().onSizeChanged { headerPx = it.height },
            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
            backdrop = backdrop,
            tint = p.barTint,
        ) {
            Column(Modifier.statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = { vm.back() }, size = 42.dp)
                    Spacer(Modifier.width(14.dp))
                    Text("New message", style = LType.title.copy(color = p.text), modifier = Modifier.weight(1f))
                    GlassIconButton(
                        Icons.Rounded.Dialpad,
                        if (numericKeyboard) "Letters keyboard" else "Number keyboard",
                        onClick = { numericKeyboard = !numericKeyboard },
                        size = 42.dp,
                        tint = if (numericKeyboard) p.accentB else p.text,
                    )
                }
                Spacer(Modifier.height(12.dp))
                GlassSurface(modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(25.dp)) {
                    Row(
                        Modifier.fillMaxSize().padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("To", style = LType.button.copy(color = p.accentB))
                        Spacer(Modifier.width(12.dp))
                        Box(Modifier.weight(1f)) {
                            if (to.isEmpty()) Text("Name or number", style = LType.body.copy(color = p.textFaint))
                            BasicTextField(
                                value = to,
                                onValueChange = { to = it },
                                singleLine = true,
                                textStyle = LType.body.copy(color = p.text),
                                cursorBrush = SolidColor(p.accentA),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = if (numericKeyboard) KeyboardType.Phone else KeyboardType.Text,
                                    imeAction = ImeAction.Go,
                                ),
                                keyboardActions = KeyboardActions(onGo = {
                                    when {
                                        typedNumber != null -> vm.startConversationWith(typedNumber)
                                        filtered.size == 1 -> vm.startConversationWith(filtered[0].number)
                                    }
                                }),
                                modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = vm.pendingBody != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Text(
                        "Sharing: " + (vm.pendingBody ?: ""),
                        style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 18.dp, top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactRow(c: ContactEntry, query: String, entranceIndex: Int, onClick: () -> Unit) {
    val p = LocalPalette.current
    val appear = remember { Animatable(if (entranceIndex < 14) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (appear.value < 1f) {
            delay(entranceIndex * 22L)
            appear.animateTo(1f, spring(0.8f, 300f))
        }
    }
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value.coerceIn(0f, 1f)
                translationX = (1f - appear.value) * 40.dp.toPx()
            },
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(c.name, c.number, c.photoUri, 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    highlight(c.name, query, p.accentB),
                    style = LType.name.copy(color = p.text),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${c.label} · ${c.number}",
                    style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal),
                    maxLines = 1,
                )
            }
        }
    }
}
