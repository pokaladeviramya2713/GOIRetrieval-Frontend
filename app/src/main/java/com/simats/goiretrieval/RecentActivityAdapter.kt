package com.simats.goiretrieval

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.simats.goiretrieval.api.ApiActivity

class RecentActivityAdapter(
    private var activities: List<ApiActivity>,
    private val showHeaders: Boolean = true,
    private val onItemClick: (ApiActivity) -> Unit
) : RecyclerView.Adapter<RecentActivityAdapter.ActivityViewHolder>() {

    class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateHeader: TextView = view.findViewById(R.id.tv_date_header)
        val icon: ImageView = view.findViewById(R.id.iv_activity_icon)
        val detail: TextView = view.findViewById(R.id.tv_activity_detail)
        val time: TextView = view.findViewById(R.id.tv_activity_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = activities[position]
        
        // Show date header only if it's the first item for that date (and headers are enabled)
        if (showHeaders && (position == 0 || activity.date_label != activities[position - 1].date_label)) {
            holder.dateHeader.visibility = View.VISIBLE
            holder.dateHeader.text = activity.date_label
        } else {
            holder.dateHeader.visibility = View.GONE
        }

        holder.detail.text = activity.detail
        holder.time.text = activity.time

        // Configure icons and colors based on activity type (Case-Insensitive)
        val type = activity.type.lowercase()
        when {
            type.contains("audit") -> {
                holder.icon.setImageResource(R.drawable.ic_ai_assist_nav)
                holder.icon.setBackgroundResource(R.drawable.bg_stat_icon_purple)
                holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.navy_home))
            }
            type.contains("chat") || type.contains("chatbot") || type.contains("query") -> {
                holder.icon.setImageResource(R.drawable.ic_ai_queries)
                holder.icon.setBackgroundResource(R.drawable.bg_stat_icon_purple)
                holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.navy_home))
            }
            type.contains("search") -> {
                holder.icon.setImageResource(R.drawable.ic_search_nav)
                holder.icon.setBackgroundResource(R.drawable.bg_stat_icon_blue)
                holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.navy_home))
            }
            type.contains("view") || type.contains("document") -> {
                holder.icon.setImageResource(R.drawable.ic_documents)
                holder.icon.setBackgroundResource(R.drawable.bg_stat_icon_orange)
                holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.orange_primary))
            }
            type.contains("download") || type.contains("export") -> {
                holder.icon.setImageResource(R.drawable.ic_download)
                holder.icon.setBackgroundResource(R.drawable.bg_stat_icon_green)
                holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.green_primary))
            }
            type.contains("login") || type.contains("signup") || type.contains("user") -> {
                holder.icon.setImageResource(R.drawable.ic_user)
                holder.icon.setBackgroundResource(R.drawable.bg_stat_icon_lavender)
                holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.navy_home))
            }
            else -> {
                holder.icon.setImageResource(R.drawable.ic_pulse)
                holder.icon.setBackgroundResource(R.drawable.bg_stat_icon_blue)
                holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.navy_home))
            }
        }

        holder.itemView.setOnClickListener { onItemClick(activity) }
    }

    override fun getItemCount() = activities.size

    fun updateData(newActivities: List<ApiActivity>) {
        activities = newActivities
        notifyDataSetChanged()
    }
}
