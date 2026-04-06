package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bg_header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        setupMenuRows()
        setupNavigation()
        loadUserProfile()
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val tvName = findViewById<TextView>(R.id.tv_name)
        val tvAvatar = findViewById<TextView>(R.id.tv_avatar)
        
        val name = SessionManager.getInstance(this).getUserName() ?: "User"
        tvName.text = name
        
        // Use first letter as avatar initial
        tvAvatar.text = if (name.isNotEmpty()) name[0].uppercaseChar().toString() else "U"
    }

    private fun setupMenuRows() {
        // Section: Account
        setupRow(R.id.menu_personal_info, "Personal Information", R.drawable.ic_user_outline)
        setupRow(R.id.menu_role_permissions, "Role & Permissions", R.drawable.ic_shield_outline)
        setupRow(R.id.menu_preferences, "Preferences", R.drawable.ic_gear_outline)

        // Section: System
        setupRow(R.id.menu_notification_settings, "Notification Settings", R.drawable.ic_bell_outline)
        setupRow(R.id.menu_help_support, "Help & Support", R.drawable.ic_question_outline)

        // Section: Settings
        setupRow(R.id.menu_security_settings, "Security Settings", R.drawable.ic_shield_check_outline)
        setupRow(R.id.menu_privacy_settings, "Privacy Settings", R.drawable.ic_padlock_outline)
        setupRow(R.id.menu_about_app, "About Application", R.drawable.ic_info_circle_outline)
    }

    private fun setupRow(rowId: Int, title: String, iconResId: Int) {
        val rowView = findViewById<View>(rowId)
        val tvTitle = rowView.findViewById<TextView>(R.id.row_title)
        val ivIcon = rowView.findViewById<ImageView>(R.id.row_icon)
        
        tvTitle.text = title
        ivIcon.setImageResource(iconResId)
    }

    private fun setupNavigation() {
        // Menu item click listeners
        findViewById<View>(R.id.menu_personal_info).setOnClickListener {
            startActivity(Intent(this, PersonalInfoActivity::class.java))
        }
        findViewById<View>(R.id.menu_role_permissions).setOnClickListener {
            startActivity(Intent(this, RolePermissionsActivity::class.java))
        }
        findViewById<View>(R.id.menu_preferences).setOnClickListener {
            startActivity(Intent(this, PreferencesActivity::class.java))
        }
        findViewById<View>(R.id.menu_notification_settings).setOnClickListener {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
        }
        findViewById<View>(R.id.menu_help_support).setOnClickListener {
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }
        findViewById<View>(R.id.menu_security_settings).setOnClickListener {
            startActivity(Intent(this, SecuritySettingsActivity::class.java))
        }
        findViewById<View>(R.id.menu_privacy_settings).setOnClickListener {
            startActivity(Intent(this, PrivacySettingsActivity::class.java))
        }
        findViewById<View>(R.id.menu_about_app).setOnClickListener {
            startActivity(Intent(this, AboutAppActivity::class.java))
        }

        // Back Button in Header
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            navigateHome()
        }

        // Bottom Navigation Buttons
        findViewById<View>(R.id.nav_home).setOnClickListener {
            navigateHome()
        }
        
        findViewById<View>(R.id.nav_search).setOnClickListener {
            startActivity(Intent(this, SearchDocsActivity::class.java))
            finish()
        }
        
        findViewById<View>(R.id.nav_docs).setOnClickListener {
            startActivity(Intent(this, BrowseDocsActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_ai_assist_btn).setOnClickListener {
            startActivity(Intent(this, AIAssistActivity::class.java))
            finish()
        }
        // Profile is the active tab, no listener needed for nav_profile_btn

        // Handle Logout Button
        findViewById<View>(R.id.btn_logout).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Yes") { _, _ ->
                    // Clear Session
                    SessionManager.getInstance(this).logout()
                    
                    val intent = Intent(this, SignInActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun navigateHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}
