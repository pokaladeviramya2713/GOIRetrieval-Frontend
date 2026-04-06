package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class NotificationSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification_settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val session = SessionManager.getInstance(this)
        val switchPolicy = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_policy)
        val switchScheme = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_scheme)
        val switchAI = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_ai)
        val switchSystem = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_system)
        val switchPush = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_push)
        val switchEmail = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_email)
        val switchSms = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_sms)

        // Load current settings
        val currentSettings = session.getTopicNotifications()
        switchPolicy.isChecked = currentSettings["policy"] ?: true
        switchScheme.isChecked = currentSettings["scheme"] ?: true
        switchAI.isChecked = currentSettings["ai"] ?: false
        switchSystem.isChecked = currentSettings["system"] ?: true

        val (push, email, sms) = session.getNotificationPrefs()
        switchPush.isChecked = push
        switchEmail.isChecked = email
        switchSms.isChecked = sms

        // Header Back Button -> ProfileActivity
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Footer Save Button
        findViewById<LinearLayout>(R.id.btn_save_changes).setOnClickListener {
            session.saveTopicNotifications(
                switchPolicy.isChecked,
                switchScheme.isChecked,
                switchAI.isChecked,
                switchSystem.isChecked
            )
            session.saveNotificationPrefs(
                switchPush.isChecked,
                switchEmail.isChecked,
                switchSms.isChecked
            )
            android.widget.Toast.makeText(this, "Notification Settings Saved", android.widget.Toast.LENGTH_SHORT).show()
            finish()
        }

        // Bottom Navigation - Home Icon -> MainActivity
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}
