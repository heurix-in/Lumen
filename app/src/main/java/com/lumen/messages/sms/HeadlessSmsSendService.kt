package com.lumen.messages.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.lumen.messages.lumen
import kotlinx.coroutines.launch

/** Handles "reply with a message" from the incoming-call screen. */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val recipients = intent?.data?.schemeSpecificPart
            ?.substringBefore('?')
            ?.split(',', ';')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        if (!text.isNullOrBlank() && recipients.isNotEmpty()) {
            lumen.appScope.launch {
                recipients.forEach { SmsSender.send(applicationContext, it, text, -1) }
                stopSelf(startId)
            }
        } else {
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }
}
