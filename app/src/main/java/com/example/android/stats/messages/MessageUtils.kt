package com.example.android.stats.messages

import android.content.Context
import android.database.Cursor
import android.provider.BaseColumns._ID
import android.provider.Telephony.*
import android.provider.Telephony.BaseMmsColumns.*
import android.provider.Telephony.Sms.ADDRESS
import android.provider.Telephony.TextBasedSmsColumns.*
import com.example.android.stats.timeRangeToContentProviderSelect
import java.time.LocalDateTime

enum class MessageStatus { SENT, RECEIVED, UNKNOWN }
enum class MessageType { SMS, MMS }

data class Message(val id: String, val type: MessageType, val sender: String, val status: MessageStatus)
data class IndividualMessageStats(val name: String, val totalReceived: Int, val totalSent: Int, val messages: List<Message>)

fun getMessages(context: Context, startDate: LocalDateTime? = null, endDate: LocalDateTime? = null): List<IndividualMessageStats> {
    val contentResolver = context.applicationContext.contentResolver
    val select = timeRangeToContentProviderSelect(TextBasedSmsColumns.DATE, null, null)
    val messages = arrayListOf<Message>()

    var project = arrayOf(_ID, ADDRESS, TYPE, TextBasedSmsColumns.DATE)
    contentResolver.query(Sms.CONTENT_URI, project, select.first, select.second.toTypedArray(), "${TextBasedSmsColumns.DATE} desc")?.use {
        while (it.moveToNext()) {
            val date = it.getLong(it.getColumnIndex(TextBasedSmsColumns.DATE))

            val messageType = MessageType.SMS
            messages.add(
                Message(
                    it.getString(it.getColumnIndex(_ID)),
                    messageType,
                    getAddress(messageType, it),
                    getStatus(messageType, it)
                )
            )
        }
    }

    project = arrayOf(_ID, MESSAGE_BOX, CONTENT_TYPE)
    contentResolver.query(Mms.CONTENT_URI, project, select.first, select.second.toTypedArray(), "${BaseMmsColumns.DATE} desc")?.use {
        while (it.moveToNext()) {
            val messageType = MessageType.MMS
            messages.add(
                Message(
                    it.getString(it.getColumnIndex(_ID)),
                    messageType,
                    getAddress(messageType, it),
                    getStatus(messageType, it)
                )
            )
        }
    }

    return messages
        .groupBy { it.sender }
        .map {
            IndividualMessageStats(
                it.key,
                it.value.count { x -> x.status == MessageStatus.RECEIVED },
                it.value.count { x -> x.status == MessageStatus.SENT },
                it.value
            )
        }
}

private fun getAddress(messageType: MessageType, cursor: Cursor): String {
    return if (messageType == MessageType.SMS) {
        val addressColumn = cursor.getColumnIndex(ADDRESS)
        cursor.getString(addressColumn)
    } else {
        // TODO
        ""
    }
}

private fun getStatus(messageType: MessageType, cursor: Cursor): MessageStatus {
    return if (messageType == MessageType.SMS) {
        val typeColumn = cursor.getColumnIndex(TextBasedSmsColumns.TYPE)
        when (cursor.getInt(typeColumn)) {
            MESSAGE_TYPE_INBOX -> MessageStatus.RECEIVED
            MESSAGE_TYPE_SENT -> MessageStatus.SENT
            else -> MessageStatus.UNKNOWN
        }
    } else {
        val typeColumn = cursor.getColumnIndex(BaseMmsColumns.MESSAGE_BOX)
        when (cursor.getInt(typeColumn)) {
            MESSAGE_BOX_INBOX -> MessageStatus.RECEIVED
            MESSAGE_BOX_SENT -> MessageStatus.SENT
            else -> MessageStatus.UNKNOWN
        }
    }
}