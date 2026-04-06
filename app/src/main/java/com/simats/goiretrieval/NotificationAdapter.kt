package com.simats.goiretrieval

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.simats.goiretrieval.api.ApiNotification

class NotificationAdapter(
    private var notifications: List<ApiNotification>,
    private val onNotificationClick: (ApiNotification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.card_notif)
        val rlIconContainer: View = view.findViewById(R.id.rl_icon_container)
        val ivIcon: ImageView = view.findViewById(R.id.iv_notif_icon)
        val tvTitle: TextView = view.findViewById(R.id.tv_notif_title)
        val tvMessage: TextView = view.findViewById(R.id.tv_notif_message)
        val tvTime: TextView = view.findViewById(R.id.tv_notif_time)
        val dotUnread: View = view.findViewById(R.id.dot_unread)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notif = notifications[position]
        val context = holder.itemView.context
        
        holder.tvTitle.text = notif.title
        holder.tvMessage.text = notif.message
        holder.tvTime.text = notif.time_ago
        
        // Icon and Background based on type
        when (notif.type.lowercase()) {
            "success" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_check_green)
                holder.rlIconContainer.setBackgroundResource(R.drawable.bg_icon_circle_green)
            }
            "warning" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_warning_orange)
                holder.rlIconContainer.setBackgroundResource(R.drawable.bg_icon_circle_orange)
            }
            "error" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_alert_red)
                holder.rlIconContainer.setBackgroundResource(R.drawable.bg_icon_red_light)
            }
            else -> {
                holder.ivIcon.setImageResource(R.drawable.ic_info_blue)
                holder.rlIconContainer.setBackgroundResource(R.drawable.bg_icon_circle_blue)
            }
        }
        
        // Read/Unread styling
        if (notif.is_read) {
            holder.card.setCardBackgroundColor(Color.WHITE)
            holder.card.strokeWidth = 0
            holder.dotUnread.visibility = View.GONE
        } else {
            holder.card.setCardBackgroundColor(Color.parseColor("#F8FAFF"))
            holder.card.strokeWidth = 2
            holder.card.strokeColor = Color.parseColor("#DDE6FF")
            holder.dotUnread.visibility = View.VISIBLE
        }
        
        holder.itemView.setOnClickListener {
            onNotificationClick(notif)
        }
    }

    override fun getItemCount() = notifications.size

    fun updateList(newList: List<ApiNotification>) {
        notifications = newList
        notifyDataSetChanged()
    }
}
