package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SecuritySettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_security_settings)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        setupNavigation()
    }

    private fun setupNavigation() {
        // Back Button
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Change Password -> SecurityChangePasswordActivity
        findViewById<View>(R.id.btn_change_password).setOnClickListener {
            startActivity(Intent(this, SecurityChangePasswordActivity::class.java))
        }

        // Device Management
        findViewById<View>(R.id.btn_device_management).setOnClickListener {
            startActivity(Intent(this, DeviceManagementActivity::class.java))
        }

        // Two-Factor Authentication Functional Switch
        val switch2FA = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_two_factor)
        val session = SessionManager.getInstance(this)
        
        switch2FA.isChecked = session.is2FAEnabled()
        switch2FA.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed) return@setOnCheckedChangeListener // Only handle user-initiated changes

            val userId = session.getUserId()
            val userEmail = session.getUserEmail()

            if (userId == -1 || userEmail.isNullOrEmpty()) {
                android.widget.Toast.makeText(this@SecuritySettingsActivity, "Session error: User ID or Email not found", android.widget.Toast.LENGTH_SHORT).show()
                buttonView.isChecked = !isChecked
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                // If enabling, we need verification
                val request = com.simats.goiretrieval.api.Update2FARequest(userId, true)
                
                com.simats.goiretrieval.api.RetrofitClient.instance.update2FA(request).enqueue(object : retrofit2.Callback<com.simats.goiretrieval.api.SignupResponse> {
                    override fun onResponse(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, response: retrofit2.Response<com.simats.goiretrieval.api.SignupResponse>) {
                        val apiResponse = response.body()
                        if (response.isSuccessful && apiResponse?.status == "success") {
                            android.widget.Toast.makeText(this@SecuritySettingsActivity, "Email sent OTP", android.widget.Toast.LENGTH_SHORT).show()
                            
                            val intent = Intent(this@SecuritySettingsActivity, TwoFactorVerifyActivity::class.java)
                            intent.putExtra("email", userEmail)
                            intent.putExtra("is_from_settings", true)
                            startActivity(intent)
                        } else {
                            buttonView.isChecked = false
                            val errorMsg = apiResponse?.message ?: "Server Error: ${response.code()}"
                            android.widget.Toast.makeText(this@SecuritySettingsActivity, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, t: Throwable) {
                        buttonView.isChecked = false
                        android.widget.Toast.makeText(this@SecuritySettingsActivity, "Network Error: ${t.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                // If disabling, just update
                val request = com.simats.goiretrieval.api.Update2FARequest(userId, false)
                
                com.simats.goiretrieval.api.RetrofitClient.instance.update2FA(request).enqueue(object : retrofit2.Callback<com.simats.goiretrieval.api.SignupResponse> {
                    override fun onResponse(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, response: retrofit2.Response<com.simats.goiretrieval.api.SignupResponse>) {
                        val apiResponse = response.body()
                        if (response.isSuccessful && apiResponse?.status == "success") {
                            session.set2FAEnabled(false)
                            android.widget.Toast.makeText(this@SecuritySettingsActivity, "2FA Disabled", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            buttonView.isChecked = true
                            val errorMsg = apiResponse?.message ?: "Update Failed: ${response.code()}"
                            android.widget.Toast.makeText(this@SecuritySettingsActivity, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, t: Throwable) {
                        buttonView.isChecked = true
                        android.widget.Toast.makeText(this@SecuritySettingsActivity, "Network Error: ${t.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                })
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
