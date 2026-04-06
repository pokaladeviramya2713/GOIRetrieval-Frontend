package com.simats.goiretrieval

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.goiretrieval.api.ApiDocument

class ReportsAdapter(
    private val reports: List<ApiDocument>,
    private val onItemClick: (ApiDocument) -> Unit,
    private val onDownloadClick: (ApiDocument) -> Unit,
    private val onDeleteClick: (ApiDocument) -> Unit
) : RecyclerView.Adapter<ReportsAdapter.ReportViewHolder>() {

    class ReportViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_report_title)
        val dept: TextView = view.findViewById(R.id.tv_report_dept)
        val btnDownload: View = view.findViewById(R.id.btn_report_download)
        val btnDelete: View = view.findViewById(R.id.btn_report_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_report_grid, parent, false)
        return ReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val doc = reports[position]
        holder.title.text = doc.title
        holder.dept.text = doc.department ?: "Ministry of Education"
        
        holder.itemView.setOnClickListener { onItemClick(doc) }
        holder.btnDownload.setOnClickListener { onDownloadClick(doc) }
        holder.btnDelete.setOnClickListener { onDeleteClick(doc) }
    }

    override fun getItemCount() = reports.size
}
