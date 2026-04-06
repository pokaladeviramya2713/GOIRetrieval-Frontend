package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.graphics.Color
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.switchmaterial.SwitchMaterial

class PreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_preferences)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupPreferences()
    }


    private fun setupPreferences() {
        val session = SessionManager.getInstance(this)
        
        // Load initial states
        findViewById<SwitchMaterial>(R.id.switch_stats).isChecked = session.shouldShowStats()
        findViewById<SwitchMaterial>(R.id.switch_chart).isChecked = session.shouldShowChart()
        findViewById<SwitchMaterial>(R.id.switch_activity).isChecked = session.shouldShowActivity()

        // Language Selector Dropdown
        findViewById<LinearLayout>(R.id.layout_language_selector).setOnClickListener {
            showLanguageDropdown(it)
        }

        // Header Back Button -> ProfileActivity
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Footer Save Button -> ProfileActivity
        findViewById<LinearLayout>(R.id.btn_save_preferences).setOnClickListener {
            saveUserPreferences()
        }

        // Bottom Navigation - Home Icon -> MainActivity
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun showLanguageDropdown(view: android.view.View) {
        val popup = android.widget.PopupMenu(this, view)
        popup.menu.add(android.view.Menu.NONE, 1, android.view.Menu.NONE, "English").apply {
            isCheckable = true
            isChecked = true
        }
        
        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == 1) {
                findViewById<TextView>(R.id.tv_selected_language).text = "English"
                Toast.makeText(this, "Language set to English", Toast.LENGTH_SHORT).show()
                true
            } else false
        }
        popup.show()
    }

    private fun saveUserPreferences() {
        val session = SessionManager.getInstance(this)
        val showStats = findViewById<SwitchMaterial>(R.id.switch_stats).isChecked
        val showChart = findViewById<SwitchMaterial>(R.id.switch_chart).isChecked
        val showActivity = findViewById<SwitchMaterial>(R.id.switch_activity).isChecked

        // We still pass "light" as a dummy theme for the session manager's signature
        session.savePreferences("light", showStats, showChart, showActivity)
        Toast.makeText(this, "Preferences Saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}
