package com.beeper.sms.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.beeper.sms.Bridge

class DefaultSmsAppReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Telephony.Sms.Intents.ACTION_EXTERNAL_PROVIDER_CHANGE,
            Telephony.Sms.Intents.ACTION_DEFAULT_SMS_PACKAGE_CHANGED ->
                Bridge.INSTANCE.start(context)
            else ->
                Log.d(TAG, "Ignoring broadcast: ${intent?.action}")
        }
    }

    companion object {
        private const val TAG = "DefaultSmsAppReceiver"
    }
}