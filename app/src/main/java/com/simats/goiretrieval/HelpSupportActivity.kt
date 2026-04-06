package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.simats.goiretrieval.api.RetrofitClient

class HelpSupportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_help_support)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        setupNavigation()
    }

    private fun setupNavigation() {
        // Back Button in Header returns to Profile
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // FAQ Button
        findViewById<View>(R.id.btn_faq).setOnClickListener {
            startActivity(Intent(this, FAQActivity::class.java))
        }

        // User Guide Button
        findViewById<View>(R.id.btn_user_guide).setOnClickListener {
            startActivity(Intent(this, UserGuideActivity::class.java))
        }

        // Raise Ticket - Redirects directly to email app
        findViewById<View>(R.id.btn_raise_ticket).setOnClickListener {
            val session = SessionManager.getInstance(this@HelpSupportActivity)
            val userName = session.getUserName() ?: "User"
            val userId = session.getUserId()
            
            // Build support header
            val deviceInfo = "\n\n--- App Support Data ---\n" +
                             "User ID: $userId\n" +
                             "Device: ${android.os.Build.MODEL}\n" +
                             "Android: ${android.os.Build.VERSION.RELEASE}"

            val mailUri = android.net.Uri.parse("mailto:goiretrieval@gmail.com")
            val emailIntent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                putExtra(Intent.EXTRA_SUBJECT, "Support Ticket: $userName")
                putExtra(Intent.EXTRA_TEXT, "Hello Team,\n\nI need help with... [Type your issue here]\n$deviceInfo")
            }

            try {
                startActivity(Intent.createChooser(emailIntent, "Send ticket via..."))
                
            } catch (ex: android.content.ActivityNotFoundException) {
                android.widget.Toast.makeText(this, "No email client installed.", android.widget.Toast.LENGTH_SHORT).show()
            }
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
