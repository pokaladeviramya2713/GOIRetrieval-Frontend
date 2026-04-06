package com.simats.goiretrieval

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.goiretrieval.api.AuditDocument

class AuditDetailPagerAdapter(private val doc: AuditDocument) :
    RecyclerView.Adapter<AuditDetailPagerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLabel: TextView = view.findViewById(R.id.tv_content_label)
        val tvBody: TextView = view.findViewById(R.id.tv_content_body)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_audit_page_content, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (position) {
            0 -> {
                holder.tvLabel.text = "AI Summary & Overview"
                holder.tvBody.text = doc.summary ?: "No summary available."
            }
            1 -> {
                holder.tvLabel.text = "Original Document Segment"
                holder.tvBody.text = doc.original_content ?: "Original document text not found."
            }
            2 -> {
                holder.tvLabel.text = "AI Critical Analysis & Compliance"
                holder.tvBody.text = doc.ai_analysis ?: "Structural analysis still processing."
            }
        }
    }

    override fun getItemCount(): Int = 3
}
