package com.lumen.messages.data

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

enum class ThemeMode(val label: String) { System("Auto"), Dark("Espresso"), Black("Black"), Light("White") }
enum class BubbleStyle(val label: String) { Ink("Ink"), Accent("Colour"), Glass("Glass") }
enum class BubbleShape(val label: String) { Round("Round"), Soft("Square") }
enum class BlurLevel(val label: String, val scale: Float) { Subtle("Subtle", 0.55f), Medium("Medium", 1f), Strong("Strong", 1.6f) }

/** Accent colours offered in Settings → Theme colour. */
val AccentChoices: List<Long> = listOf(
    0xFFF5793B, // amber (default)
    0xFF3D7BFF, // blue
    0xFF7C5CFF, // violet
    0xFF14B8A6, // teal
    0xFF22C55E, // green
    0xFFFF8A3D, // orange
    0xFFFF4F8B, // pink
    0xFF8E8E93, // graphite
)

/**
 * User settings, persisted in SharedPreferences and observable by Compose.
 * Receivers read the same values, so notification behaviour follows the settings too.
 */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("lumen_settings", Context.MODE_PRIVATE)

    private inner class State<T>(initial: T, private val save: (T) -> Unit) : ReadWriteProperty<Any?, T> {
        private val state = mutableStateOf(initial)
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            state.value = value
            save(value)
        }
    }

    private fun bool(key: String, def: Boolean) =
        State(sp.getBoolean(key, def)) { sp.edit().putBoolean(key, it).apply() }

    private fun float(key: String, def: Float) =
        State(sp.getFloat(key, def)) { sp.edit().putFloat(key, it).apply() }

    private fun long(key: String, def: Long) =
        State(sp.getLong(key, def)) { sp.edit().putLong(key, it).apply() }

    private fun <E : Enum<E>> enumOf(key: String, def: E, values: Array<E>) =
        State(values.firstOrNull { it.name == sp.getString(key, def.name) } ?: def) {
            sp.edit().putString(key, it.name).apply()
        }

    // Look
    var themeMode: ThemeMode by enumOf("theme2", ThemeMode.Dark, ThemeMode.values())
    var accent: Long by long("accent2", AccentChoices[0])
    var bubbleStyle: BubbleStyle by enumOf("bubble_style2", BubbleStyle.Ink, BubbleStyle.values())
    var bubbleShape: BubbleShape by enumOf("bubble_shape", BubbleShape.Round, BubbleShape.values())
    var textScale: Float by float("text_scale", 1f)
    var blur: BlurLevel by enumOf("blur", BlurLevel.Medium, BlurLevel.values())
    var reduceMotion: Boolean by bool("reduce_motion", false)

    // Messages
    var smartTabs: Boolean by bool("smart_tabs", true)
    var deliveryReports: Boolean by bool("delivery_reports", true)

    // Notifications
    var notifReply: Boolean by bool("notif_reply", true)
    var notifMarkRead: Boolean by bool("notif_mark_read", true)
    var notifCopyCode: Boolean by bool("notif_copy_code", true)
    var notifShowContact: Boolean by bool("notif_show_contact", true)
    var notifShowText: Boolean by bool("notif_show_text", true)
    var inChatSound: Boolean by bool("in_chat_sound", true)
    var inChatVibrate: Boolean by bool("in_chat_vibrate", false)
}
