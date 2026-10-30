package com.lumen.messages.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumen.messages.data.Transaction
import com.lumen.messages.data.TxKind
import com.lumen.messages.ui.AppViewModel
import com.lumen.messages.ui.Fmt
import com.lumen.messages.ui.MoneyGrouping
import com.lumen.messages.ui.components.GlassIconButton
import com.lumen.messages.ui.glass.GlassSurface
import com.lumen.messages.ui.glass.backdropSource
import com.lumen.messages.ui.glass.rememberBackdropState
import com.lumen.messages.ui.theme.LType
import com.lumen.messages.ui.theme.LocalPalette
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
private val monthShort = DateTimeFormatter.ofPattern("MMM")

fun formatMoney(amount: Double, currency: String, signed: TxKind? = null): String {
    val decimals = if (currency in setOf("KWD", "BHD", "OMR")) 3 else 2
    val sign = when (signed) {
        TxKind.Credit -> "+"
        TxKind.Debit -> "−"
        null -> ""
    }
    return sign + currency + " " + String.format(Locale.US, "%,.${decimals}f", amount)
}

private fun Transaction.month(): YearMonth = YearMonth.from(Fmt.localDate(date))

private data class Totals(val currency: String, val inSum: Double, val outSum: Double, val count: Int)

private fun totalsOf(list: List<Transaction>): List<Totals> =
    list.groupBy { it.currency }
        .map { (cur, l) ->
            Totals(
                cur,
                l.filter { it.kind == TxKind.Credit }.sumOf { it.amount },
                l.filter { it.kind == TxKind.Debit }.sumOf { it.amount },
                l.size,
            )
        }
        .sortedByDescending { it.count }

/** Small summary shown on the Transactions tab of the conversation list. */
@Composable
fun MoneyTeaser(vm: AppViewModel, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val month = YearMonth.now()
    val totals = remember(vm.transactions) {
        totalsOf(vm.transactions.filter { it.month() == month }).firstOrNull()
    }
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tint = p.accentA.copy(alpha = if (p.dark) 0.16f else 0.10f),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = vm::openMoney).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(p.accentA),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.AccountBalance, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Money · ${month.format(monthFmt)}", style = LType.name.copy(color = p.text))
                if (totals != null) {
                    Text(
                        "In ${formatMoney(totals.inSum, totals.currency)}  ·  Out ${formatMoney(totals.outSum, totals.currency)}",
                        style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Text("See bank transfers by account and month", style = LType.caption.copy(color = p.textDim))
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = p.textDim)
        }
    }
}

@Composable
fun MoneyScreen(vm: AppViewModel) {
    val p = LocalPalette.current
    val density = LocalDensity.current
    val backdrop = rememberBackdropState()
    var headerPx by remember { mutableIntStateOf(0) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val all = vm.transactions
    val accounts = remember(all) {
        all.groupBy { it.accountKey }
            .map { (key, l) -> Triple(key, l.first().accountLabel, l.size) }
            .sortedByDescending { it.third }
    }
    val forAccount = remember(all, vm.moneyAccount) {
        if (vm.moneyAccount == null) all else all.filter { it.accountKey == vm.moneyAccount }
    }
    val monthTx = remember(forAccount, vm.moneyMonth) {
        forAccount.filter { it.month() == vm.moneyMonth }.sortedByDescending { it.date }
    }
    val totals = remember(monthTx) { totalsOf(monthTx) }
    val primaryCurrency = remember(forAccount) {
        forAccount.groupingBy { it.currency }.eachCount().maxByOrNull { it.value }?.key ?: "INR"
    }
    val bars = remember(forAccount, vm.moneyMonth, primaryCurrency) {
        (5 downTo 0).map { back ->
            val m = vm.moneyMonth.minusMonths(back.toLong())
            val l = forAccount.filter { it.currency == primaryCurrency && it.month() == m }
            Triple(
                m,
                l.filter { it.kind == TxKind.Credit }.sumOf { it.amount },
                l.filter { it.kind == TxKind.Debit }.sumOf { it.amount },
            )
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().backdropSource(backdrop),
            contentPadding = PaddingValues(
                top = with(density) { headerPx.toDp() } + 12.dp,
                bottom = navBottom + BottomBarSpace,
                start = 14.dp, end = 14.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "month") {
                MonthSwitcher(
                    month = vm.moneyMonth,
                    onPrev = { vm.moneyMonth = vm.moneyMonth.minusMonths(1) },
                    onNext = { if (vm.moneyMonth < YearMonth.now()) vm.moneyMonth = vm.moneyMonth.plusMonths(1) },
                    canNext = vm.moneyMonth < YearMonth.now(),
                )
            }
            if (accounts.size > 1) {
                item(key = "accounts") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item(key = "all") {
                            AccountChip("All accounts", all.size, vm.moneyAccount == null) { vm.moneyAccount = null }
                        }
                        items(accounts, key = { it.first }) { (key, label, count) ->
                            AccountChip(label, count, vm.moneyAccount == key) { vm.moneyAccount = key }
                        }
                    }
                }
            }
            item(key = "summary") { SummaryCard(totals, monthTx.size) }
            item(key = "chart") {
                ChartCard(bars, primaryCurrency, vm.moneyMonth) { vm.moneyMonth = it }
            }
            item(key = "grouping") {
                Segmented(
                    options = MoneyGrouping.values().toList(),
                    selected = vm.moneyGrouping,
                    label = { it.label },
                    onSelect = { vm.moneyGrouping = it },
                )
            }

            if (!vm.transactionsLoaded) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = p.accentA, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                }
            } else if (monthTx.isEmpty()) {
                item(key = "empty") {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Rounded.AccountBalance, null, tint = p.textFaint, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("No bank messages in ${vm.moneyMonth.format(monthFmt)}", style = LType.name.copy(color = p.textDim))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Lumen reads SMS that mention a debit or credit and an account number.",
                            style = LType.caption.copy(color = p.textFaint, fontWeight = FontWeight.Normal),
                        )
                    }
                }
            } else {
                when (vm.moneyGrouping) {
                    MoneyGrouping.Date -> {
                        val byDay = monthTx.groupBy { Fmt.localDate(it.date) }
                        byDay.forEach { (day, list) ->
                            item(key = "d-$day") { GroupHeader(Fmt.dayLabel(day), totalsOf(list)) }
                            items(list, key = { "t-${it.messageId}" }) { tx ->
                                TxRow(tx, showAccount = vm.moneyAccount == null) { vm.openThreadById(tx.threadId) }
                            }
                        }
                    }
                    MoneyGrouping.Party -> {
                        val byParty = monthTx.groupBy { it.party ?: it.bank }
                            .entries.sortedByDescending { e -> e.value.sumOf { it.amount } }
                        items(byParty.toList(), key = { "p-${it.key}" }) { (party, list) ->
                            PartyCard(party, list) { vm.openThreadById(it.threadId) }
                        }
                    }
                    MoneyGrouping.Account -> {
                        val byAcc = monthTx.groupBy { it.accountLabel }
                        byAcc.forEach { (label, list) ->
                            item(key = "a-$label") { GroupHeader(label, totalsOf(list)) }
                            items(list, key = { "ta-${it.messageId}" }) { tx ->
                                TxRow(tx, showAccount = false) { vm.openThreadById(tx.threadId) }
                            }
                        }
                    }
                }
            }
            item(key = "privacy") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Lock, null, tint = p.textFaint, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Worked out on this phone from your SMS. Nothing is uploaded.", style = LType.micro.copy(color = p.textFaint))
                }
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
                Column(Modifier.weight(1f)) {
                    Text("Money", style = LType.hero.copy(color = p.text, fontSize = 30.sp))
                    Text(
                        "Bank transfers from your SMS",
                        style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal),
                    )
                }
                GlassIconButton(Icons.Rounded.Refresh, "Rescan", onClick = vm::loadTransactions, size = 42.dp)
            }
        }
    }
}

@Composable
private fun MonthSwitcher(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit, canNext: Boolean) {
    val p = LocalPalette.current
    GlassSurface(modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(26.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onPrev),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.ChevronLeft, "Previous month", tint = p.text) }
            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(spring(0.85f, 500f)) { if (forward) it / 2 else -it / 2 } + fadeIn())
                        .togetherWith(slideOutHorizontally(tween(160)) { if (forward) -it / 2 else it / 2 } + fadeOut(tween(120)))
                },
                label = "month",
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) { m ->
                Text(
                    m.format(monthFmt),
                    style = LType.name.copy(color = p.text, fontSize = 17.sp),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).clickable(enabled = canNext, onClick = onNext),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.ChevronRight, "Next month", tint = if (canNext) p.text else p.textFaint) }
        }
    }
}

@Composable
private fun AccountChip(label: String, count: Int, active: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val fill by animateColorAsState(if (active) p.accentA else p.glassTint, label = "acc")
    Row(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(fill)
            .border(1.dp, if (active) Color.Transparent else p.glassEdgeLight, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = LType.button.copy(fontSize = 14.sp, color = if (active) Color.White else p.text))
        Spacer(Modifier.width(6.dp))
        Text("$count", style = LType.micro.copy(color = if (active) Color.White.copy(alpha = 0.8f) else p.textFaint))
    }
}

@Composable
private fun AnimatedAmount(value: Double, currency: String, color: Color, big: Boolean) {
    val shown by animateFloatAsState(value.toFloat(), spring(dampingRatio = 1f, stiffness = 120f), label = "amt")
    Text(
        formatMoney(if (shown == value.toFloat()) value else shown.toDouble(), currency),
        style = (if (big) LType.title.copy(fontSize = 19.sp) else LType.name).copy(color = color, fontWeight = FontWeight.Bold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SummaryCard(totals: List<Totals>, count: Int) {
    val p = LocalPalette.current
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(18.dp)) {
            if (totals.isEmpty()) {
                Text("No transactions", style = LType.name.copy(color = p.textDim))
            }
            totals.forEachIndexed { i, t ->
                if (i > 0) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp).height(1.dp).background(p.divider))
                }
                Row {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ArrowDownward, null, tint = p.success, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Money in", style = LType.caption.copy(color = p.textDim))
                        }
                        AnimatedAmount(t.inSum, t.currency, p.success, big = true)
                    }
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ArrowUpward, null, tint = p.danger, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Money out", style = LType.caption.copy(color = p.textDim))
                        }
                        AnimatedAmount(t.outSum, t.currency, p.danger, big = true)
                    }
                }
                Spacer(Modifier.height(8.dp))
                val net = t.inSum - t.outSum
                Text(
                    "Net ${if (net >= 0) "+" else "−"}${formatMoney(kotlin.math.abs(net), t.currency)}  ·  ${t.count} transactions",
                    style = LType.caption.copy(color = p.textFaint, fontWeight = FontWeight.Normal),
                )
            }
        }
    }
}

@Composable
private fun ChartCard(
    bars: List<Triple<YearMonth, Double, Double>>,
    currency: String,
    selected: YearMonth,
    onSelect: (YearMonth) -> Unit,
) {
    val p = LocalPalette.current
    val max = (bars.maxOfOrNull { maxOf(it.second, it.third) } ?: 0.0).coerceAtLeast(1.0)
    val grow = remember { Animatable(0f) }
    LaunchedEffect(bars) {
        grow.snapTo(0f)
        grow.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 140f))
    }
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Last 6 months", style = LType.name.copy(color = p.text), modifier = Modifier.weight(1f))
                Legend(p.success, "In")
                Spacer(Modifier.width(10.dp))
                Legend(p.danger, "Out")
                Spacer(Modifier.width(10.dp))
                Text(currency, style = LType.micro.copy(color = p.textFaint))
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().height(130.dp), verticalAlignment = Alignment.Bottom) {
                bars.forEach { (m, inSum, outSum) ->
                    val active = m == selected
                    val bg by animateColorAsState(if (active) p.glassTint else Color.Transparent, label = "barBg")
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(14.dp))
                            .background(bg)
                            .clickable { onSelect(m) }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            Modifier.weight(1f),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Bar((inSum / max).toFloat() * grow.value, p.success)
                            Bar((outSum / max).toFloat() * grow.value, p.danger)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            m.format(monthShort),
                            style = LType.micro.copy(
                                color = if (active) p.text else p.textFaint,
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
private fun Bar(fraction: Float, color: Color) {
    Box(
        Modifier
            .width(9.dp)
            .fillMaxHeight(fraction.coerceIn(0.015f, 1f))
            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
            .background(color)
    )
}

@Composable
private fun Legend(color: Color, label: String) {
    val p = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = LType.micro.copy(color = p.textDim))
    }
}

@Composable
private fun GroupHeader(title: String, totals: List<Totals>) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = LType.section.copy(color = p.textDim), modifier = Modifier.weight(1f))
        val t = totals.firstOrNull()
        if (t != null) {
            Text(
                buildString {
                    if (t.inSum > 0) append("+" + formatMoney(t.inSum, t.currency))
                    if (t.inSum > 0 && t.outSum > 0) append("  ")
                    if (t.outSum > 0) append("−" + formatMoney(t.outSum, t.currency))
                },
                style = LType.micro.copy(color = p.textFaint),
            )
        }
    }
}

@Composable
private fun TxRow(tx: Transaction, showAccount: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val credit = tx.kind == TxKind.Credit
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background((if (credit) p.success else p.danger).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (credit) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward, null,
                    tint = if (credit) p.success else p.danger,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tx.party ?: (if (credit) "Money received" else "Money sent"),
                    style = LType.name.copy(color = p.text),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    (if (showAccount) tx.accountLabel + " · " else "") + Fmt.time(context, tx.date),
                    style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatMoney(tx.amount, tx.currency, tx.kind),
                    style = LType.name.copy(color = if (credit) p.success else p.text, fontWeight = FontWeight.Bold),
                )
                tx.balance?.let {
                    Text("Bal ${formatMoney(it, tx.currency)}", style = LType.micro.copy(color = p.textFaint))
                }
            }
        }
    }
}

@Composable
private fun PartyCard(party: String, list: List<Transaction>, onOpen: (Transaction) -> Unit) {
    val p = LocalPalette.current
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val t = totalsOf(list).firstOrNull()
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chev")
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(party, style = LType.name.copy(color = p.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${list.size} transaction${if (list.size == 1) "" else "s"}",
                        style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal),
                    )
                }
                if (t != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        if (t.outSum > 0) Text(formatMoney(t.outSum, t.currency, TxKind.Debit), style = LType.name.copy(color = p.text))
                        if (t.inSum > 0) Text(formatMoney(t.inSum, t.currency, TxKind.Credit), style = LType.name.copy(color = p.success))
                    }
                }
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Rounded.KeyboardArrowDown, null, tint = p.textDim,
                    modifier = Modifier.graphicsLayer { rotationZ = rotation },
                )
            }
            AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(Modifier.padding(bottom = 8.dp)) {
                    list.forEach { tx ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpen(tx) }.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                Fmt.listTime(context, tx.date) + " · " + tx.accountLabel,
                                style = LType.caption.copy(color = p.textDim, fontWeight = FontWeight.Normal),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                formatMoney(tx.amount, tx.currency, tx.kind),
                                style = LType.caption.copy(color = if (tx.kind == TxKind.Credit) p.success else p.text),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Generic glass segmented control with a sliding pill. */
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    val p = LocalPalette.current
    GlassSurface(modifier = Modifier.fillMaxWidth().height(42.dp), shape = RoundedCornerShape(21.dp)) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(4.dp)) {
            val w = maxWidth / options.size
            val index = options.indexOf(selected).coerceAtLeast(0)
            val x by androidx.compose.animation.core.animateDpAsState(
                w * index, spring(dampingRatio = 0.75f, stiffness = 520f), label = "seg",
            )
            Box(
                Modifier
                    .offset(x = x)
                    .width(w)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(17.dp))
                    .background(p.accentA)
            )
            Row(Modifier.fillMaxSize()) {
                options.forEach { o ->
                    val active = o == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(17.dp))
                            .clickable { onSelect(o) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label(o),
                            style = LType.button.copy(
                                fontSize = 13.5.sp,
                                color = if (active) Color.White else p.textDim,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
