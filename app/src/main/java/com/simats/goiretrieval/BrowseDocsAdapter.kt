package com.simats.goiretrieval

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

import com.simats.goiretrieval.api.ApiDocument
import com.google.gson.Gson

// BrowseDocItem is now replaced by ApiDocument

class BrowseDocsAdapter(
    private var docsList: List<ApiDocument>,
    var isSavedList: Boolean = false
) : RecyclerView.Adapter<BrowseDocsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        val tvType: TextView = itemView.findViewById(R.id.tv_type) // In item_search_doc it is tv_type
        val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        val tvExcerpt: TextView = itemView.findViewById(R.id.tv_excerpt)
        val ivBookmark: ImageView = itemView.findViewById(R.id.iv_bookmark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_doc, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc = docsList[position]
        holder.tvTitle.text = doc.title ?: "Untitled Document"
        holder.tvType.text = doc.category ?: "Policy"
        holder.tvDate.text = doc.date ?: "N/A"
        holder.tvExcerpt.text = doc.excerpt ?: doc.description ?: "No description available."

        // Handle Bookmark Icon state
        val userId = SessionManager.getInstance(holder.itemView.context).getUserId()
        val isSaved = DocumentRepository.isSaved(doc.id)
        if (isSaved) {
            holder.ivBookmark.setImageResource(R.drawable.ic_bookmark_filled)
            holder.ivBookmark.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1565C0"))
        } else {
            holder.ivBookmark.setImageResource(R.drawable.ic_bookmark_outline)
            holder.ivBookmark.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#B0BEC5"))
        }
        
        holder.ivBookmark.setOnClickListener {
            if (userId != -1) {
                if (isSaved) {
                    unsaveDocument(holder, userId, doc)
                } else {
                    saveDocument(holder, userId, doc)
                }
            }
        }

        // Navigate to Document Preview
        holder.itemView.setOnClickListener {
            if (userId != -1) {
                val request = com.simats.goiretrieval.api.SavedDocRequest(userId, doc.id, doc.title, doc.category, doc.date, doc.format, doc.source, doc.description, doc.content)
                com.simats.goiretrieval.api.RetrofitClient.instance.recordView(request).enqueue(object : retrofit2.Callback<com.simats.goiretrieval.api.SignupResponse> {
                    override fun onResponse(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, response: retrofit2.Response<com.simats.goiretrieval.api.SignupResponse>) {}
                    override fun onFailure(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, t: Throwable) {}
                })
            }

            val intent = android.content.Intent(holder.itemView.context, ArticleViewActivity::class.java).apply {
                putExtra("DOC_ID", doc.id)
                putExtra("DOC_TITLE", doc.title)
                putExtra("DOC_TYPE", doc.category)
                putExtra("DOC_FOLDER", doc.folder ?: "pdf")
                putExtra("DOC_FILENAME", doc.filename ?: "${doc.id}.pdf")
                
                // Pass organized content directly to the article view for fast loading
                val docOrganizedContent = """
                    DESCRIPTION:
                    ${doc.description ?: "N/A"}
                    
                    ELIGIBILITY:
                    ${doc.eligibility ?: "N/A"}
                    
                    BENEFITS:
                    ${doc.benefits ?: "N/A"}
                    
                    FULL CONTENT:
                    ${doc.content ?: "No content available."}
                """.trimIndent()
                putExtra("ARTICLE_CONTENT", docOrganizedContent)
                putExtra("IS_ORGANIZED", true)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    private fun saveDocument(holder: ViewHolder, userId: Int, doc: ApiDocument) {
        val request = com.simats.goiretrieval.api.SavedDocRequest(userId, doc.id, doc.title, doc.category, doc.date, doc.format, doc.source, doc.description, doc.content)
        com.simats.goiretrieval.api.RetrofitClient.instance.saveDocument(request).enqueue(object : retrofit2.Callback<com.simats.goiretrieval.api.SignupResponse> {
            override fun onResponse(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, response: retrofit2.Response<com.simats.goiretrieval.api.SignupResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    DocumentRepository.addSavedDoc(doc)
                    holder.ivBookmark.setImageResource(R.drawable.ic_bookmark_filled)
                    holder.ivBookmark.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1565C0"))
                    android.widget.Toast.makeText(holder.itemView.context, "Saved to Docs", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, t: Throwable) {}
        })
    }

    private fun unsaveDocument(holder: ViewHolder, userId: Int, doc: ApiDocument) {
        doc.id?.trim()?.uppercase()?.let { id ->
            com.simats.goiretrieval.api.RetrofitClient.instance.deleteSavedDocument(userId, id).enqueue(object : retrofit2.Callback<com.simats.goiretrieval.api.SignupResponse> {
                override fun onResponse(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, response: retrofit2.Response<com.simats.goiretrieval.api.SignupResponse>) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        DocumentRepository.removeSavedDoc(doc.id)
                        
                        if (isSavedList) {
                            val position = holder.adapterPosition
                            if (position != RecyclerView.NO_POSITION) {
                                val mutableList = docsList.toMutableList()
                                mutableList.removeAt(position)
                                docsList = mutableList
                                notifyItemRemoved(position)
                            }
                        } else {
                            holder.ivBookmark.setImageResource(R.drawable.ic_bookmark_outline)
                            holder.ivBookmark.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#B0BEC5"))
                        }
                        android.widget.Toast.makeText(holder.itemView.context, "Removed from Docs", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, t: Throwable) {}
            })
        }
    }

    override fun getItemCount(): Int = docsList.size

    fun updateList(newList: List<ApiDocument>) {
        docsList = newList
        notifyDataSetChanged()
    }
}
