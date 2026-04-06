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

class DeviceManagementActivity : AppCompatActivity() {

    private lateinit var containerCurrent: android.widget.LinearLayout
    private lateinit var containerOther: android.widget.LinearLayout
    private lateinit var tvOtherHeader: TextView
    private lateinit var cardOther: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_device_management)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        containerCurrent = findViewById(R.id.container_current_device)
        containerOther = findViewById(R.id.container_other_devices)
        tvOtherHeader = findViewById(R.id.tv_other_devices_header)
        cardOther = findViewById(R.id.card_other_devices)

        findViewById<TextView>(R.id.btn_logout_all).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to logout from all devices? This will also log you out from this device.")
                .setPositiveButton("Yes") { _, _ ->
                    val session = SessionManager.getInstance(this)
                    val userId = session.getUserId()
                    
                    com.simats.goiretrieval.api.RetrofitClient.instance.logoutAllDevices(userId).enqueue(object : retrofit2.Callback<com.simats.goiretrieval.api.SignupResponse> {
                        override fun onResponse(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, response: retrofit2.Response<com.simats.goiretrieval.api.SignupResponse>) {
                            // Regardless of server success, we clear local session for safety
                            session.logout()
                            val intent = Intent(this@DeviceManagementActivity, SignInActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }

                        override fun onFailure(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, t: Throwable) {
                            // Clear session locally anyway
                            session.logout()
                            val intent = Intent(this@DeviceManagementActivity, SignInActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    })
                }
                .setNegativeButton("No", null)
                .show()
        }

        setupNavigation()
        
        // Show current device immediately using local hardware info
        val currentModel = android.os.Build.MODEL
        renderDevices(emptyList(), currentModel)
        
        // Fetch full list from backend
        fetchDevices()
    }

    private fun fetchDevices() {
        val session = SessionManager.getInstance(this)
        val userId = session.getUserId()
        val currentModel = android.os.Build.MODEL

        com.simats.goiretrieval.api.RetrofitClient.instance.getDevices(userId).enqueue(object : retrofit2.Callback<com.simats.goiretrieval.api.DevicesResponse> {
            override fun onResponse(call: retrofit2.Call<com.simats.goiretrieval.api.DevicesResponse>, response: retrofit2.Response<com.simats.goiretrieval.api.DevicesResponse>) {
                val devices = response.body()?.devices ?: emptyList()
                renderDevices(devices, currentModel)
            }

            override fun onFailure(call: retrofit2.Call<com.simats.goiretrieval.api.DevicesResponse>, t: Throwable) {
                // If API fails, we still show current device (already rendered in onCreate)
                renderError("Unable to load other devices. Please check your connection.")
            }
        })
    }

    private fun renderError(message: String) {
        tvOtherHeader.visibility = View.VISIBLE
        tvOtherHeader.text = "Other Devices (Offline)"
        cardOther.visibility = View.VISIBLE
        containerOther.removeAllViews()
        
        val errorView = layoutInflater.inflate(R.layout.item_device, containerOther, false)
        errorView.findViewById<TextView>(R.id.tv_device_name).text = "Network Error"
        errorView.findViewById<TextView>(R.id.tv_device_details).text = message
        errorView.findViewById<ImageView>(R.id.iv_device_icon).apply {
            setImageResource(R.drawable.ic_alert_red)
            setColorFilter(android.graphics.Color.RED)
        }
        errorView.findViewById<TextView>(R.id.btn_logout_device).visibility = View.GONE
        errorView.findViewById<TextView>(R.id.tv_online_tag).apply {
            visibility = View.VISIBLE
            text = "RETRY"
            setOnClickListener { fetchDevices() }
        }
        
        containerOther.addView(errorView)
    }

    private fun renderDevices(devices: List<com.simats.goiretrieval.api.DeviceItem>, currentModel: String) {
        containerCurrent.removeAllViews()
        containerOther.removeAllViews()

        // 1. ALWAYS ADD CURRENT DEVICE (Local Source)
        val currentView = layoutInflater.inflate(R.layout.item_device, containerCurrent, false)
        val tvCurrentName = currentView.findViewById<TextView>(R.id.tv_device_name)
        val tvCurrentDetails = currentView.findViewById<TextView>(R.id.tv_device_details)
        val btnCurrentLogout = currentView.findViewById<TextView>(R.id.btn_logout_device)
        val tvOnline = currentView.findViewById<TextView>(R.id.tv_online_tag)
        
        tvCurrentName.text = "Android Phone (This Device)"
        tvCurrentDetails.text = "Salem, India • Active Now"
        
        // UI Change: Show LOGOUT button instead of ONLINE tag as per user request
        tvOnline.visibility = View.GONE 
        btnCurrentLogout.visibility = View.VISIBLE
        btnCurrentLogout.text = "LOGOUT"
        btnCurrentLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to log out from this device?")
                .setPositiveButton("Logout") { _, _ ->
                    SessionManager.getInstance(this).logout()
                    val intent = Intent(this, SignInActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        
        val currentIconContainer = currentView.findViewById<View>(R.id.ll_device_icon_container)
        val currentIcon = currentView.findViewById<ImageView>(R.id.iv_device_icon)
        
        currentIconContainer.setBackgroundResource(R.drawable.bg_icon_circle_blue)
        currentIcon.setImageResource(R.drawable.ic_phone)
        currentIcon.setColorFilter(getColor(R.color.navy_primary), android.graphics.PorterDuff.Mode.SRC_IN)
        
        containerCurrent.addView(currentView)

        // 2. ADD OTHER DEVICES (API Source)
        var otherCount = 0
        for (device in devices) {
            // Already showing this one in current section
            if (device.deviceName == currentModel) continue

            val itemView = layoutInflater.inflate(R.layout.item_device, containerOther, false)
            val tvName = itemView.findViewById<TextView>(R.id.tv_device_name)
            val tvDetails = itemView.findViewById<TextView>(R.id.tv_device_details)
            val btnLogout = itemView.findViewById<TextView>(R.id.btn_logout_device)
            val ivIcon = itemView.findViewById<ImageView>(R.id.iv_device_icon)

            otherCount++
            tvName.text = device.deviceName ?: "Other Device"
            tvDetails.text = "${device.osVersion ?: "Unknown OS"} • ${formatLastLogin(device.lastLogin)}"
            
            // Icon Logic
            if (device.deviceName?.contains("Windows", ignoreCase = true) == true || 
                device.deviceName?.contains("PC", ignoreCase = true) == true ||
                device.deviceName?.contains("Browser", ignoreCase = true) == true) {
                ivIcon.setImageResource(R.drawable.ic_desktop_outline)
            } else {
                ivIcon.setImageResource(R.drawable.ic_phone)
            }
            
            btnLogout.visibility = View.VISIBLE
            btnLogout.setOnClickListener { confirmLogout(device) }
            
            // Add separator if NOT the first item in "Other" list
            if (otherCount > 1) {
                val separator = View(this)
                val params = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                    1
                ).apply { 
                    val margin = (48 * resources.displayMetrics.density).toInt() + (16 * resources.displayMetrics.density).toInt()
                    setMargins(margin, 0, (16 * resources.displayMetrics.density).toInt(), 0) 
                }
                separator.layoutParams = params
                separator.setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
                containerOther.addView(separator)
            }
            
            containerOther.addView(itemView)
        }

        if (otherCount > 0) {
            tvOtherHeader.visibility = View.VISIBLE
            cardOther.visibility = View.VISIBLE
        } else {
            tvOtherHeader.visibility = View.GONE
            cardOther.visibility = View.GONE
        }
    }

    private fun formatLastLogin(lastLogin: String?): String {
        return if (lastLogin == null) "Active Now" else "Last Login: $lastLogin"
    }

    private fun confirmLogout(device: com.simats.goiretrieval.api.DeviceItem) {
        AlertDialog.Builder(this)
            .setTitle("Sign Out Device")
            .setMessage("Are you sure you want to sign out from ${device.deviceName}?")
            .setPositiveButton("Logout") { _, _ ->
                logoutDevice(device.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logoutDevice(deviceId: Int) {
        com.simats.goiretrieval.api.RetrofitClient.instance.logoutDevice(deviceId).enqueue(object : retrofit2.Callback<com.simats.goiretrieval.api.SignupResponse> {
            override fun onResponse(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, response: retrofit2.Response<com.simats.goiretrieval.api.SignupResponse>) {
                if (response.isSuccessful) {
                    fetchDevices()
                    android.widget.Toast.makeText(this@DeviceManagementActivity, "Device logged out", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, t: Throwable) {
                android.widget.Toast.makeText(this@DeviceManagementActivity, "Failed to logout device", android.widget.Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupNavigation() {
        // Back Button
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Bottom Navigation
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

        findViewById<View>(R.id.nav_ai_assist).setOnClickListener {
            startActivity(Intent(this, AIAssistActivity::class.java))
            finish()
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
