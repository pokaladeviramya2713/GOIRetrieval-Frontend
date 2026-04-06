package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.goiretrieval.api.ApiActivity
import com.simats.goiretrieval.api.RecentActivityResponse
import com.simats.goiretrieval.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private lateinit var recentActivityAdapter: RecentActivityAdapter
    private var trendCounts = arrayOf(0, 0, 0, 0, 0, 0, 0)
    private var trendDays = arrayOf("", "", "", "", "", "", "")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupQuickActions(view)
        setupRecentActivity(view)
        setupInteractiveGraph(view)
        setupNotifications(view)
    }

    override fun onResume() {
        super.onResume()
        setupUserInfo()
        applyUserPreferences()
        fetchTodayActivities()
        fetchGlobalStats()
        updateNotificationDot()
    }

    private fun setupNotifications(view: View) {
        val bell = view.findViewById<View>(R.id.rl_notification_bell)
        val dot = view.findViewById<View>(R.id.view_notification_dot)

        bell.setOnClickListener {
            // Hide dot immediately when clicking
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
                if (response.isSuccessful) {
                    val notifications = response.body()?.notifications ?: emptyList()
                    val hasUnread = notifications.any { !it.is_read }
                    dot.visibility = if (hasUnread) View.VISIBLE else View.GONE
                }
            }

            override fun onFailure(call: Call<com.simats.goiretrieval.api.NotificationsResponse>, t: Throwable) {
                // Silently fail for UI polish
            }
        })
    }

    private fun setupQuickActions(view: View) {
        view.findViewById<View>(R.id.card_search_docs).setOnClickListener {
            // Should probably switch fragment instead of activity
            (activity as? MainActivity)?.switchToTab(1)
        }

        view.findViewById<View>(R.id.card_browse_docs).setOnClickListener {
            (activity as? MainActivity)?.switchToTab(2)
        }

        view.findViewById<View>(R.id.card_ai_assist).setOnClickListener {
            (activity as? MainActivity)?.switchToTab(3)
        }

        view.findViewById<View>(R.id.card_reports).setOnClickListener {
            startActivity(Intent(requireContext(), ReportsActivity::class.java))
        }

        view.findViewById<TextView>(R.id.tv_view_all_activity).setOnClickListener {
            startActivity(Intent(requireContext(), RecentActivityLogActivity::class.java))
        }
    }

    private fun applyUserPreferences() {
        val session = SessionManager.getInstance(requireContext())
        val view = view ?: return
        
        view.findViewById<View>(R.id.section_stats).visibility = if (session.shouldShowStats()) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.section_trends_chart).visibility = if (session.shouldShowChart()) View.VISIBLE else View.GONE
        view.findViewById<View>(R.id.section_recent_activity).visibility = if (session.shouldShowActivity()) View.VISIBLE else View.GONE
    }

    private fun setupUserInfo() {
        val view = view ?: return
        val tvDate = view.findViewById<TextView>(R.id.tv_current_date)
        val tvName = view.findViewById<TextView>(R.id.tv_user_name)

        // Set Current Date
        val sdf = java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale.getDefault())
        tvDate.text = sdf.format(java.util.Date())

        // Set User Name
        val userName = SessionManager.getInstance(requireContext()).getUserName() ?: "User"
        tvName.text = userName
    }

    private fun setupRecentActivity(view: View) {
        val rvActivity = view.findViewById<RecyclerView>(R.id.rv_home_activity)
        rvActivity.layoutManager = LinearLayoutManager(requireContext())
        
        recentActivityAdapter = RecentActivityAdapter(emptyList(), false) { activity ->
            if (activity.doc_id != null) {
                val intent = Intent(requireContext(), ArticleViewActivity::class.java).apply {
                    val title = activity.detail.substringAfter("\"").substringBefore("\"")
                    putExtra("DOC_ID", activity.doc_id)
                    putExtra("DOC_TITLE", title)
                    putExtra("DOC_TYPE", "Policy")
                    putExtra("DOC_FOLDER", "json")
                    putExtra("DOC_FILENAME", "${activity.doc_id}.json")
                }
                startActivity(intent)
            }
        }
        rvActivity.adapter = recentActivityAdapter
    }

    private fun fetchTodayActivities() {
        val userId = SessionManager.getInstance(requireContext()).getUserId()
        val view = view ?: return
        val cardActivity = view.findViewById<View>(R.id.card_recent_activity)

        if (userId == -1) return

        RetrofitClient.instance.getActivities(userId)
            .enqueue(object : Callback<RecentActivityResponse> {
                override fun onResponse(call: Call<RecentActivityResponse>, response: Response<RecentActivityResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val allActivities = response.body()?.activities ?: emptyList()
                        val topActivity = allActivities.take(5)
                        
                        if (topActivity.isNotEmpty()) {
                            recentActivityAdapter.updateData(topActivity)
                            cardActivity.visibility = View.VISIBLE
                        } else {
                            cardActivity.visibility = View.GONE
                        }
                        processRetrievalTrends(allActivities)
                    }
                }
                override fun onFailure(call: Call<RecentActivityResponse>, t: Throwable) {}
            })
    }

    private fun processRetrievalTrends(activities: List<ApiActivity>) {
        val displaySdf = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        val parseSdf = java.text.SimpleDateFormat("dd MMM, yyyy", java.util.Locale.getDefault())

        val newTrendCounts = Array(7) { 0 }
        val newTrendDays = Array(7) { "" }

        for (i in 6 downTo 0) {
            val pastCal = java.util.Calendar.getInstance()
            pastCal.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val pastDate = pastCal.time

            val matchLabel: (String) -> Boolean = when (i) {
                0 -> { label -> label == "Today" }
                1 -> { label -> label == "Yesterday" }
                else -> { label -> 
                    val expected = parseSdf.format(pastDate)
                    label == expected
                }
            }

            val dayName = if (i == 0) "Today" else displaySdf.format(pastDate)
            newTrendDays[6 - i] = dayName
            newTrendCounts[6 - i] = activities.filter { it.type.lowercase().contains("audit") }
                                               .count { matchLabel(it.date_label) }
        }

        trendCounts = newTrendCounts
        trendDays = newTrendDays
        
        view?.findViewById<TrendGraphView>(R.id.trend_graph_view)?.setData(newTrendCounts.toIntArray())
        updateTrendLabels()
    }

    private fun updateTrendLabels() {
        val view = view ?: return
        val trendTextViews = listOf(
            view.findViewById<TextView>(R.id.tv_day_0),
            view.findViewById<TextView>(R.id.tv_day_1),
            view.findViewById<TextView>(R.id.tv_day_2),
            view.findViewById<TextView>(R.id.tv_day_3),
            view.findViewById<TextView>(R.id.tv_day_4),
            view.findViewById<TextView>(R.id.tv_day_5),
            view.findViewById<TextView>(R.id.tv_day_6)
        )

        for (i in 0 until 7) {
            trendTextViews[i].text = trendDays[i]
        }
    }

    private fun fetchGlobalStats() {
        val view = view ?: return
        RetrofitClient.instance.getGlobalStats()
            .enqueue(object : Callback<com.simats.goiretrieval.api.GlobalStatsResponse> {
                override fun onResponse(call: Call<com.simats.goiretrieval.api.GlobalStatsResponse>, response: Response<com.simats.goiretrieval.api.GlobalStatsResponse>) {
                    val stats = response.body()
                    if (response.isSuccessful && stats?.status == "success") {
                        fun formatCount(n: Int): String = if (n >= 1000) "%.1fk".format(n / 1000.0) else n.toString()
                        view.findViewById<TextView>(R.id.tv_docs_count).text = formatCount(stats.totalDocuments)
                        view.findViewById<TextView>(R.id.tv_ai_queries_count).text = formatCount(stats.totalAiQueries)
                        view.findViewById<TextView>(R.id.tv_users_count).text = formatCount(stats.totalUsers)
                    }
                }
                override fun onFailure(call: Call<com.simats.goiretrieval.api.GlobalStatsResponse>, t: Throwable) {}
            })
    }

    private fun setupInteractiveGraph(view: View) {
        val graphContainer = view.findViewById<View>(R.id.trends_graph_container)
        val tooltip = view.findViewById<View>(R.id.graph_tooltip_flag)
        val tvDay = view.findViewById<TextView>(R.id.tv_tooltip_day)
        val tvCount = view.findViewById<TextView>(R.id.tv_tooltip_count)
        
        graphContainer.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE -> {
                    val width = v.width.toFloat()
                    val height = v.height.toFloat()
                    val touchX = event.x.coerceIn(0f, width)
                    val sectionWidth = width / 6
                    val dayIndex = (touchX / sectionWidth).toInt().coerceIn(0, 5)
                    val dataIndex = dayIndex + 1
                    
                    tvDay.text = trendDays[dataIndex]
                    tvCount.text = trendCounts[dataIndex].toString()
                    tooltip.visibility = View.VISIBLE
                    tooltip.post {
                        val tWidth = tooltip.width.toFloat()
                        val tHeight = tooltip.height.toFloat()
                        tooltip.x = (touchX - (tWidth / 2)).coerceIn(0f, width - tWidth)
                        val maxCount = trendCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
                        val yRatio = 1f - (trendCounts[dataIndex].toFloat() / maxCount.toFloat() * 0.5f)
                        tooltip.y = (height * yRatio.coerceIn(0.3f, 0.7f)) - tHeight - 10f
                    }
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {}
            }
            true
        }
    }
}
