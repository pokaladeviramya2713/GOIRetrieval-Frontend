package com.simats.goiretrieval

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.goiretrieval.api.RecentActivityResponse
import com.simats.goiretrieval.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RecentActivityLogActivity : AppCompatActivity() {
    private lateinit var adapter: RecentActivityAdapter
    private lateinit var rvRecentActivity: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recent_activity)

        rvRecentActivity = findViewById(R.id.rv_recent_activity)
        
        // Add a ProgressBar if not already in layout (or just use visible/gone for RV)
        // I'll assume standard layout for now, or just handle toast on failure
        
        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        setupRecyclerView()
        fetchRecentActivities()
        setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        adapter = RecentActivityAdapter(emptyList()) { activity ->
            if (activity.doc_id != null) {
                // Navigate to Document Detail if doc_id is present
                val intent = Intent(this, ArticleViewActivity::class.java).apply {
                    val title = activity.detail.substringAfter("\"").substringBefore("\"")
                    putExtra("DOC_ID", activity.doc_id)
                    putExtra("DOC_TITLE", title)
                    putExtra("DOC_TYPE", "Report")
                    putExtra("DOC_FOLDER", "Reports")
                    putExtra("DOC_FILENAME", "${activity.doc_id}.json")
                }
                startActivity(intent)
            }
        }
        rvRecentActivity.layoutManager = LinearLayoutManager(this)
        rvRecentActivity.adapter = adapter
    }

    private fun fetchRecentActivities() {
        val userId = SessionManager.getInstance(this).getUserId()

        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }


        RetrofitClient.instance.getActivities(userId).enqueue(object : Callback<RecentActivityResponse> {
            override fun onResponse(call: Call<RecentActivityResponse>, response: Response<RecentActivityResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    val allActivities = response.body()?.activities ?: emptyList()
                    adapter.updateData(allActivities)
                } else {
                    Toast.makeText(this@RecentActivityLogActivity, "Failed to load activities", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RecentActivityResponse>, t: Throwable) {
                Toast.makeText(this@RecentActivityLogActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.nav_home_btn).setOnClickListener {
            finish() // Since it is launched from Home, just finish
        }
        findViewById<LinearLayout>(R.id.nav_search_btn).setOnClickListener {
            val intent = Intent(this, SearchDocsActivity::class.java)
            startActivity(intent)
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_docs_btn).setOnClickListener {
            val intent = Intent(this, BrowseDocsActivity::class.java)
            startActivity(intent)
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_ai_assist_btn).setOnClickListener {
            val intent = Intent(this, AIAssistActivity::class.java)
            startActivity(intent)
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_profile_btn).setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
