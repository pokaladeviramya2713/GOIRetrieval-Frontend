package com.simats.goiretrieval

import android.content.Context
import android.content.SharedPreferences

class NotificationPrefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

    fun getLastNotifiedId(): Int {
        return prefs.getInt("last_notified_id", -1)
    }

    fun setLastNotifiedId(id: Int) {
        prefs.edit().putInt("last_notified_id", id).apply()
    }

    // New methods for compatibility with NotificationsActivity
    fun setUnreadNotifications(hasUnread: Boolean) {
        prefs.edit().putBoolean("has_unread", hasUnread).apply()
    }

    fun hasUnreadNotifications(): Boolean {
        return prefs.getBoolean("has_unread", false)
    }

    fun markNotificationsRead() {
        prefs.edit().putBoolean("has_unread", false).apply()
    }

    companion object {
        @Volatile
        private var instance: NotificationPrefs? = null

        fun getInstance(context: Context): NotificationPrefs {
            return instance ?: synchronized(this) {
                instance ?: NotificationPrefs(context).also { instance = it }
            }
        }

        // Static helpers for compatibility
        fun setUnreadNotifications(context: Context, hasUnread: Boolean) {
            getInstance(context).setUnreadNotifications(hasUnread)
        }

        fun markNotificationsRead(context: Context) {
            getInstance(context).markNotificationsRead()
        }
    }
}
