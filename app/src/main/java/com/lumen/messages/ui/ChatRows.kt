package com.lumen.messages.ui

import com.lumen.messages.data.Message
import java.time.LocalDate

sealed interface ChatRow {
    val key: String
}

data class DayRow(val day: LocalDate) : ChatRow {
    override val key: String = "day-${day.toEpochDay()}"
}

data class MsgRow(
    val msg: Message,
    val groupTop: Boolean,
    val groupBottom: Boolean,
    val otp: String?,
) : ChatRow {
    override val key: String = "m-${msg.id}"
}

private const val GROUP_GAP_MS = 3 * 60 * 1000L

/**
 * Builds rows newest-first (for a bottom-anchored list) with day separators and bubble grouping.
 * Runs on a background thread so opening a long conversation never stutters.
 */
fun buildRows(messages: List<Message>): List<ChatRow> {
    val rows = ArrayList<ChatRow>(messages.size + 8)
    var lastDay: LocalDate? = null
    val days = messages.map { Fmt.localDate(it.date) }
    for (i in messages.indices) {
        val m = messages[i]
        val day = days[i]
        if (day != lastDay) {
            rows += DayRow(day)
            lastDay = day
        }
        val prev = messages.getOrNull(i - 1)
        val next = messages.getOrNull(i + 1)
        val top = prev == null || prev.isMine != m.isMine || m.date - prev.date > GROUP_GAP_MS || days[i - 1] != day
        val bottom = next == null || next.isMine != m.isMine || next.date - m.date > GROUP_GAP_MS || days[i + 1] != day
        rows += MsgRow(m, top, bottom, if (m.isMine) null else findOtp(m.body))
    }
    rows.reverse()
    return rows
}
