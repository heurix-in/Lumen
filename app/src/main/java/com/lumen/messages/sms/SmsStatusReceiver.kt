package com.lumen.messages.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsMessage
import com.lumen.messages.data.SmsCol
import com.lumen.messages.lumen
import kotlinx.coroutines.launch

/** Updates a message to Sent / Failed / Delivered when the radio reports back. */
class SmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val uriString = intent.getStringExtra(SmsSender.EXTRA_URI) ?: return
        val uri = Uri.parse(uriString)
        val last = intent.getBooleanExtra(SmsSender.EXTRA_LAST, true)
        val code = resultCode
        val repo = context.lumen.repository
        val pending = goAsync()
        context.lumen.appScope.launch {
            try {
                when (intent.action) {
                    SmsSender.ACTION_SENT -> {
                        if (code == Activity.RESULT_OK) {
                            if (last) repo.updateType(uri, SmsCol.TYPE_SENT)
                        } else {
                            repo.updateType(uri, SmsCol.TYPE_FAILED)
                        }
                    }
                    SmsSender.ACTION_DELIVERED -> if (last) {
                        val pdu = intent.getByteArrayExtra("pdu")
                        val format = intent.getStringExtra("format")
                        val status = pdu?.let { runCatching { SmsMessage.createFromPdu(it, format)?.status }.getOrNull() }
                        val mapped = when {
                            status == null -> SmsCol.STATUS_COMPLETE
                            status == 0 -> SmsCol.STATUS_COMPLETE
                            status >= 64 -> SmsCol.STATUS_FAILED
                            else -> SmsCol.STATUS_PENDING
                        }
                        repo.updateStatus(uri, mapped)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
