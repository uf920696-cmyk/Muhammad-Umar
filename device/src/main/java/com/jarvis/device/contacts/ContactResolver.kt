package com.jarvis.device.contacts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.jarvis.core.common.ToolResult

data class ContactMatch(
    val contactId: String,
    val displayName: String,
    val phoneNumber: String,
    val normalizedNumber: String
)

/**
 * CONTACT RESOLVER:
 * Queries Android ContactsProvider to resolve spoken names (e.g., "Ali", "Mom")
 * to validated phone numbers with country code normalization.
 * Enforces Absolute Honesty:
 * - If READ_CONTACTS permission is missing, returns ToolResult.NeedsPermission.
 * - If multiple matching contacts exist, returns details asking for clarification.
 */
class ContactResolver(private val context: Context) {

    companion object {
        private const val TAG = "ContactResolver"
    }

    fun resolveContactByName(spokenName: String): Pair<ToolResult?, List<ContactMatch>> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CONTACTS permission not granted")
            return Pair(
                ToolResult.NeedsPermission(
                    action = "resolve_contact",
                    permission = Manifest.permission.READ_CONTACTS
                ),
                emptyList()
            )
        }

        val trimmed = spokenName.trim()
        if (trimmed.isBlank()) {
            return Pair(
                ToolResult.Failed("Spoken contact name cannot be empty", recoverable = false),
                emptyList()
            )
        }

        val matches = mutableListOf<ContactMatch>()
        val uri: Uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
        )

        // Case-insensitive name match
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$trimmed%")

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.let {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val normNumIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)

                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getString(idIdx) else ""
                    val name = if (nameIdx != -1) it.getString(nameIdx) else ""
                    val number = if (numIdx != -1) it.getString(numIdx) else ""
                    val norm = if (normNumIdx != -1 && it.getString(normNumIdx) != null) it.getString(normNumIdx) else normalizeNumber(number)

                    if (number.isNotBlank()) {
                        matches.add(
                            ContactMatch(
                                contactId = id,
                                displayName = name,
                                phoneNumber = number,
                                normalizedNumber = norm
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying contacts: ${e.message}")
            return Pair(ToolResult.Failed("Failed to query contacts: ${e.message}", recoverable = true), emptyList())
        } finally {
            cursor?.close()
        }

        return Pair(null, matches)
    }

    private fun normalizeNumber(raw: String): String {
        return raw.replace("[^0-9+]".toRegex(), "")
    }
}
