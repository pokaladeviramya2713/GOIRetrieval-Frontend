package com.simats.goiretrieval

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.goiretrieval.api.ApiNotification
import com.simats.goiretrieval.api.ApiService
import com.simats.goiretrieval.api.NotificationsResponse
import com.simats.goiretrieval.api.SignupResponse
import com.simats.goiretrieval.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var tvMarkRead: TextView
    private val notificationList = mutableListOf<ApiNotification>()
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)

        // Get User ID from SessionManager
        userId = SessionManager.getInstance(this).getUserId()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        setupViews()
        fetchNotifications()
    }

    private fun setupViews() {
        rvNotifications = findViewById(R.id.rv_notifications)
        tvMarkRead = findViewById(R.id.tv_mark_read)

        rvNotifications.layoutManager = LinearLayoutManager(this)
        notificationAdapter = NotificationAdapter(notificationList) { notification ->
            markAsRead(notification.id)
        }
        rvNotifications.adapter = notificationAdapter

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        tvMarkRead.setOnClickListener {
            markAllAsRead()
        }

        // Bottom Navigation
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_search).setOnClickListener {
            startActivity(Intent(this, SearchDocsActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_docs).setOnClickListener {
            startActivity(Intent(this, BrowseDocsActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_ai_assist).setOnClickListener {
            startActivity(Intent(this, AIAssistActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.nav_profile).setOnClickListener {
            finish()
        }
    }

    private fun fetchNotifications() {
        if (userId == -1) return

        RetrofitClient.instance.getNotifications(userId).enqueue(object : Callback<NotificationsResponse> {
            override fun onResponse(call: Call<NotificationsResponse>, response: Response<NotificationsResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()
                    if (apiResponse?.status == "success") {
                        notificationList.clear()
                        apiResponse.notifications?.let { notificationList.addAll(it) }
                        notificationAdapter.notifyDataSetChanged()
                        
                        // Sync with global unread pref
                        val hasAnyUnread = notificationList.any { !it.is_read }
                        NotificationPrefs.setUnreadNotifications(this@NotificationsActivity, hasAnyUnread)
                        
                        updateHeaderStates()
                    }
                }
            }

            override fun onFailure(call: Call<NotificationsResponse>, t: Throwable) {
                Toast.makeText(this@NotificationsActivity, "Error fetching notifications", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun markAllAsRead(silent: Boolean = false) {
        if (userId == -1) return

        val request = com.simats.goiretrieval.api.MarkReadRequest(userId = userId)
        RetrofitClient.instance.markNotificationsRead(request).enqueue(object : Callback<SignupResponse> {
            override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                if (response.isSuccessful) {
                    // Update local list
                    notificationList.forEach { it.is_read = true }
                    notificationAdapter.notifyDataSetChanged()
                    
                    // Clear global unread dot
                    NotificationPrefs.markNotificationsRead(this@NotificationsActivity)
                    
                    updateHeaderStates()
                    if (!silent) {
                        Toast.makeText(this@NotificationsActivity, "All notifications marked as read", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                Toast.makeText(this@NotificationsActivity, "Error marking notifications", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun markAsRead(notificationId: Int) {
        if (userId == -1) return

        val request = com.simats.goiretrieval.api.MarkReadRequest(userId = userId, notificationId = notificationId)
        RetrofitClient.instance.markNotificationsRead(request).enqueue(object : Callback<SignupResponse> {
            override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                if (response.isSuccessful) {
                    // Update local item
                    notificationList.find { it.id == notificationId }?.is_read = true
                    notificationAdapter.notifyDataSetChanged()
                    updateHeaderStates()
                }
            }

            override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                // Silent fail for individual mark read
            }
        })
    }

    private fun updateHeaderStates() {
        val hasUnread = notificationList.any { !it.is_read }
        tvMarkRead.visibility = if (hasUnread) View.VISIBLE else View.GONE
        findViewById<ImageView>(R.id.iv_header_bell).setImageResource(
            if (hasUnread) R.drawable.ic_notification else R.drawable.ic_notification_read
        )
    }
}
