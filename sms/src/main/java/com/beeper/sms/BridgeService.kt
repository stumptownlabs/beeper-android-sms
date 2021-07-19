package com.beeper.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.beeper.sms.extensions.isDefaultSmsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors.newSingleThreadExecutor

class BridgeService : Service() {

    private val dispatcher = newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isDefaultSmsApp) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        Log.d(TAG, "starting service")
        val notificationChannel =
            intent?.getStringExtra(CHANNEL_ID) ?: throw RuntimeException("Missing channel_id")
        startForeground(
            ONGOING_NOTIFICATION_ID,
            NotificationCompat.Builder(this, notificationChannel)
                .setSound(null)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(intent.getIntExtra(CHANNEL_ICON, DEFAULT_CHANNEL_ICON))
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_body))
                .build()
        )
        val commandProcessor = CommandProcessor(applicationContext)
        scope.launch {
            Bridge.INSTANCE.stdout.forEachLine {
                if (it.startsWith("{") && it.endsWith("}")) {
                    Log.d(TAG, "receive: $it")
                    commandProcessor.handle(it)
                } else {
                    Log.d(TAG, it)
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "BridgeService"
        private const val ONGOING_NOTIFICATION_ID = 10681
        private const val CHANNEL_ID = "channel_id"
        private const val CHANNEL_ICON = "channel_icon"
        private val DEFAULT_CHANNEL_ICON = R.drawable.ic_status_bar_beeper

        internal fun Context.startBridge(channelId: String, channelIcon: Int? = null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(channelId)
            }
            ContextCompat.startForegroundService(
                this,
                Intent(this, BridgeService::class.java)
                    .putExtra(CHANNEL_ID, channelId)
                    .putExtra(CHANNEL_ICON, channelIcon)
            )
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun Context.createNotificationChannel(channelId: String) =
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(
                    NotificationChannel(
                        channelId,
                        getString(R.string.notification_channel),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        enableLights(false)
                        enableVibration(false)
                        setBypassDnd(false)
                        setShowBadge(false)
                    }
                )
    }
}