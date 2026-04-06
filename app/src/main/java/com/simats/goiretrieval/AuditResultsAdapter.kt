package com.simats.goiretrieval

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.goiretrieval.api.AuditDocument

class AuditResultsAdapter(
    private var docs: List<AuditDocument>,
    private val exportListener: OnExportClickListener
) : RecyclerView.Adapter<AuditResultsAdapter.ViewHolder>() {

    interface OnExportClickListener {
        fun onExportClick(doc: AuditDocument)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_audit_doc_title)
        val tvCategory: TextView = view.findViewById(R.id.tv_audit_doc_category)
        val tvPct: TextView = view.findViewById(R.id.tv_compliance_pct)
        val badge: View = view.findViewById(R.id.compliance_badge)
        val ivExport: View = view.findViewById(R.id.iv_item_download) // Re-using existing ID but naming Export
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_audit_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc = docs[position]
        // Use HtmlCompat to handle any HTML bold/markdown tags from AI
        val title = doc.title ?: "Untitled Document"
        holder.tvTitle.text = androidx.core.text.HtmlCompat.fromHtml(title, androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY)
        
        holder.tvCategory.text = "${doc.category ?: "Document"} • ${doc.format ?: "PDF"}"
        
        val score = doc.compliance_score ?: 0
        holder.tvPct.text = "$score%"
        
        // Color coding
        when {
            score >= 80 -> {
                holder.badge.setBackgroundResource(R.drawable.bg_pct_badge)
                holder.tvPct.setTextColor(Color.parseColor("#33691E")) // Green
            }
            score >= 50 -> {
                holder.badge.setBackgroundResource(R.drawable.bg_chip_light_blue)
                holder.tvPct.setTextColor(Color.parseColor("#0277BD")) // Blue
            }
            else -> {
                holder.badge.setBackgroundResource(R.drawable.bg_icon_red_light)
                holder.tvPct.setTextColor(Color.parseColor("#C62828")) // Red
            }
        }

        // Download Icon (PDF Action)
        (holder.ivExport as? android.widget.ImageView)?.setImageResource(R.drawable.ic_download)
        holder.ivExport.setOnClickListener {
            exportListener.onExportClick(doc)
        }

        // Unified Action: Both click and Export open PDF
        holder.itemView.setOnClickListener {
            exportListener.onExportClick(doc)
        }
    }

    override fun getItemCount() = docs.size

    fun updateList(newList: List<AuditDocument>) {
        docs = newList
        notifyDataSetChanged()
    }
}
