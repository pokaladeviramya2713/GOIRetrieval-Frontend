package com.simats.goiretrieval

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "app_prefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_EMPLOYEE_ID = "employee_id"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_PREF_THEME = "pref_theme"
        private const val KEY_SHOW_STATS = "show_stats"
        private const val KEY_SHOW_CHART = "show_chart"
        private const val KEY_SHOW_ACTIVITY = "show_activity"
        private const val KEY_2FA_ENABLED = "2fa_enabled"
        private const val KEY_NOTIF_PUSH = "notif_push"
        private const val KEY_NOTIF_EMAIL = "notif_email"
        private const val KEY_NOTIF_SMS = "notif_sms"
        private const val KEY_NOTIF_POLICY = "notif_policy"
        private const val KEY_NOTIF_SCHEME = "notif_scheme"
        private const val KEY_NOTIF_AI = "notif_ai"
        private const val KEY_NOTIF_SYSTEM = "notif_system"
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun saveUserSession(userId: Int, token: String, name: String? = null, email: String? = null, empId: String? = null, role: String? = null) {
        prefs.edit().apply {
            putInt(KEY_USER_ID, userId)
            putString(KEY_AUTH_TOKEN, token)
            name?.let { putString(KEY_USER_NAME, it) }
            email?.let { putString(KEY_USER_EMAIL, it) }
            empId?.let { putString(KEY_EMPLOYEE_ID, it) }
            role?.let { putString(KEY_USER_ROLE, it) }
            apply()
        }
    }

    fun updateUserInfo(name: String, email: String, empId: String? = null, role: String? = null) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            empId?.let { putString(KEY_EMPLOYEE_ID, it) }
            role?.let { putString(KEY_USER_ROLE, it) }
            apply()
        }
    }

    fun getUserId(): Int {
        return prefs.getInt(KEY_USER_ID, -1)
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun getEmployeeId(): String? {
        return prefs.getString(KEY_EMPLOYEE_ID, null)
    }

    fun getRole(): String? {
        return prefs.getString(KEY_USER_ROLE, "Analyst")
    }

    fun isLoggedIn(): Boolean {
        return getUserId() != -1
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    // Preferences
    fun savePreferences(theme: String, showStats: Boolean, showChart: Boolean, showActivity: Boolean) {
        prefs.edit().apply {
            putString(KEY_PREF_THEME, theme)
            putBoolean(KEY_SHOW_STATS, showStats)
            putBoolean(KEY_SHOW_CHART, showChart)
            putBoolean(KEY_SHOW_ACTIVITY, showActivity)
            apply()
        }
    }

    fun getThemeMode(): String = prefs.getString(KEY_PREF_THEME, "light") ?: "light"
    fun shouldShowStats(): Boolean = prefs.getBoolean(KEY_SHOW_STATS, true)
    fun shouldShowChart(): Boolean = prefs.getBoolean(KEY_SHOW_CHART, true)
    fun shouldShowActivity(): Boolean = prefs.getBoolean(KEY_SHOW_ACTIVITY, true)

    fun is2FAEnabled(): Boolean = prefs.getBoolean(KEY_2FA_ENABLED, false)
    fun set2FAEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_2FA_ENABLED, enabled).apply()
    }

    fun getNotificationPrefs(): Triple<Boolean, Boolean, Boolean> {
        val push = prefs.getBoolean(KEY_NOTIF_PUSH, true)
        val email = prefs.getBoolean(KEY_NOTIF_EMAIL, false)
        val sms = prefs.getBoolean(KEY_NOTIF_SMS, false)
        return Triple(push, email, sms)
    }

    fun saveNotificationPrefs(push: Boolean, email: Boolean, sms: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_NOTIF_PUSH, push)
            putBoolean(KEY_NOTIF_EMAIL, email)
            putBoolean(KEY_NOTIF_SMS, sms)
            apply()
        }
    }

    fun getTopicNotifications(): Map<String, Boolean> {
        return mapOf(
            "policy" to prefs.getBoolean(KEY_NOTIF_POLICY, true),
            "scheme" to prefs.getBoolean(KEY_NOTIF_SCHEME, true),
            "ai" to prefs.getBoolean(KEY_NOTIF_AI, false),
            "system" to prefs.getBoolean(KEY_NOTIF_SYSTEM, true)
        )
    }

    fun saveTopicNotifications(policy: Boolean, scheme: Boolean, ai: Boolean, system: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_NOTIF_POLICY, policy)
            putBoolean(KEY_NOTIF_SCHEME, scheme)
            putBoolean(KEY_NOTIF_AI, ai)
            putBoolean(KEY_NOTIF_SYSTEM, system)
            apply()
        }
    }
}
