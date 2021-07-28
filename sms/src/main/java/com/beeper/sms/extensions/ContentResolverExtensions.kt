package com.beeper.sms.extensions

import android.content.ContentResolver
import android.content.ContentValues.TAG
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.util.Log
import androidx.core.database.getStringOrNull
import com.beeper.sms.BuildConfig

fun <T> ContentResolver.flatMap(
    uri: Uri,
    where: String? = null,
    projection: Array<String>? = null,
    order: String? = null,
    block: (Cursor) -> List<T>?
): List<T> {
    val result = ArrayList<T>()
    map(uri, where, projection, order) {
        block(it)?.let { l -> result.addAll(l)}
    }
    return result
}

fun <T> ContentResolver.map(
    uri: Uri,
    where: String? = null,
    projection: Array<String>? = null,
    order: String? = null,
    block: (Cursor) -> T?
): List<T> {
    val result = ArrayList<T>()
    query(uri, projection, where, null, order)?.use {
        Log.v(TAG, "$uri where=$where: ${it.count} results\n${it.dumpToString()}")
        while (it.moveToNext()) {
            block(it)?.let { t -> result.add(t) }
        }
    }
    return result
}

fun <T> ContentResolver.firstOrNull(
    uri: Uri,
    where: String? = null,
    projection: Array<String>? = null,
    order: String? = null,
    block: (Cursor) -> T?
): T? = map(uri, where, projection, order, block).firstOrNull()

private fun Cursor.dumpToString(): String {
    val sb = StringBuilder()
    val startPos: Int = position
    moveToPosition(-1)
    while (moveToNext()) {
        sb.append("$position { ${dumpCurrentRow().joinToString(DELIM)} }\n")
    }
    moveToPosition(startPos)
    return sb.toString()
}

private fun Cursor.dumpCurrentRow(): List<String> =
    columnNames.mapIndexed { i, name ->
        val value = try {
            getStringOrNull(i)
        } catch (e: SQLiteException) {
            // assume that if the getString threw this exception then the column is not
            // representable by a string, e.g. it is a BLOB.
            "<unprintable>"
        }
        "$name=${when {
            value == null -> "<null>"
            REDACT.contains(name) -> if (BuildConfig.DEBUG) value else "<redacted>"
            else -> value
        }}"
    }

private val DELIM = Char(0x1f).toString()
private val REDACT = listOf(
    "body",
    "subject",
    "snippet",
    "text",
)