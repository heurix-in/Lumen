package com.lumen.messages.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

/**
 * Turns phone numbers into contact names and photos.
 * All contacts are read in ONE query and kept in memory, so resolving hundreds of
 * conversations is instant (the old per-number lookup was the main cause of slow loading).
 */
class ContactResolver(private val context: Context) {

    data class Resolved(val name: String?, val photoUri: String?)

    private val none = Resolved(null, null)

    @Volatile
    private var byNumber: Map<String, Resolved>? = null

    fun canRead(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    /** Last 8 digits identify a number regardless of country code or formatting. */
    private fun key(number: String): String? {
        val digits = number.filter { it.isDigit() }
        return if (digits.length >= 6) digits.takeLast(8) else null
    }

    fun lookup(address: String): Resolved {
        val k = key(address) ?: return none
        val map = byNumber ?: load()
        return map[k] ?: none
    }

    @Synchronized
    private fun load(): Map<String, Resolved> {
        byNumber?.let { return it }
        val out = HashMap<String, Resolved>()
        if (canRead()) {
            try {
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf("display_name", "data1", "photo_thumb_uri"),
                    null, null, null
                )?.use { c ->
                    while (c.moveToNext()) {
                        val name = c.getString(0) ?: continue
                        val k = key(c.getString(1) ?: continue) ?: continue
                        val existing = out[k]
                        if (existing == null || (existing.photoUri == null && c.getString(2) != null)) {
                            out[k] = Resolved(name, c.getString(2))
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        byNumber = out
        return out
    }

    fun invalidate() {
        byNumber = null
    }

    fun allContacts(): List<ContactEntry> {
        if (!canRead()) return emptyList()
        val out = ArrayList<ContactEntry>()
        val seen = HashSet<String>()
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf("display_name", "data1", "photo_thumb_uri", "data2", "data3"),
                null, null, "display_name COLLATE NOCASE ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(0) ?: continue
                    val number = c.getString(1) ?: continue
                    val digits = number.filter { it.isDigit() }
                    if (!seen.add(name + "|" + digits.takeLast(9))) continue
                    val type = if (c.isNull(3)) 0 else c.getInt(3)
                    val label = ContactsContract.CommonDataKinds.Phone
                        .getTypeLabel(context.resources, type, c.getString(4)).toString()
                    out += ContactEntry(name, number, label, c.getString(2))
                }
            }
        } catch (_: Exception) {
        }
        return out
    }
}
