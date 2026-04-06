package com.simats.goiretrieval

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.goiretrieval.api.ApiDocument
import com.simats.goiretrieval.api.RetrofitClient

class SearchDocsAdapter(private var docs: List<ApiDocument>) :
    RecyclerView.Adapter<SearchDocsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvCategory: TextView = view.findViewById(R.id.tv_type)
        val tvDate: TextView = view.findViewById(R.id.tv_date)
        val tvExcerpt: TextView = view.findViewById(R.id.tv_excerpt)
        val ivBookmark: ImageView = view.findViewById(R.id.iv_bookmark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_doc, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val doc = docs[position]
        holder.tvTitle.text = doc.title
        holder.tvCategory.text = doc.category
        holder.tvDate.text = doc.date
        holder.tvExcerpt.text = doc.excerpt ?: doc.description

        // Handle Bookmark Icon state
        val isSaved = DocumentRepository.isSaved(doc.id)
        if (isSaved) {
            holder.ivBookmark.setImageResource(R.drawable.ic_bookmark_filled)
            holder.ivBookmark.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1565C0"))
        } else {
            holder.ivBookmark.setImageResource(R.drawable.ic_bookmark_outline)
            holder.ivBookmark.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#B0BEC5"))
        }

        holder.ivBookmark.setOnClickListener {
            val context = holder.itemView.context
            val userId = SessionManager.getInstance(context).getUserId()
            
            if (userId == -1) {
                android.widget.Toast.makeText(context, "Please sign in to save documents", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isSaved) {
                unsaveDocument(holder, userId, doc)
            } else {
                saveDocument(holder, userId, doc)
            }
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ArticleViewActivity::class.java).apply {
                putExtra("DOC_ID", doc.id)
                putExtra("DOC_TITLE", doc.title)
                putExtra("DOC_TYPE", doc.category)
                putExtra("DOC_FOLDER", doc.folder ?: "pdf")
                putExtra("DOC_FILENAME", doc.filename ?: "${doc.id}.pdf")
                putExtra("IS_ORGANIZED", false)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    private fun saveDocument(holder: ViewHolder, userId: Int, doc: ApiDocument) {
        val context = holder.itemView.context
        val request = com.simats.goiretrieval.api.SavedDocRequest(
            userId = userId,
            docId = doc.id,
            title = doc.title,
            category = doc.category,
            date = doc.date,
            format = doc.format ?: "PDF",
            source = doc.source ?: "GOI Database",
            description = doc.description ?: doc.excerpt,
            content = doc.content
        )

        RetrofitClient.instance.saveDocument(request).enqueue(object : retrofit2.Callback<com.simats.goiretrieval.api.SignupResponse> {
            override fun onResponse(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, response: retrofit2.Response<com.simats.goiretrieval.api.SignupResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    DocumentRepository.addSavedDoc(doc)
                    holder.ivBookmark.setImageResource(R.drawable.ic_bookmark_filled)
                    holder.ivBookmark.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1565C0"))
                    android.widget.Toast.makeText(context, "Saved to library!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, t: Throwable) {
                android.widget.Toast.makeText(context, "Error: ${t.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun unsaveDocument(holder: ViewHolder, userId: Int, doc: ApiDocument) {
        val context = holder.itemView.context
        doc.id?.trim()?.uppercase()?.let { id ->
            RetrofitClient.instance.deleteSavedDocument(userId, id).enqueue(object : retrofit2.Callback<com.simats.goiretrieval.api.SignupResponse> {
                override fun onResponse(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, response: retrofit2.Response<com.simats.goiretrieval.api.SignupResponse>) {
                    if (response.isSuccessful && response.body()?.status == "success") {
                        DocumentRepository.removeSavedDoc(doc.id)
                        holder.ivBookmark.setImageResource(R.drawable.ic_bookmark_outline)
                        holder.ivBookmark.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#B0BEC5"))
                        android.widget.Toast.makeText(context, "Removed from library", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, t: Throwable) {
                    android.widget.Toast.makeText(context, "Error: ${t.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun getItemCount() = docs.size

    fun updateList(newList: List<ApiDocument>) {
        docs = newList
        notifyDataSetChanged()
    }
}
