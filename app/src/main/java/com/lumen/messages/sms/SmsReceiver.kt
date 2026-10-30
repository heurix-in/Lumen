package com.lumen.messages.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SubscriptionManager
import com.lumen.messages.lumen
import com.lumen.messages.notify.Foreground
import com.lumen.messages.notify.Notifier
import kotlinx.coroutines.launch

/**
 * Receives incoming SMS. As the default SMS app, Lumen is responsible for saving
 * each incoming message into the phone's SMS store.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val parts = runCatching { Telephony.Sms.Intents.getMessagesFromIntent(intent) }.getOrNull()
        if (parts.isNullOrEmpty()) return
        val first = parts[0] ?: return
        val address = first.displayOriginatingAddress ?: first.originatingAddress ?: ""
        val body = parts.joinToString("") { it?.displayMessageBody ?: it?.messageBody ?: "" }
        val sentAt = first.timestampMillis
        val subId = intent.getIntExtra(
            SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
            intent.getIntExtra("subscription", -1)
        )

        val app = context.lumen
        val pending = goAsync()
        app.appScope.launch {
            try {
                val uri = app.repository.insertIncoming(address, body, sentAt, subId)
                val threadId = uri?.let { app.repository.threadOf(it) } ?: -1L
                if (threadId > 0 && threadId != Foreground.visibleThread) {
                    Notifier.showThread(app, threadId)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
