package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.goiretrieval.api.ApiDocument
import com.simats.goiretrieval.api.DocumentsResponse
import com.simats.goiretrieval.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DocsFragment : Fragment() {

    private lateinit var adapter: BrowseDocsAdapter
    private var docList = mutableListOf<ApiDocument>()
    private var userId: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_docs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId = SessionManager.getInstance(requireContext()).getUserId()

        setupRecyclerView(view)
        setupListeners(view)
        setupNotifications(view)
        
        // Default to Recent
        if (userId != -1) fetchRecentDocuments(userId) else adapter.updateList(emptyList())
    }

    override fun onResume() {
        super.onResume()
        updateNotificationDot()
    }

    private fun setupNotifications(view: View) {
        val bell = view.findViewById<View>(R.id.rl_notification_bell) ?: return
        val dot = view.findViewById<View>(R.id.view_notification_dot) ?: return

        bell.setOnClickListener {
            dot.visibility = View.GONE
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }
    }

    private fun updateNotificationDot() {
        val view = view ?: return
        val dot = view.findViewById<View>(R.id.view_notification_dot) ?: return
        
        if (userId == -1) return

        RetrofitClient.instance.getNotifications(userId).enqueue(object : Callback<com.simats.goiretrieval.api.NotificationsResponse> {
            override fun onResponse(
                call: Call<com.simats.goiretrieval.api.NotificationsResponse>,
                response: Response<com.simats.goiretrieval.api.NotificationsResponse>
            ) {
                if (response.isSuccessful && isAdded) {
                    val notifications = response.body()?.notifications ?: emptyList()
                    val hasUnread = notifications.any { !it.is_read }
                    dot.visibility = if (hasUnread) View.VISIBLE else View.GONE
                }
            }
            override fun onFailure(call: Call<com.simats.goiretrieval.api.NotificationsResponse>, t: Throwable) {}
        })
    }

    private fun setupRecyclerView(view: View) {
        val rv = view.findViewById<RecyclerView>(R.id.rv_browse_docs)
        adapter = BrowseDocsAdapter(docList)
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupListeners(view: View) {
        val tabRecent = view.findViewById<LinearLayout>(R.id.tab_recent)
        val tabSaved = view.findViewById<LinearLayout>(R.id.tab_saved)
        
        val indicatorRecent = view.findViewById<View>(R.id.indicator_recent)
        val indicatorSaved = view.findViewById<View>(R.id.indicator_saved)
        
        val tvRecent = view.findViewById<TextView>(R.id.tv_tab_recent)
        val tvSaved = view.findViewById<TextView>(R.id.tv_tab_saved)

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
        if (!isAdded) return
        val navy = ContextCompat.getColor(requireContext(), R.color.navy_home)
        val grey = ContextCompat.getColor(requireContext(), R.color.grey_text)
        val trans = ContextCompat.getColor(requireContext(), android.R.color.transparent)

        iR.setBackgroundColor(if (activeIndex == 0) navy else trans)
        iS.setBackgroundColor(if (activeIndex == 1) navy else trans)

        tR.setTextColor(if (activeIndex == 0) navy else grey)
        tS.setTextColor(if (activeIndex == 1) navy else grey)
        
        tR.typeface = if (activeIndex == 0) android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD) else android.graphics.Typeface.SANS_SERIF
        tS.typeface = if (activeIndex == 1) android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD) else android.graphics.Typeface.SANS_SERIF
    }

    private fun fetchSavedDocuments(uid: Int) {
        adapter.isSavedList = true
        view?.findViewById<View>(R.id.rv_browse_docs)?.alpha = 0.5f
        RetrofitClient.instance.getSavedDocuments(uid).enqueue(object : Callback<DocumentsResponse> {
            override fun onResponse(call: Call<DocumentsResponse>, response: Response<DocumentsResponse>) {
                view?.findViewById<View>(R.id.rv_browse_docs)?.alpha = 1.0f
                if (response.isSuccessful && response.body()?.status == "success") {
                    docList = response.body()?.documents?.toMutableList() ?: mutableListOf()
                    DocumentRepository.setSavedDocs(docList)
                    adapter.updateList(docList)
                }
            }
            override fun onFailure(call: Call<DocumentsResponse>, t: Throwable) {
                view?.findViewById<View>(R.id.rv_browse_docs)?.alpha = 1.0f
                if (isAdded) Toast.makeText(requireContext(), "Failed to load saved documents", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun fetchRecentDocuments(uid: Int) {
        adapter.isSavedList = false
        view?.findViewById<View>(R.id.rv_browse_docs)?.alpha = 0.5f
        RetrofitClient.instance.getRecentViews(uid).enqueue(object : Callback<DocumentsResponse> {
            override fun onResponse(call: Call<DocumentsResponse>, response: Response<DocumentsResponse>) {
                view?.findViewById<View>(R.id.rv_browse_docs)?.alpha = 1.0f
                if (response.isSuccessful && response.body()?.status == "success") {
                    docList = response.body()?.documents?.toMutableList() ?: mutableListOf()
                    adapter.updateList(docList)
                }
            }
            override fun onFailure(call: Call<DocumentsResponse>, t: Throwable) {
                view?.findViewById<View>(R.id.rv_browse_docs)?.alpha = 1.0f
                if (isAdded) Toast.makeText(requireContext(), "Failed to load recent documents", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
