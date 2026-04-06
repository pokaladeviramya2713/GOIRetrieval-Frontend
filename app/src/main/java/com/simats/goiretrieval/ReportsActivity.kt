package com.simats.goiretrieval

import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.simats.goiretrieval.api.ApiDocument
import com.simats.goiretrieval.api.DocumentsResponse
import com.simats.goiretrieval.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : AppCompatActivity() {

    private lateinit var rvReports: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var cgFilterDays: ChipGroup
    private var allReports = mutableListOf<ApiDocument>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reports)

        progressBar = findViewById(R.id.pb_reports)
        rvReports = findViewById(R.id.rv_reports_grid)
        cgFilterDays = findViewById(R.id.cg_filter_days)
        
        setupRecyclerView()
        setupListeners()
        fetchReports()
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        cgFilterDays.setOnCheckedChangeListener { _, _ ->
            applyFilter()
        }

        findViewById<ImageView>(R.id.iv_info_reports).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("AI Audit Reports")
                .setMessage("This section contains your personalized AI Audit Briefings.\n\n" +
                        "• Day Wise: Sorted by the date and time of generation.\n" +
                        "• Private: Only reports you generate and download appear here.\n" +
                        "• Exportable: Open in your favorite PDF viewer or share with others.\n")
                .setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        setupRecyclerView()
        fetchReports()
    }

    private fun setupRecyclerView() {
        rvReports.layoutManager = GridLayoutManager(this, 2)
    }

    private fun fetchReports() {
        progressBar.visibility = View.VISIBLE
        val userId = SessionManager.getInstance(this).getUserId()
        RetrofitClient.instance.getDocuments("Reports", userId).enqueue(object : Callback<DocumentsResponse> {
            override fun onResponse(call: Call<DocumentsResponse>, response: Response<DocumentsResponse>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.status == "success") {
                    val rawReports = response.body()?.documents ?: emptyList()
                    // Filter out official docs and store in allReports
                    allReports = rawReports.filter { it.id != null && !it.id!!.startsWith("DOC") }.toMutableList()
                    applyFilter() // Show initial filtered list (All by default)
                }
            }

            override fun onFailure(call: Call<DocumentsResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@ReportsActivity, "Failed to load reports", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun applyFilter() {
        val filteredList = when (cgFilterDays.checkedChipId) {
            R.id.chip_today -> filterByDays(0)
            R.id.chip_7_days -> filterByDays(7)
            R.id.chip_30_days -> filterByDays(30)
            else -> allReports // chip_all or none
        }
        updateRecyclerView(filteredList)
    }

    private fun filterByDays(days: Int): List<ApiDocument> {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        
        if (days == 0) {
            // Today: Start from 00:00
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        } else {
            // Last N days
            calendar.add(Calendar.DAY_OF_YEAR, -days)
        }
        
        val cutoffDate = calendar.time

        return allReports.filter { doc ->
            try {
                // Many reports use "dd MMM yyyy, HH:mm"
                val dateStr = doc.date ?: return@filter false
                val docDate = sdf.parse(dateStr)
                docDate != null && docDate.after(cutoffDate)
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun updateRecyclerView(reports: List<ApiDocument>) {
        rvReports.adapter = ReportsAdapter(reports, { doc ->
            openDetail(doc)
        }, { doc ->
            downloadDocument(doc)
        }, { doc ->
            confirmDeleteReport(doc)
        })

        if (reports.isEmpty() && allReports.isNotEmpty()) {
            Toast.makeText(this, "No reports found for this period", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadDocument(doc: ApiDocument) {
        val docId = doc.id ?: return
        val docTitle = doc.title ?: "Document"
        
        try {
            val downloadManager = getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            
            // Prioritize the official source URL if available (for real reports)
            val downloadUrl = if (!doc.source.isNullOrEmpty() && doc.source!!.contains("http")) {
                doc.source!!
            } else {
                "${RetrofitClient.BASE_URL}content/Reports/${docId}.pdf"
            }
            
            val request = android.app.DownloadManager.Request(android.net.Uri.parse(downloadUrl))
                .setTitle("Downloading $docTitle")
                .setDescription("GOI Retrieval Official Report")
                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "${docTitle.replace(" ", "_")}.pdf")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            downloadManager.enqueue(request)
            Toast.makeText(this, "Download started to gallery/files...", Toast.LENGTH_SHORT).show()
            ActivityLogger.log(this, "Download", "Downloaded \"$docTitle\"", docId)
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openDetail(doc: ApiDocument) {
        val docId = doc.id ?: return
        val docTitle = doc.title ?: "Report"
        val filename = "${docId}.pdf"

        // Use the ProgressBar from layout instead of deprecated ProgressDialog
        progressBar.visibility = View.VISIBLE

        RetrofitClient.instance.downloadReport(filename).enqueue(object : retrofit2.Callback<okhttp3.ResponseBody> {
            override fun onResponse(call: retrofit2.Call<okhttp3.ResponseBody>, response: retrofit2.Response<okhttp3.ResponseBody>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val file = File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), filename)
                        Thread {
                            var inputStream: java.io.InputStream? = null
                            var outputStream: java.io.FileOutputStream? = null
                            try {
                                inputStream = body.byteStream()
                                outputStream = FileOutputStream(file)
                                inputStream.copyTo(outputStream)
                                
                                runOnUiThread {
                                    progressBar.visibility = View.GONE
                                    openDownloadedFile(docId, docTitle)
                                }
                            } catch (e: Exception) {
                                runOnUiThread {
                                    progressBar.visibility = View.GONE
                                    Toast.makeText(this@ReportsActivity, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                try { inputStream?.close() } catch (e: Exception) {}
                                try { outputStream?.close() } catch (e: Exception) {}
                            }
                        }.start()
                    } else {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@ReportsActivity, "Empty file", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@ReportsActivity, "Server Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<okhttp3.ResponseBody>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@ReportsActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
        
        ActivityLogger.log(this, "View", "Opened official report: $docTitle", docId)
    }

    private fun openDownloadedFile(docId: String, docTitle: String) {
        val file = File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), "${docId}.pdf")
        if (file.exists()) {
            try {
                val contentUri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                
                val chooser = Intent.createChooser(intent, "Open Report with...")
                startActivity(chooser)
            } catch (e: Exception) {
                android.util.Log.e("ReportsActivity", "Open failed", e)
                Toast.makeText(this, "No PDF Viewer found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "File not found after download", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteReport(doc: ApiDocument) {
        val docId = doc.id ?: return
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Report")
            .setMessage("Are you sure you want to permanently delete '${doc.title}'?")
            .setPositiveButton("Delete") { _, _ -> deleteReport(docId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteReport(docId: String) {
        progressBar.visibility = View.VISIBLE
        RetrofitClient.instance.deleteReport(mapOf("doc_id" to docId)).enqueue(object : Callback<com.simats.goiretrieval.api.SignupResponse> {
            override fun onResponse(call: Call<com.simats.goiretrieval.api.SignupResponse>, response: Response<com.simats.goiretrieval.api.SignupResponse>) {
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.status == "success") {
                    Toast.makeText(this@ReportsActivity, "Report deleted", Toast.LENGTH_SHORT).show()
                    fetchReports() // Refresh list
                } else {
                    Toast.makeText(this@ReportsActivity, "Failed to delete: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.simats.goiretrieval.api.SignupResponse>, t: Throwable) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@ReportsActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
