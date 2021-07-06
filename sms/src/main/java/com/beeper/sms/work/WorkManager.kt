package com.beeper.sms.work

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WorkManager @Inject constructor(@ApplicationContext context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun sendMessage(uri: Uri) {
        Log.d(TAG, uri.toString())
        OneTimeWorkRequest
            .Builder(SendMessage::class.java)
            .setInputData(Data.Builder().putString(SendMessage.URI, uri.toString()).build())
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
            .apply { workManager.enqueue(this) }
    }

    companion object {
        private const val TAG = "WorkManager"
    }
}