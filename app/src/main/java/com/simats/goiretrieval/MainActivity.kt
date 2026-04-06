package com.simats.goiretrieval

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import com.simats.goiretrieval.api.NotificationsResponse
import com.simats.goiretrieval.api.RetrofitClient
import com.simats.goiretrieval.utils.NotificationHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import android.app.AlarmManager
import android.content.Context
import com.simats.goiretrieval.utils.ReminderReceiver
import android.content.Intent
import android.app.PendingIntent

class MainActivity : AppCompatActivity() {

    private var currentTab = 0
    private val navigationHistory = mutableListOf<Int>()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var notificationRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        setupBottomNavigation()
        
        // Initialize Notification pop-ups
        NotificationHelper.init(this)
        requestNotificationPermission()
        startNotificationPolling()
        scheduleRecurringReminder()

        // Handle Top Window Insets (Status Bar)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragment_container)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        // Handle Bottom Window Insets (Navigation Bar)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_nav)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        
        // Handle Intent Extra for specific tab selection
        val selectFragment = intent.getStringExtra("SELECT_FRAGMENT")
        if (selectFragment != null) {
            val tabIndex = when (selectFragment) {
                "home" -> 0
                "search" -> 1
                "docs" -> 2
                "ai_assist" -> 3
                "profile" -> 4
                else -> 0
            }
            switchToTab(tabIndex, addToHistory = false)
        } else if (savedInstanceState == null) {
            switchToTab(0, addToHistory = false)
        }

        // Silent Sync of Saved Docs for consistent bookmarking
        fetchSavedDocsSilently()

        // Modern OnBackPressed handling
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navigationHistory.isNotEmpty()) {
                    val lastIndex = navigationHistory.removeAt(navigationHistory.size - 1)
                    switchToTab(lastIndex, addToHistory = false)
                } else {
                    if (currentTab != 0) {
                        switchToTab(0, addToHistory = false)
                    } else {
                        isEnabled = false // Disable this callback to allow system back
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        val selectFragment = intent?.getStringExtra("SELECT_FRAGMENT")
        if (selectFragment != null) {
            val tabIndex = when (selectFragment) {
                "home" -> 0
                "search" -> 1
                "docs" -> 2
                "ai_assist" -> 3
                "profile" -> 4
                else -> 0
            }
            switchToTab(tabIndex, addToHistory = false)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun startNotificationPolling() {
        notificationRunnable = object : Runnable {
            override fun run() {
                checkForNewNotifications()
                handler.postDelayed(this, 30000) // Poll every 30 seconds
            }
        }
        handler.post(notificationRunnable)
    }

    private fun scheduleRecurringReminder() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        
        // Use a unique requestCode (e.g., 202) for the reminder
        val pendingIntent = PendingIntent.getBroadcast(
            this, 
            202, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set to repeat every 4 hours (4 * 60 * 60 * 1000 ms)
        val interval = 4L * 60L * 60L * 1000L
        val triggerAtMillis = System.currentTimeMillis() + interval

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            interval,
            pendingIntent
        )
    }

    private fun checkForNewNotifications() {
        val userId = SessionManager.getInstance(this).getUserId()
        if (userId == -1) return

        RetrofitClient.instance.getNotifications(userId).enqueue(object : Callback<NotificationsResponse> {
            override fun onResponse(call: Call<NotificationsResponse>, response: Response<NotificationsResponse>) {
                if (response.isSuccessful) {
                    val notifications = response.body()?.notifications ?: emptyList()
                    if (notifications.isNotEmpty()) {
                        val latest = notifications[0]
                        val prefs = NotificationPrefs.getInstance(this@MainActivity)
                        
                        // If it's a new notification and unread
                        if (latest.id > prefs.getLastNotifiedId() && !latest.is_read) {
                            NotificationHelper.showNotification(this@MainActivity, latest.title, latest.message)
                            prefs.setLastNotifiedId(latest.id)
                        }
                    }
                }
            }
            override fun onFailure(call: Call<NotificationsResponse>, t: Throwable) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::notificationRunnable.isInitialized) {
            handler.removeCallbacks(notificationRunnable)
        }
    }

    private fun fetchSavedDocsSilently() {
        val userId = SessionManager.getInstance(this).getUserId()
        if (userId == -1) return

        com.simats.goiretrieval.api.RetrofitClient.instance.getSavedDocuments(userId)
            .enqueue(object : retrofit2.Callback<com.simats.goiretrieval.api.DocumentsResponse> {
                override fun onResponse(
                    call: retrofit2.Call<com.simats.goiretrieval.api.DocumentsResponse>,
                    response: retrofit2.Response<com.simats.goiretrieval.api.DocumentsResponse>
                ) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        val docs = response.body()?.documents ?: emptyList()
                        DocumentRepository.setSavedDocs(docs)
                    }
                }
                override fun onFailure(call: retrofit2.Call<com.simats.goiretrieval.api.DocumentsResponse>, t: Throwable) {}
            })
    }

    private fun setupBottomNavigation() {
        findViewById<View>(R.id.nav_home).setOnClickListener { switchToTab(0) }
        findViewById<View>(R.id.nav_search).setOnClickListener { switchToTab(1) }
        findViewById<View>(R.id.nav_docs).setOnClickListener { switchToTab(2) }
        findViewById<View>(R.id.nav_ai_assist).setOnClickListener { switchToTab(3) }
        findViewById<View>(R.id.nav_profile).setOnClickListener { switchToTab(4) }
    }

    fun switchToTab(index: Int, addToHistory: Boolean = true) {
        if (addToHistory && index != currentTab) {
            navigationHistory.add(currentTab)
        }
        
        val fragment: Fragment = when (index) {
            0 -> HomeFragment()
            1 -> SearchFragment()
            2 -> DocsFragment()
            3 -> AIAssistFragment()
            4 -> ProfileFragment()
            else -> HomeFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        updateNavUI(index)
        currentTab = index
    }

    private fun updateNavUI(activeIndex: Int) {
        val navy = ContextCompat.getColor(this, R.color.navy_home)
        val grey = ContextCompat.getColor(this, R.color.grey_text)

        val navItems = listOf(
            Triple(R.id.iv_nav_home, R.id.tv_nav_home, R.id.dot_nav_home),
            Triple(R.id.iv_nav_search, R.id.tv_nav_search, R.id.dot_nav_search),
            Triple(R.id.iv_nav_docs, R.id.tv_nav_docs, R.id.dot_nav_docs),
            Triple(R.id.iv_nav_ai, R.id.tv_nav_ai, R.id.dot_nav_ai),
            Triple(R.id.iv_nav_profile, R.id.tv_nav_profile, R.id.dot_nav_profile)
        )

        navItems.forEachIndexed { index, triple ->
            val isSelected = index == activeIndex
            findViewById<ImageView>(triple.first).setColorFilter(if (isSelected) navy else grey)
            findViewById<TextView>(triple.second).setTextColor(if (isSelected) navy else grey)
            findViewById<TextView>(triple.second).typeface = if (isSelected) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            findViewById<View>(triple.third).visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
        }
    }
}
