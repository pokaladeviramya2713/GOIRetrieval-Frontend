package com.simats.goiretrieval

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.ScaleAnimation
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        // Premium Fade and Scale Intro
        val container = findViewById<android.widget.LinearLayout>(R.id.logo_container)
        
        val fadeIn = AlphaAnimation(0f, 1.0f).apply {
            duration = 1200
            fillAfter = true
        }
        
        val scaleUp = ScaleAnimation(
            0.85f, 1.0f, 
            0.85f, 1.0f, 
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f, 
            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1500
            fillAfter = true
        }
        
        container.startAnimation(fadeIn)
        container.startAnimation(scaleUp)

        // Intent logic after delay
        Handler(Looper.getMainLooper()).postDelayed({
            checkSession()
        }, 3000) // 3 seconds splash for premium brand presence
    }

    private fun checkSession() {
        val session = SessionManager.getInstance(this)
        
        val nextActivity = when {
            session.isLoggedIn() -> MainActivity::class.java
            else -> OnboardingActivity::class.java
        }
        
        val intent = Intent(this, nextActivity)
        startActivity(intent)
        
        // Smooth transition to next screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        
        finish()
    }
}
