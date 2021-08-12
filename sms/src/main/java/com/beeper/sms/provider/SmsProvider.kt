package com.beeper.sms.provider

import android.content.Context
import android.net.Uri
import android.provider.Telephony.Sms.*
import android.util.Log
import com.beeper.sms.commands.outgoing.Message
import com.beeper.sms.extensions.*
import com.beeper.sms.provider.ThreadProvider.Companion.chatGuid


class SmsProvider constructor(context: Context) {
    private val packageName = context.applicationInfo.packageName
    private val cr = context.contentResolver

    fun getMessage(uri: Uri) = uri.lastPathSegment?.toLongOrNull()?.let { getMessage(it) }

    fun getMessage(id: Long) = getSms(where = "_id = $id").firstOrNull()

    private fun getSms(where: String? = null): List<Message> =
        cr.map(CONTENT_URI, where) {
            val address = it.getString(ADDRESS)
            if (address == null) {
                // TODO: try to lookup address w/ThreadProvider?
                Log.w(TAG, "Missing address: ${it.dumpCurrentRow()}")
                return@map null
            }
            val isFromMe = when (it.getInt(TYPE)) {
                MESSAGE_TYPE_OUTBOX, MESSAGE_TYPE_SENT -> true
                else -> false
            }
            val chatGuid = address.chatGuid
            Message(
                guid = it.getInt(_ID).toString(),
                timestamp = it.getLong(DATE) / 1000,
                subject = it.getString(SUBJECT) ?: "",
                text = it.getString(BODY) ?: "",
                chat_guid = chatGuid,
                sender_guid = if (isFromMe) null else chatGuid,
                is_from_me = isFromMe,
                sent_from_matrix = it.getString(CREATOR) == packageName
            )
        }

    companion object {
        private const val TAG = "SmsProvider"
    }
}