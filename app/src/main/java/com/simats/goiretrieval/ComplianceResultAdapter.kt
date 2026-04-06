package com.simats.goiretrieval

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

data class ComplianceResult(
    val title: String,
    val type: String,
    val date: String,
    val tags: List<String>,
    val score: Int,
    val complianceLevel: String,
    val summary: String,
    val scoreColor: String,
    val reference: String = "",
    val detailedContent: String = "",
    val organizedContent: String = ""
)

class ComplianceResultAdapter(
    private val results: List<ComplianceResult>
) : RecyclerView.Adapter<ComplianceResultAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_document_title)
        val tvType: TextView = view.findViewById(R.id.tv_document_type)
        val tvDate: TextView = view.findViewById(R.id.tv_document_date)
        val llTags: LinearLayout = view.findViewById(R.id.ll_keyword_tags)
        val tvScorePercent: TextView = view.findViewById(R.id.tv_compliance_score_percent)
        val tvComplianceLevel: TextView = view.findViewById(R.id.tv_compliance_level)
        val progressCompliance: LinearProgressIndicator = view.findViewById(R.id.progress_compliance)
        val tvAiSummary: TextView = view.findViewById(R.id.tv_ai_summary)
        val tvExactReference: TextView = view.findViewById(R.id.tv_exact_reference)
        val btnOpenDocument: MaterialButton = view.findViewById(R.id.btn_open_document)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_compliance_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = results[position]

        holder.tvTitle.text = item.title
        holder.tvType.text = item.type
        holder.tvDate.text = item.date
        holder.tvAiSummary.text = item.summary
        holder.tvExactReference.text = "Reference: \"${item.reference}\""

        // Setup Score & Progress
        holder.tvScorePercent.text = "${item.score} %"
        holder.tvScorePercent.setTextColor(Color.parseColor(item.scoreColor))
        
        holder.tvComplianceLevel.text = item.complianceLevel
        holder.tvComplianceLevel.setTextColor(Color.parseColor(item.scoreColor))
        
        holder.progressCompliance.progress = item.score
        holder.progressCompliance.setIndicatorColor(Color.parseColor(item.scoreColor))

        // Dynamic Tags Setup
        holder.llTags.removeAllViews()
        for (tag in item.tags) {
            val tagView = TextView(holder.itemView.context).apply {
                text = tag
                textSize = 10f
                setTextColor(Color.parseColor("#1A237E"))
                setBackgroundResource(R.drawable.bg_tag_pill)
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F0F4FF"))
                setPadding(
                    (8 * resources.displayMetrics.density).toInt(),
                    (4 * resources.displayMetrics.density).toInt(),
                    (8 * resources.displayMetrics.density).toInt(),
                    (4 * resources.displayMetrics.density).toInt()
                )
                
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.marginEnd = (8 * resources.displayMetrics.density).toInt()
                layoutParams = params
            }
            holder.llTags.addView(tagView)
        }
        
        holder.btnOpenDocument.setOnClickListener {
            val intent = Intent(holder.itemView.context, ArticleViewActivity::class.java).apply {
                putExtra("ARTICLE_TITLE", item.title)
                putExtra("ARTICLE_CONTENT", item.organizedContent)
                putExtra("IS_ORGANIZED", true) // Flag to show it's AI organized
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = results.size
}
