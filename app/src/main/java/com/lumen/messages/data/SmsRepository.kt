package com.lumen.messages.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.lumen.messages.ui.findOtp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * All access to the phone's own SMS database. Nothing here touches the network:
 * messages are read from and written to Android's local SMS store only.
 */
class SmsRepository(private val context: Context) {

    private val cr get() = context.contentResolver
    val contacts = ContactResolver(context)

    /** Threads known to contain bank transactions (filled by [scanTransactions]). */
    @Volatile
    var transactionThreads: Set<Long> = emptySet()

    companion object {
        val SMS_URI: Uri = Uri.parse("content://sms")
        val INBOX_URI: Uri = Uri.parse("content://sms/inbox")
        val OUTBOX_URI: Uri = Uri.parse("content://sms/outbox")
        const val PAGE = 300
    }

    fun has(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** Emits whenever the SMS store or contacts change. */
    fun observe(): Flow<Unit> = callbackFlow {
        val handler = Handler(Looper.getMainLooper())
        val smsObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        val contactsObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                contacts.invalidate()
                trySend(Unit)
            }
        }
        val registered = ArrayList<ContentObserver>()
        try {
            cr.registerContentObserver(SMS_URI, true, smsObserver); registered += smsObserver
        } catch (_: Exception) {
        }
        if (contacts.canRead()) {
            try {
                cr.registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contactsObserver)
                registered += contactsObserver
            } catch (_: Exception) {
            }
        }
        trySend(Unit)
        awaitClose { registered.forEach { cr.unregisterContentObserver(it) } }
    }

    // ---------------------------------------------------------------- conversations

    private class ConvAcc(
        val threadId: Long,
        var address: String,
        val snippet: String,
        val date: Long,
        val lastFromMe: Boolean,
        val lastType: Int,
    ) {
        var unread = 0
        var count = 1
    }

    fun categorize(address: String, name: String?, snippet: String, threadId: Long): Category {
        val business = address.any { it.isLetter() }
        return when {
            name != null && !business -> Category.Personal
            findOtp(snippet) != null -> Category.Otp
            threadId in transactionThreads -> Category.Transactions
            business && BankParser.parse(0, threadId, address, snippet, 0) != null -> Category.Transactions
            business -> Category.Updates
            address.count { it.isDigit() } in 3..6 -> Category.Updates // short codes
            else -> Category.Personal
        }
    }

    fun loadConversations(): List<Conversation> {
        if (!has(Manifest.permission.READ_SMS)) return emptyList()
        val acc = LinkedHashMap<Long, ConvAcc>()
        try {
            cr.query(
                SMS_URI,
                arrayOf(SmsCol.THREAD_ID, SmsCol.ADDRESS, SmsCol.BODY, SmsCol.DATE, SmsCol.READ, SmsCol.TYPE),
                null, null, "${SmsCol.DATE} DESC"
            )?.use { c ->
                val iThread = c.getColumnIndexOrThrow(SmsCol.THREAD_ID)
                val iAddr = c.getColumnIndexOrThrow(SmsCol.ADDRESS)
                val iBody = c.getColumnIndexOrThrow(SmsCol.BODY)
                val iDate = c.getColumnIndexOrThrow(SmsCol.DATE)
                val iRead = c.getColumnIndexOrThrow(SmsCol.READ)
                val iType = c.getColumnIndexOrThrow(SmsCol.TYPE)
                while (c.moveToNext()) {
                    val type = c.getInt(iType)
                    if (type == SmsCol.TYPE_DRAFT) continue
                    val threadId = c.getLong(iThread)
                    val unread = type == SmsCol.TYPE_INBOX && c.getInt(iRead) == 0
                    val existing = acc[threadId]
                    if (existing == null) {
                        acc[threadId] = ConvAcc(
                            threadId = threadId,
                            address = c.getString(iAddr).orEmpty(),
                            snippet = c.getString(iBody).orEmpty(),
                            date = c.getLong(iDate),
                            lastFromMe = type != SmsCol.TYPE_INBOX,
                            lastType = type,
                        ).also { if (unread) it.unread = 1 }
                    } else {
                        existing.count++
                        if (unread) existing.unread++
                        if (existing.address.isBlank()) existing.address = c.getString(iAddr).orEmpty()
                    }
                }
            }
        } catch (_: Exception) {
        }
        return acc.values.map { a ->
            val r = contacts.lookup(a.address)
            Conversation(
                threadId = a.threadId,
                address = a.address,
                name = r.name,
                photoUri = r.photoUri,
                snippet = a.snippet,
                date = a.date,
                unread = a.unread,
                lastFromMe = a.lastFromMe,
                lastFailed = a.lastType == SmsCol.TYPE_FAILED,
                count = a.count,
                category = categorize(a.address, r.name, a.snippet, a.threadId),
            )
        }
    }

    // ---------------------------------------------------------------- instant-start cache
    // The last conversation list is kept in the app's private storage so the screen
    // appears immediately on launch while the full list refreshes in the background.

    private val cacheFile get() = File(context.filesDir, "conversations.json")

    fun saveCache(list: List<Conversation>) {
        try {
            val arr = JSONArray()
            list.take(200).forEach { c ->
                arr.put(
                    JSONObject()
                        .put("t", c.threadId).put("a", c.address).put("n", c.name ?: "")
                        .put("p", c.photoUri ?: "").put("s", c.snippet.take(160)).put("d", c.date)
                        .put("u", c.unread).put("m", c.lastFromMe).put("f", c.lastFailed)
                        .put("c", c.count).put("g", c.category.name)
                )
            }
            cacheFile.writeText(arr.toString())
        } catch (_: Exception) {
        }
    }

    fun loadCache(): List<Conversation> = try {
        if (!cacheFile.exists()) emptyList()
        else {
            val arr = JSONArray(cacheFile.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Conversation(
                    threadId = o.getLong("t"),
                    address = o.getString("a"),
                    name = o.getString("n").ifEmpty { null },
                    photoUri = o.getString("p").ifEmpty { null },
                    snippet = o.getString("s"),
                    date = o.getLong("d"),
                    unread = o.getInt("u"),
                    lastFromMe = o.getBoolean("m"),
                    lastFailed = o.getBoolean("f"),
                    count = o.getInt("c"),
                    category = Category.values().firstOrNull { it.name == o.optString("g") } ?: Category.Personal,
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    // ---------------------------------------------------------------- messages

    private val fullProjection = arrayOf(
        SmsCol.ID, SmsCol.THREAD_ID, SmsCol.ADDRESS, SmsCol.BODY, SmsCol.DATE,
        SmsCol.TYPE, SmsCol.READ, SmsCol.STATUS, SmsCol.SUB_ID
    )
    private val basicProjection = fullProjection.copyOf(fullProjection.size - 1).requireNoNulls()

    /** Queries with sub_id, falling back without it on the rare phones that lack that column. */
    private fun queryMessages(selection: String, args: Array<String>, order: String): Cursor? =
        try {
            cr.query(SMS_URI, fullProjection, selection, args, order)
        } catch (_: Exception) {
            try {
                cr.query(SMS_URI, basicProjection, selection, args, order)
            } catch (_: Exception) {
                null
            }
        }

    private fun Cursor.toMessages(limit: Int = Int.MAX_VALUE): List<Message> {
        val out = ArrayList<Message>(minOf(count, limit))
        val iId = getColumnIndexOrThrow(SmsCol.ID)
        val iThread = getColumnIndexOrThrow(SmsCol.THREAD_ID)
        val iAddr = getColumnIndexOrThrow(SmsCol.ADDRESS)
        val iBody = getColumnIndexOrThrow(SmsCol.BODY)
        val iDate = getColumnIndexOrThrow(SmsCol.DATE)
        val iType = getColumnIndexOrThrow(SmsCol.TYPE)
        val iRead = getColumnIndexOrThrow(SmsCol.READ)
        val iStatus = getColumnIndex(SmsCol.STATUS)
        val iSub = getColumnIndex(SmsCol.SUB_ID)
        while (out.size < limit && moveToNext()) {
            val type = getInt(iType)
            if (type == SmsCol.TYPE_DRAFT) continue
            out += Message(
                id = getLong(iId),
                threadId = getLong(iThread),
                address = getString(iAddr).orEmpty(),
                body = getString(iBody).orEmpty(),
                date = getLong(iDate),
                type = type,
                read = getInt(iRead) == 1,
                status = if (iStatus >= 0 && !isNull(iStatus)) getInt(iStatus) else SmsCol.STATUS_NONE,
                subId = if (iSub >= 0 && !isNull(iSub)) getInt(iSub) else -1,
            )
        }
        return out
    }

    /** Newest [limit] messages of a thread, returned oldest-first. Second value: more exist. */
    fun loadMessages(threadId: Long, limit: Int = PAGE): Pair<List<Message>, Boolean> {
        if (!has(Manifest.permission.READ_SMS)) return emptyList<Message>() to false
        var more = false
        val list = queryMessages("${SmsCol.THREAD_ID} = ?", arrayOf(threadId.toString()), "${SmsCol.DATE} DESC")
            ?.use { c ->
                val l = c.toMessages(limit)
                more = !c.isAfterLast && !c.isLast && l.size >= limit
                l
            } ?: emptyList()
        return list.asReversed().toList() to more
    }

    fun unreadInThread(threadId: Long, limit: Int): List<Message> {
        if (!has(Manifest.permission.READ_SMS)) return emptyList()
        return queryMessages(
            "${SmsCol.THREAD_ID} = ? AND ${SmsCol.TYPE} = ${SmsCol.TYPE_INBOX} AND ${SmsCol.READ} = 0",
            arrayOf(threadId.toString()), "${SmsCol.DATE} DESC"
        )?.use { it.toMessages(limit) }?.reversed() ?: emptyList()
    }

    fun search(query: String, limit: Int = 40): List<SearchHit> {
        if (query.isBlank() || !has(Manifest.permission.READ_SMS)) return emptyList()
        val escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        val messages = queryMessages(
            "${SmsCol.BODY} LIKE ? ESCAPE '\\'", arrayOf("%$escaped%"), "${SmsCol.DATE} DESC"
        )?.use { it.toMessages(limit) } ?: emptyList()
        return messages.map {
            SearchHit(it.threadId, it.id, it.address, contacts.lookup(it.address).name, it.body, it.date)
        }
    }

    /** Reads every incoming SMS and extracts bank transactions. */
    fun scanTransactions(): List<Transaction> {
        if (!has(Manifest.permission.READ_SMS)) return emptyList()
        val out = ArrayList<Transaction>()
        try {
            cr.query(
                SMS_URI,
                arrayOf(SmsCol.ID, SmsCol.THREAD_ID, SmsCol.ADDRESS, SmsCol.BODY, SmsCol.DATE),
                "${SmsCol.TYPE} = ${SmsCol.TYPE_INBOX}", null, "${SmsCol.DATE} DESC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val body = c.getString(3) ?: continue
                    if (!BankParser.looksFinancial(body)) continue
                    BankParser.parse(c.getLong(0), c.getLong(1), c.getString(2).orEmpty(), body, c.getLong(4))
                        ?.let { out += it }
                }
            }
        } catch (_: Exception) {
        }
        transactionThreads = out.mapTo(HashSet()) { it.threadId }
        return out
    }

    fun headerFor(threadId: Long): ThreadHeader? {
        if (!has(Manifest.permission.READ_SMS)) return null
        return try {
            cr.query(
                SMS_URI, arrayOf(SmsCol.ADDRESS), "${SmsCol.THREAD_ID} = ?",
                arrayOf(threadId.toString()), "${SmsCol.DATE} DESC"
            )?.use { c ->
                var address = ""
                while (address.isBlank() && c.moveToNext()) address = c.getString(0).orEmpty()
                if (address.isBlank()) null
                else contacts.lookup(address).let { ThreadHeader(threadId, address, it.name, it.photoUri) }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun threadIdFor(address: String): Long =
        try {
            Telephony.Threads.getOrCreateThreadId(context, address)
        } catch (_: Exception) {
            -1L
        }

    // ---------------------------------------------------------------- writing
    // Only the default SMS app may write; other apps' writes are silently ignored by Android.

    fun markRead(threadId: Long) {
        try {
            val values = ContentValues().apply {
                put(SmsCol.READ, 1)
                put(SmsCol.SEEN, 1)
            }
            cr.update(
                SMS_URI, values, "${SmsCol.THREAD_ID} = ? AND (${SmsCol.READ} = 0 OR ${SmsCol.SEEN} = 0)",
                arrayOf(threadId.toString())
            )
        } catch (_: Exception) {
        }
    }

    fun deleteThreads(threadIds: Collection<Long>) {
        threadIds.forEach { id ->
            try {
                cr.delete(Uri.parse("content://sms/conversations/$id"), null, null)
            } catch (_: Exception) {
            }
        }
    }

    fun deleteMessage(id: Long) {
        try {
            cr.delete(ContentUris.withAppendedId(SMS_URI, id), null, null)
        } catch (_: Exception) {
        }
    }

    fun insertIncoming(address: String, body: String, dateSent: Long, subId: Int): Uri? = try {
        val values = ContentValues().apply {
            put(SmsCol.ADDRESS, address)
            put(SmsCol.BODY, body)
            put(SmsCol.DATE, System.currentTimeMillis())
            put(SmsCol.DATE_SENT, dateSent)
            put(SmsCol.READ, 0)
            put(SmsCol.SEEN, 0)
            put(SmsCol.TYPE, SmsCol.TYPE_INBOX)
            if (subId >= 0) put(SmsCol.SUB_ID, subId)
        }
        cr.insert(INBOX_URI, values)
    } catch (_: Exception) {
        null
    }

    fun insertOutgoing(address: String, body: String, threadId: Long, subId: Int): Uri? = try {
        val values = ContentValues().apply {
            put(SmsCol.ADDRESS, address)
            put(SmsCol.BODY, body)
            put(SmsCol.DATE, System.currentTimeMillis())
            put(SmsCol.READ, 1)
            put(SmsCol.SEEN, 1)
            put(SmsCol.TYPE, SmsCol.TYPE_OUTBOX)
            put(SmsCol.STATUS, SmsCol.STATUS_NONE)
            if (threadId > 0) put(SmsCol.THREAD_ID, threadId)
            if (subId >= 0) put(SmsCol.SUB_ID, subId)
        }
        cr.insert(OUTBOX_URI, values)
    } catch (_: Exception) {
        null
    }

    fun threadOf(uri: Uri): Long = try {
        cr.query(uri, arrayOf(SmsCol.THREAD_ID), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getLong(0) else -1L
        } ?: -1L
    } catch (_: Exception) {
        -1L
    }

    fun updateType(uri: Uri, type: Int) {
        try {
            cr.update(uri, ContentValues().apply { put(SmsCol.TYPE, type) }, null, null)
        } catch (_: Exception) {
        }
    }

    fun updateStatus(uri: Uri, status: Int) {
        try {
            cr.update(uri, ContentValues().apply { put(SmsCol.STATUS, status) }, null, null)
        } catch (_: Exception) {
        }
    }

    // ---------------------------------------------------------------- SIMs

    @SuppressLint("MissingPermission")
    fun sims(): List<SimInfo> {
        if (!has(Manifest.permission.READ_PHONE_STATE)) return emptyList()
        return try {
            val sm = context.getSystemService(SubscriptionManager::class.java)
            sm.activeSubscriptionInfoList.orEmpty()
                .sortedBy { it.simSlotIndex }
                .map {
                    SimInfo(
                        subId = it.subscriptionId,
                        label = it.displayName?.toString()?.takeIf { s -> s.isNotBlank() }
                            ?: "SIM ${it.simSlotIndex + 1}",
                        slot = it.simSlotIndex,
                        color = it.iconTint,
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun defaultSmsSubId(): Int = SubscriptionManager.getDefaultSmsSubscriptionId()
}
