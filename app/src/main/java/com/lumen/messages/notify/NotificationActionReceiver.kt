package com.lumen.messages.notify

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.lumen.messages.lumen
import com.lumen.messages.sms.SmsSender
import kotlinx.coroutines.launch

/** Handles "Reply" and "Mark as read" straight from the notification. */
class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_REPLY = "com.lumen.messages.REPLY"
        const val ACTION_MARK_READ = "com.lumen.messages.MARK_READ"
        const val ACTION_COPY = "com.lumen.messages.COPY_CODE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(Notifier.EXTRA_THREAD, -1L)
        if (threadId < 0) return
        val app = context.lumen
        if (intent.action == ACTION_COPY) {
            val code = intent.getStringExtra(Notifier.EXTRA_CODE) ?: return
            val cm = context.getSystemService(ClipboardManager::class.java)
            cm?.setPrimaryClip(ClipData.newPlainText("Code", code))
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, "Copied $code", Toast.LENGTH_SHORT).show()
            }
            return
        }
        val pending = goAsync()
        app.appScope.launch {
            try {
                when (intent.action) {
                    ACTION_REPLY -> {
                        val text = RemoteInput.getResultsFromIntent(intent)
                            ?.getCharSequence(Notifier.KEY_REPLY)?.toString()?.trim()
                        val address = intent.getStringExtra(Notifier.EXTRA_ADDRESS)
                        if (!text.isNullOrEmpty() && !address.isNullOrBlank()) {
                            SmsSender.send(app, address, text, -1)
                        }
                        app.repository.markRead(threadId)
                        Notifier.cancel(app, threadId)
                    }
                    ACTION_MARK_READ -> {
                        app.repository.markRead(threadId)
                        Notifier.cancel(app, threadId)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
