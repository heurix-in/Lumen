package com.lumen.messages.sms

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import com.lumen.messages.data.SmsCol
import com.lumen.messages.lumen

/** Sends SMS through the SIM. Uses the phone's cellular SMS channel, not the internet. */
object SmsSender {
    const val ACTION_SENT = "com.lumen.messages.SMS_SENT"
    const val ACTION_DELIVERED = "com.lumen.messages.SMS_DELIVERED"
    const val EXTRA_URI = "uri"
    const val EXTRA_LAST = "last"

    @Suppress("DEPRECATION")
    private fun manager(context: Context, subId: Int): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val base = context.getSystemService(SmsManager::class.java)
            if (subId >= 0) base.createForSubscriptionId(subId) else base
        } else {
            if (subId >= 0) SmsManager.getSmsManagerForSubscriptionId(subId) else SmsManager.getDefault()
        }

    /** Must be called off the main thread. Returns false if the message could not be handed to the radio. */
    fun send(context: Context, address: String, body: String, subId: Int): Boolean {
        val repo = context.lumen.repository
        val effectiveSub = if (subId >= 0) subId else repo.defaultSmsSubId()
        val threadId = repo.threadIdFor(address)
        val uri = repo.insertOutgoing(address, body, threadId, effectiveSub)
        val messageId = uri?.let { runCatching { ContentUris.parseId(it) }.getOrNull() } ?: System.currentTimeMillis()

        return try {
            val sms = manager(context, effectiveSub)
            val parts = sms.divideMessage(body)
            val sent = ArrayList<PendingIntent>(parts.size)
            val delivered = ArrayList<PendingIntent>(parts.size)
            for (i in parts.indices) {
                val last = i == parts.lastIndex
                val base = (messageId * 16 + i).toInt()
                sent += PendingIntent.getBroadcast(
                    context, base,
                    Intent(context, SmsStatusReceiver::class.java)
                        .setAction(ACTION_SENT)
                        .putExtra(EXTRA_URI, uri?.toString())
                        .putExtra(EXTRA_LAST, last),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                delivered += PendingIntent.getBroadcast(
                    context, base,
                    Intent(context, SmsStatusReceiver::class.java)
                        .setAction(ACTION_DELIVERED)
                        .putExtra(EXTRA_URI, uri?.toString())
                        .putExtra(EXTRA_LAST, last),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            val reports = context.lumen.prefs.deliveryReports
            if (parts.size <= 1) {
                sms.sendTextMessage(address, null, body, sent.firstOrNull(), if (reports) delivered.firstOrNull() else null)
            } else {
                sms.sendMultipartTextMessage(address, null, parts, sent, if (reports) delivered else null)
            }
            true
        } catch (e: Exception) {
            uri?.let { repo.updateType(it, SmsCol.TYPE_FAILED) }
            false
        }
    }
}
