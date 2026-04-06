package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
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

class SearchFragment : Fragment() {

    private lateinit var rvDocs: RecyclerView
    private lateinit var adapter: SearchDocsAdapter
    private var allDocs = listOf<ApiDocument>()
    private lateinit var chips: List<TextView>
    private lateinit var etSearch: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        setupChips(view)
        setupSearchInput(view)
        setupBackButton(view)
        setupNotifications(view)
        
        // Initial fetch
        performSearch("")
    }

    override fun onResume() {
        super.onResume()
        updateNotificationDot()
    }

    private fun setupBackButton(view: View) {
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun setupNotifications(view: View) {
        val bell = view.findViewById<View>(R.id.rl_notification_bell)
        val dot = view.findViewById<View>(R.id.view_notification_dot)

        bell.setOnClickListener {
            dot.visibility = View.GONE
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }
    }

    private fun updateNotificationDot() {
        val view = view ?: return
        val dot = view.findViewById<View>(R.id.view_notification_dot)
        val userId = SessionManager.getInstance(requireContext()).getUserId()
        
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

    private fun setupSearchInput(view: View) {
        etSearch = view.findViewById(R.id.et_search)
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearch.text.toString())
                true
            } else false
        }
    }

    private fun performSearch(query: String) {
        RetrofitClient.instance.searchDocuments(query).enqueue(object : Callback<DocumentsResponse> {
            override fun onResponse(call: Call<DocumentsResponse>, response: Response<DocumentsResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    allDocs = response.body()?.documents ?: emptyList()
                    adapter.updateList(allDocs)
                    
                    if (query.isNotEmpty() && isAdded) {
                        ActivityLogger.log(requireContext(), "Search", "Searched for \"$query\"")
                    }
                    
                    if (query.isEmpty()) {
                        resetChips()
                    }
                }
            }
            override fun onFailure(call: Call<DocumentsResponse>, t: Throwable) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Search failed", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun resetChips() {
        if (!::chips.isInitialized) return
        val chipAll = view?.findViewById<TextView>(R.id.chip_all) ?: return
        for (chip in chips) {
            if (chip == chipAll) {
                chip.setBackgroundResource(R.drawable.bg_chip_active)
                chip.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_inactive)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_text))
            }
        }
    }

    private fun setupRecyclerView(view: View) {
        rvDocs = view.findViewById(R.id.rv_docs)
        rvDocs.layoutManager = LinearLayoutManager(requireContext())
        adapter = SearchDocsAdapter(emptyList())
        rvDocs.adapter = adapter
    }

    private fun setupChips(view: View) {
        val chipAll = view.findViewById<TextView>(R.id.chip_all)
        val chipPolicies = view.findViewById<TextView>(R.id.chip_policies)
        val chipSchemes = view.findViewById<TextView>(R.id.chip_schemes)
        val chipCirculars = view.findViewById<TextView>(R.id.chip_circulars)
        val chipReports = view.findViewById<TextView>(R.id.chip_reports)
        val chipGuidelines = view.findViewById<TextView>(R.id.chip_guidelines)
        val chipNotifications = view.findViewById<TextView>(R.id.chip_notifications)
        
        chips = listOf(chipAll, chipPolicies, chipSchemes, chipCirculars, chipReports, chipGuidelines, chipNotifications)

        chipAll.setOnClickListener { filterDocs("All", chipAll) }
        chipPolicies.setOnClickListener { filterDocs("Policies", chipPolicies) }
        chipSchemes.setOnClickListener { filterDocs("Schemes", chipSchemes) }
        chipCirculars.setOnClickListener { filterDocs("Circulars", chipCirculars) }
        chipReports.setOnClickListener { filterDocs("Reports", chipReports) }
        chipGuidelines.setOnClickListener { filterDocs("Guidelines", chipGuidelines) }
        chipNotifications.setOnClickListener { filterDocs("Notifications", chipNotifications) }
    }

    private fun filterDocs(category: String, activeChip: TextView) {
        for (chip in chips) {
            if (chip == activeChip) {
                chip.setBackgroundResource(R.drawable.bg_chip_active)
                chip.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_inactive)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_text))
            }
        }

        val filteredList = if (category == "All") {
            allDocs
        } else {
            allDocs.filter { it.category.equals(category, ignoreCase = true) }
        }
        
        adapter.updateList(filteredList)
    }
}
