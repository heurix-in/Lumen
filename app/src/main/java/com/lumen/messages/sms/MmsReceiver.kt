package com.lumen.messages.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lumen.messages.notify.Notifier

/**
 * Required for the default SMS role. Picture/group messages (MMS) are not supported
 * in this version, so we tell the user one arrived instead of dropping it silently.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notifier.showMmsUnsupported(context)
    }
}
