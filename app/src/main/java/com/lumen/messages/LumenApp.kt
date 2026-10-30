package com.lumen.messages

import android.app.Application
import android.content.Context
import com.lumen.messages.data.SmsRepository
import com.lumen.messages.notify.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class LumenApp : Application() {
    /** Background work started from receivers that must outlive them. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val repository: SmsRepository by lazy { SmsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        Notifier.ensureChannel(this)
    }
}

val Context.lumen: LumenApp get() = applicationContext as LumenApp
