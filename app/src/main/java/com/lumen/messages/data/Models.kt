package com.lumen.messages.data

/** Column names and values of Android's SMS provider (content://sms). */
object SmsCol {
    const val ID = "_id"
    const val THREAD_ID = "thread_id"
    const val ADDRESS = "address"
    const val BODY = "body"
    const val DATE = "date"
    const val DATE_SENT = "date_sent"
    const val READ = "read"
    const val SEEN = "seen"
    const val TYPE = "type"
    const val STATUS = "status"
    const val SUB_ID = "sub_id"

    const val TYPE_INBOX = 1
    const val TYPE_SENT = 2
    const val TYPE_DRAFT = 3
    const val TYPE_OUTBOX = 4
    const val TYPE_FAILED = 5
    const val TYPE_QUEUED = 6

    const val STATUS_NONE = -1
    const val STATUS_COMPLETE = 0
    const val STATUS_PENDING = 32
    const val STATUS_FAILED = 64
}

/** Smart grouping of conversations. */
enum class Category(val label: String) {
    Personal("Personal"),
    Transactions("Transactions"),
    Otp("OTPs"),
    Updates("Updates"),
}

data class Conversation(
    val threadId: Long,
    val address: String,
    val name: String?,
    val photoUri: String?,
    val snippet: String,
    val date: Long,
    val unread: Int,
    val lastFromMe: Boolean,
    val lastFailed: Boolean,
    val count: Int,
    val category: Category = Category.Personal,
) {
    val title: String get() = name ?: address.ifBlank { "Unknown" }
}

data class Message(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int,
    val read: Boolean,
    val status: Int,
    val subId: Int,
) {
    val isMine: Boolean get() = type != SmsCol.TYPE_INBOX
    val isFailed: Boolean get() = type == SmsCol.TYPE_FAILED
    val isPending: Boolean get() = type == SmsCol.TYPE_OUTBOX || type == SmsCol.TYPE_QUEUED
    val isDelivered: Boolean get() = type == SmsCol.TYPE_SENT && status == SmsCol.STATUS_COMPLETE
}

data class ThreadHeader(
    val threadId: Long,
    val address: String,
    val name: String?,
    val photoUri: String?,
) {
    val title: String get() = name ?: address.ifBlank { "Unknown" }
}

data class ContactEntry(
    val name: String,
    val number: String,
    val label: String,
    val photoUri: String?,
)

data class SimInfo(
    val subId: Int,
    val label: String,
    val slot: Int,
    val color: Int,
)

data class SearchHit(
    val threadId: Long,
    val messageId: Long,
    val address: String,
    val name: String?,
    val body: String,
    val date: Long,
)

data class ThreadStats(
    val total: Int,
    val sent: Int,
    val received: Int,
    val firstDate: Long,
    val otps: Int,
    val transactions: Int,
)
