package com.lumen.messages.ui

import android.content.Context
import android.util.Patterns
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date

object Fmt {
    private val weekday = DateTimeFormatter.ofPattern("EEE")
    private val weekdayLong = DateTimeFormatter.ofPattern("EEEE")
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM")
    private val dayMonthWeekday = DateTimeFormatter.ofPattern("EEE, d MMM")
    private val fullDate = DateTimeFormatter.ofPattern("d MMM yyyy")
    private val shortDate = DateTimeFormatter.ofPattern("d/M/yy")

    fun localDate(ts: Long): LocalDate = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()

    /** Respects the phone's 12/24-hour setting. */
    fun time(context: Context, ts: Long): String =
        android.text.format.DateFormat.getTimeFormat(context).format(Date(ts))

    fun listTime(context: Context, ts: Long): String {
        val now = LocalDate.now()
        val d = localDate(ts)
        return when {
            d == now -> time(context, ts)
            d == now.minusDays(1) -> "Yesterday"
            ChronoUnit.DAYS.between(d, now) in 2..6 -> d.format(weekday)
            d.year == now.year -> d.format(dayMonth)
            else -> d.format(shortDate)
        }
    }

    fun dayLabel(d: LocalDate): String {
        val now = LocalDate.now()
        return when {
            d == now -> "Today"
            d == now.minusDays(1) -> "Yesterday"
            ChronoUnit.DAYS.between(d, now) in 2..6 -> d.format(weekdayLong)
            d.year == now.year -> d.format(dayMonthWeekday)
            else -> d.format(fullDate)
        }
    }
}

private val otpKeywords = Regex("(?i)(code|otp|pin|passcode|verification|verify|password|one[- ]time|رمز)")
private val otpDigits = Regex("(?<![\\d.,])(\\d{4,8})(?![\\d.,])")

/** Finds a one-time code in messages like "Your verification code is 482913". */
fun findOtp(body: String): String? {
    if (!otpKeywords.containsMatchIn(body)) return null
    return otpDigits.find(body)?.groupValues?.get(1)
}

/** Makes web links in a message tappable. */
fun linkify(body: String, linkColor: Color): AnnotatedString = buildAnnotatedString {
    append(body)
    val matcher = Patterns.WEB_URL.matcher(body)
    while (matcher.find()) {
        val raw = matcher.group() ?: continue
        if (!raw.contains('.') || raw.all { it.isDigit() || it == '.' }) continue
        val url = if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) raw else "https://$raw"
        addLink(
            LinkAnnotation.Url(
                url,
                TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
            ),
            matcher.start(), matcher.end()
        )
    }
}

/** Bolds and colours every match of [query] inside [text]. */
fun highlight(text: String, query: String, color: Color): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        var start = text.indexOf(query, ignoreCase = true)
        while (start >= 0) {
            addStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold), start, start + query.length)
            start = text.indexOf(query, start + query.length, ignoreCase = true)
        }
    }
}

/** For long matching messages, show the part around the match. */
fun excerpt(text: String, query: String, radius: Int = 48): String {
    val i = text.indexOf(query, ignoreCase = true)
    if (i < 0 || text.length <= radius * 2) return text
    val start = (i - radius).coerceAtLeast(0)
    val end = (i + query.length + radius).coerceAtMost(text.length)
    return (if (start > 0) "…" else "") + text.substring(start, end).trim() + (if (end < text.length) "…" else "")
}
