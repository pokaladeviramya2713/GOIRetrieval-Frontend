package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AboutAppActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_about_app)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        setupNavigation()
    }

    private fun setupNavigation() {
        // Back Button in Header
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Terms & Conditions
        findViewById<View>(R.id.btn_terms).setOnClickListener {
            startActivity(Intent(this, TermsConditionsActivity::class.java))
        }

        // Privacy Policy
        findViewById<View>(R.id.btn_privacy_policy).setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }

        // Bottom Navigation
        findViewById<View>(R.id.nav_home).setOnClickListener {
            navigateHome()
        }
        
        findViewById<View>(R.id.nav_profile).setOnClickListener {
            finish()
        }
    }

    private fun navigateHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
}
