package com.lumen.messages.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.lumen.messages.R
import com.lumen.messages.lumen
import com.lumen.messages.ui.findOtp
import com.lumen.messages.ui.MainActivity

/** Tracks which conversation is on screen so we don't notify for it. */
object Foreground {
    @Volatile
    var visibleThread: Long = -1L
}

object Notifier {
    const val CHANNEL = "messages"
    const val CHANNEL_INFO = "info"
    const val KEY_REPLY = "reply_text"
    const val EXTRA_THREAD = "thread_id"
    const val EXTRA_ADDRESS = "address"
    const val EXTRA_CODE = "code"
    private const val MMS_NOTIFICATION_ID = 0x4D4D53

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Incoming text messages"
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFF7C5CFF.toInt()
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_INFO, "App notices", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notices about messages Lumen can't show yet"
            }
        )
    }

    private fun allowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private val mutableFlag: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

    /** Shows (or refreshes) the notification for a conversation's unread messages. Call off the main thread. */
    @SuppressLint("MissingPermission")
    fun showThread(context: Context, threadId: Long) {
        if (!allowed(context)) return
        val repo = context.lumen.repository
        val unread = repo.unreadInThread(threadId, 7)
        if (unread.isEmpty()) {
            cancel(context, threadId)
            return
        }
        val prefs = context.lumen.prefs
        val address = unread.last().address
        val contact = repo.contacts.lookup(address)
        val displayName = if (prefs.notifShowContact) (contact.name ?: address) else "New message"
        val sender = Person.Builder()
            .setName(displayName)
            .setKey(address)
            .setImportant(contact.name != null)
            .build()
        val me = Person.Builder().setName("You").build()
        val style = NotificationCompat.MessagingStyle(me)
        unread.forEach {
            style.addMessage(if (prefs.notifShowText) it.body else "New message", it.date, sender)
        }

        val id = threadId.toInt()
        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_THREAD, threadId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_message)
            .setColor(prefs.accent.toInt())
            .setStyle(style)
            .setContentTitle(displayName)
            .setContentText(if (prefs.notifShowText) unread.last().body else "New message")
            .setWhen(unread.last().date)
            .setShowWhen(true)
            .setNumber(unread.size)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)

        val code = findOtp(unread.last().body)
        if (code != null && prefs.notifCopyCode) {
            val copyIntent = PendingIntent.getBroadcast(
                context, id,
                Intent(context, NotificationActionReceiver::class.java)
                    .setAction(NotificationActionReceiver.ACTION_COPY)
                    .putExtra(EXTRA_THREAD, threadId)
                    .putExtra(EXTRA_CODE, code),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(
                NotificationCompat.Action.Builder(R.drawable.ic_done, "Copy $code", copyIntent)
                    .setShowsUserInterface(false)
                    .build()
            )
        }

        if (prefs.notifReply) {
            val replyIntent = PendingIntent.getBroadcast(
                context, id,
                Intent(context, NotificationActionReceiver::class.java)
                    .setAction(NotificationActionReceiver.ACTION_REPLY)
                    .putExtra(EXTRA_THREAD, threadId)
                    .putExtra(EXTRA_ADDRESS, address),
                mutableFlag or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val remoteInput = RemoteInput.Builder(KEY_REPLY).setLabel("Reply").build()
            builder.addAction(
                NotificationCompat.Action.Builder(R.drawable.ic_reply, "Reply", replyIntent)
                    .addRemoteInput(remoteInput)
                    .setAllowGeneratedReplies(true)
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                    .setShowsUserInterface(false)
                    .build()
            )
        }

        if (prefs.notifMarkRead) {
            val readIntent = PendingIntent.getBroadcast(
                context, id,
                Intent(context, NotificationActionReceiver::class.java)
                    .setAction(NotificationActionReceiver.ACTION_MARK_READ)
                    .putExtra(EXTRA_THREAD, threadId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(
                NotificationCompat.Action.Builder(R.drawable.ic_done, "Mark as read", readIntent)
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                    .setShowsUserInterface(false)
                    .build()
            )
        }
        val notification = builder.build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    fun showMmsUnsupported(context: Context) {
        if (!allowed(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_INFO)
            .setSmallIcon(R.drawable.ic_stat_message)
            .setColor(0xFF7C5CFF.toInt())
            .setContentTitle("Picture message received")
            .setContentText("Lumen can't open picture/group messages (MMS) yet.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Someone sent you a picture or group message (MMS). Lumen can't download these yet. " +
                        "To view it, temporarily switch your default SMS app back to your phone's Messages app."
                )
            )
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(MMS_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    /** Soft sound / buzz when a message arrives in the conversation that is open on screen. */
    fun inChatAlert(context: Context) {
        val prefs = context.lumen.prefs
        if (prefs.inChatSound) {
            runCatching {
                RingtoneManager.getRingtone(
                    context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                )?.play()
            }
        }
        if (prefs.inChatVibrate) {
            runCatching {
                context.getSystemService(Vibrator::class.java)
                    ?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    fun cancel(context: Context, threadId: Long) {
        NotificationManagerCompat.from(context).cancel(threadId.toInt())
    }
}
