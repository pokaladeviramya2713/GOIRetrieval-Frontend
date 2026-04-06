package com.simats.goiretrieval.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simats.goiretrieval.utils.NotificationHelper

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Trigger the pop-up notification
        val title = "GOI Retrieval Update"
        val message = "Check for any new policy changes or document additions in the search library."
        
        NotificationHelper.showNotification(context, title, message)
    }
}
