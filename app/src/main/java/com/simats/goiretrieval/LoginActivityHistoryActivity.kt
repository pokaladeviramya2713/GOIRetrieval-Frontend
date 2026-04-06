package com.simats.goiretrieval

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.goiretrieval.api.ApiActivity

class LoginActivityHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_activity_history)

        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        val rvLogin = findViewById<RecyclerView>(R.id.rv_login_history)
        rvLogin.layoutManager = LinearLayoutManager(this)
        
        // Mocking login data for UI polish (Ideally would fetch from API)
        val loginActivities = listOf(
            ApiActivity(1, "login", "Successful Login", "09:45 AM", "Today", null),
            ApiActivity(2, "user", "Login from New Device", "11:20 PM", "Yesterday", null),
            ApiActivity(3, "security", "Password Security Scan", "03:15 PM", "28 Mar, 2024", null)
        )


        rvLogin.adapter = RecentActivityAdapter(loginActivities, true) {
            // Activity detail click (Optional)
        }
    }
}
