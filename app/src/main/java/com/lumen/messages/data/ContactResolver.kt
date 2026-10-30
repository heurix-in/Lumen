package com.lumen.messages.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import java.util.concurrent.ConcurrentHashMap

/** Turns phone numbers into contact names and photos, with an in-memory cache. */
class ContactResolver(private val context: Context) {

    data class Resolved(val name: String?, val photoUri: String?)

    private val cache = ConcurrentHashMap<String, Resolved>()
    private val none = Resolved(null, null)

    fun canRead(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    fun lookup(address: String): Resolved {
        if (address.isBlank()) return none
        cache[address]?.let { return it }
        if (!canRead()) return none
        var result = none
        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address))
            context.contentResolver.query(
                uri, arrayOf("display_name", "photo_thumb_uri"), null, null, null
            )?.use { c ->
                if (c.moveToFirst()) result = Resolved(c.getString(0), c.getString(1))
            }
        } catch (_: Exception) {
            // Alphanumeric senders (e.g. "BANK") or odd numbers simply have no contact.
        }
        cache[address] = result
        return result
    }

    fun invalidate() = cache.clear()

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
                    val key = name + "|" + digits.takeLast(9)
                    if (!seen.add(key)) continue
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
