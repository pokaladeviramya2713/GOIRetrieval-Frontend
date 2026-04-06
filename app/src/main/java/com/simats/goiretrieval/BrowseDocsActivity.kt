package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.goiretrieval.api.ApiDocument
import com.simats.goiretrieval.api.DocumentsResponse
import com.simats.goiretrieval.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BrowseDocsActivity : AppCompatActivity() {

    private lateinit var adapter: BrowseDocsAdapter
    private var docList = mutableListOf<ApiDocument>()
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_browse_docs)

        userId = SessionManager.getInstance(this).getUserId()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        setupRecyclerView()
        setupListeners()
        setupBottomNavigation()
        
        // Default to Recent instead of Library
        if (userId != -1) fetchRecentDocuments(userId) else adapter.updateList(emptyList())
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rv_browse_docs)
        adapter = BrowseDocsAdapter(docList)
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }
        
        findViewById<ImageView>(R.id.iv_notifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        val tabRecent = findViewById<LinearLayout>(R.id.tab_recent)
        val tabSaved = findViewById<LinearLayout>(R.id.tab_saved)
        
        val indicatorRecent = findViewById<View>(R.id.indicator_recent)
        val indicatorSaved = findViewById<View>(R.id.indicator_saved)
        
        val tvRecent = findViewById<TextView>(R.id.tv_tab_recent)
        val tvSaved = findViewById<TextView>(R.id.tv_tab_saved)

        tabRecent.setOnClickListener {
            updateTabUI(indicatorRecent, indicatorSaved, tvRecent, tvSaved, 0)
            if (userId != -1) fetchRecentDocuments(userId) else adapter.updateList(emptyList())
        }

        tabSaved.setOnClickListener {
            updateTabUI(indicatorRecent, indicatorSaved, tvRecent, tvSaved, 1)
            if (userId != -1) fetchSavedDocuments(userId) else adapter.updateList(emptyList())
        }
    }

    private fun updateTabUI(iR: View, iS: View, tR: TextView, tS: TextView, activeIndex: Int) {
        val navy = ContextCompat.getColor(this, R.color.navy_home)
        val grey = ContextCompat.getColor(this, R.color.grey_text)
        val trans = ContextCompat.getColor(this, android.R.color.transparent)

        iR.setBackgroundColor(if (activeIndex == 0) navy else trans)
        iS.setBackgroundColor(if (activeIndex == 1) navy else trans)

        tR.setTextColor(if (activeIndex == 0) navy else grey)
        tS.setTextColor(if (activeIndex == 1) navy else grey)
        
        tR.typeface = if (activeIndex == 0) android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD) else android.graphics.Typeface.SANS_SERIF
        tS.typeface = if (activeIndex == 1) android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD) else android.graphics.Typeface.SANS_SERIF
    }

    private fun fetchDocuments(category: String) {
        adapter.isSavedList = false
        findViewById<View>(R.id.rv_browse_docs).alpha = 0.5f
        RetrofitClient.instance.getDocuments(category).enqueue(object : Callback<DocumentsResponse> {
            override fun onResponse(call: Call<DocumentsResponse>, response: Response<DocumentsResponse>) {
                findViewById<View>(R.id.rv_browse_docs).alpha = 1.0f
                if (response.isSuccessful && response.body()?.status == "success") {
                    docList = response.body()?.documents?.toMutableList() ?: mutableListOf()
                    adapter.updateList(docList)
                }
            }
            override fun onFailure(call: Call<DocumentsResponse>, t: Throwable) {
                findViewById<View>(R.id.rv_browse_docs).alpha = 1.0f
                android.widget.Toast.makeText(this@BrowseDocsActivity, "Failed to load documents", android.widget.Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchSavedDocuments(uid: Int) {
        adapter.isSavedList = true
        findViewById<View>(R.id.rv_browse_docs).alpha = 0.5f
        RetrofitClient.instance.getSavedDocuments(uid).enqueue(object : Callback<DocumentsResponse> {
            override fun onResponse(call: Call<DocumentsResponse>, response: Response<DocumentsResponse>) {
                findViewById<View>(R.id.rv_browse_docs).alpha = 1.0f
                if (response.isSuccessful && response.body()?.status == "success") {
                    docList = response.body()?.documents?.toMutableList() ?: mutableListOf()
                    DocumentRepository.setSavedDocs(docList) // Sync local repository
                    adapter.updateList(docList)
                }
            }
            override fun onFailure(call: Call<DocumentsResponse>, t: Throwable) {
                findViewById<View>(R.id.rv_browse_docs).alpha = 1.0f
                android.widget.Toast.makeText(this@BrowseDocsActivity, "Failed to load saved documents", android.widget.Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun fetchRecentDocuments(uid: Int) {
        adapter.isSavedList = false
        findViewById<View>(R.id.rv_browse_docs).alpha = 0.5f
        RetrofitClient.instance.getRecentViews(uid).enqueue(object : Callback<DocumentsResponse> {
            override fun onResponse(call: Call<DocumentsResponse>, response: Response<DocumentsResponse>) {
                findViewById<View>(R.id.rv_browse_docs).alpha = 1.0f
                if (response.isSuccessful && response.body()?.status == "success") {
                    docList = response.body()?.documents?.toMutableList() ?: mutableListOf()
                    adapter.updateList(docList)
                }
            }
            override fun onFailure(call: Call<DocumentsResponse>, t: Throwable) {
                findViewById<View>(R.id.rv_browse_docs).alpha = 1.0f
                android.widget.Toast.makeText(this@BrowseDocsActivity, "Failed to load recent documents", android.widget.Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_search).setOnClickListener {
            startActivity(Intent(this, SearchDocsActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_ai_assist).setOnClickListener {
            startActivity(Intent(this, AIAssistActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("SELECT_FRAGMENT", "profile")
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}
