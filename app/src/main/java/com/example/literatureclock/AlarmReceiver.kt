package com.example.literatureclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.literatureclock.UPDATE_TIME") {
            // Send a broadcast to the Activity to update the UI
            val updateIntent = Intent("com.example.literatureclock.UI_UPDATE_NEEDED")
            context.sendBroadcast(updateIntent)
        }
    }
}
