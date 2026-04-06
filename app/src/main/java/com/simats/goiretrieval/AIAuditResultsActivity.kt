package com.simats.goiretrieval

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simats.goiretrieval.api.AuditDocument
import com.simats.goiretrieval.api.AuditResponse
import com.simats.goiretrieval.api.RetrofitClient
import com.simats.goiretrieval.api.BriefingExportRequest
import com.simats.goiretrieval.utils.AuditReportUtils
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayList

class AIAuditResultsActivity : AppCompatActivity(), AuditResultsAdapter.OnExportClickListener {

    private lateinit var rvResults: RecyclerView
    private lateinit var adapter: AuditResultsAdapter
    private lateinit var llLoading: LinearLayout
    private lateinit var tvFilesCount: TextView
    private lateinit var tvAvgCompliance: TextView
    private lateinit var tvOverallSummary: TextView
    private lateinit var tvSourceUrl: TextView
    private var currentMode = "SCENARIO"
    private var currentQuestions = ""
    private var currentResults = mutableListOf<AuditDocument>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_audit_results)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        initializeViews()
        setupRecyclerView()
        processAudit()
        
        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }
        
        // --- Removed: Global Export Button as requested ---
        val ivExport = findViewById<ImageView>(R.id.iv_export_pdf)
        ivExport.visibility = View.GONE
    }

    private fun initializeViews() {
        rvResults = findViewById(R.id.rv_audit_results)
        llLoading = findViewById(R.id.ll_loading)
        tvFilesCount = findViewById(R.id.tv_files_count)
        tvAvgCompliance = findViewById(R.id.tv_avg_compliance)
        tvOverallSummary = findViewById(R.id.tv_overall_ai_summary)
        tvSourceUrl = findViewById(R.id.tv_source_url)
    }

    private fun setupRecyclerView() {
        rvResults.layoutManager = LinearLayoutManager(this)
        adapter = AuditResultsAdapter(emptyList(), this)
        rvResults.adapter = adapter
    }

    private fun processAudit() {
        @Suppress("DEPRECATION")
        val fileUris = intent.getParcelableArrayListExtra<Uri>("FILE_URIS") ?: emptyList<Uri>()


        val sourceUrl = intent.getStringExtra("SOURCE_URL") ?: "General Upload"
        val questions = intent.getStringExtra("QUESTIONS") ?: ""
        currentQuestions = questions
        val mode = intent.getStringExtra("AUDIT_MODE") ?: "SCENARIO"
        currentMode = mode

        tvSourceUrl.text = "Source: $sourceUrl"
        tvFilesCount.text = "${fileUris.size} Files Selected"

        val userId = SessionManager.getInstance(this).getUserId()
        if (userId == -1) {
            Toast.makeText(this, "User session expired", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Show loading
        llLoading.visibility = View.VISIBLE
        rvResults.visibility = View.GONE
        tvOverallSummary.text = "AI is currently unpacking and analyzing your documents... This may take a minute."

        // Prepare Multipart request
        val userIdPart = userId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val sourceUrlPart = sourceUrl.toRequestBody("text/plain".toMediaTypeOrNull())
        val questionsPart = questions.toRequestBody("text/plain".toMediaTypeOrNull())
        val modePart = mode.toRequestBody("text/plain".toMediaTypeOrNull())
        
        val fileParts = mutableListOf<MultipartBody.Part>()
        fileUris.forEach { uri ->
            getFileFromUri(uri)?.let { file ->
                val mimeType = contentResolver.getType(uri) ?: "application/pdf"
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                fileParts.add(MultipartBody.Part.createFormData("files", file.name, requestFile))
            }
        }

        RetrofitClient.instance.aiAuditBulk(userIdPart, sourceUrlPart, questionsPart, modePart, fileParts)
            .enqueue(object : Callback<AuditResponse> {
                override fun onResponse(call: Call<AuditResponse>, response: Response<AuditResponse>) {
                    llLoading.visibility = View.GONE
                    if (response.isSuccessful) {
                        val auditResponse = response.body()
                        if (auditResponse?.status == "success") {
                            displayResults(auditResponse.documents ?: emptyList(), auditResponse.system_audit_summary)
                        } else {
                            showError("Audit Error: ${auditResponse?.message}")
                        }
                    } else {
                        showError("Server Error: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<AuditResponse>, t: Throwable) {
                    llLoading.visibility = View.GONE
                    showError("Network Failure: ${t.message}")
                }
            })
    }

    private fun getFileFromUri(uri: Uri): File? {
        val fileName = getFileName(uri) ?: "temp_file"
        val file = File(cacheDir, fileName)
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun showError(msg: String) {
        tvOverallSummary.text = msg
        tvOverallSummary.setTextColor(Color.RED)
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun displayResults(results: List<AuditDocument>, summary: String?) {
        rvResults.visibility = View.VISIBLE
        currentResults.clear()
        currentResults.addAll(results)
        adapter.updateList(results)
        
        tvFilesCount.text = "${results.size} Segments"
        
        if (results.isNotEmpty()) {
            val avg = results.map { it.compliance_score ?: 0 }.average().toInt()
            tvAvgCompliance.text = "$avg%"
            if (avg < 50) tvAvgCompliance.setTextColor(Color.RED)
            else if (avg < 80) tvAvgCompliance.setTextColor(Color.YELLOW)
            else tvAvgCompliance.setTextColor(Color.parseColor("#4CAF50"))
        }
        
        val displaySummary = summary ?: "No executive summary provided."
        tvOverallSummary.text = HtmlCompat.fromHtml(displaySummary, HtmlCompat.FROM_HTML_MODE_COMPACT)
        tvOverallSummary.setTextColor(Color.parseColor("#757575"))
        
        ActivityLogger.log(this, "AI Audit", "Completed $currentMode audit for ${results.size} segments")
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = cursor.getString(index)
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    override fun onExportClick(doc: AuditDocument) {
        // Sync with server AND launch viewer
        exportAndOpenReport(listOf(doc), doc.title ?: "Audit_Report")
    }

    private fun exportAndOpenReport(auditData: List<AuditDocument>, baseName: String) {
        val userId = SessionManager.getInstance(this).getUserId()
        val reportId = "Audit_${System.currentTimeMillis()}"
        val request = BriefingExportRequest(userId, auditData, currentQuestions, reportId)

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setMessage("Generating & Opening Executive Report...")
            .setCancelable(false)
            .show()

        RetrofitClient.instance.exportAuditPdf(request).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    val body = response.body()
                    val contentType = response.headers()["Content-Type"] ?: ""
                    
                    if (body != null && (contentType.contains("application/pdf") || contentType.contains("application/octet-stream"))) {
                        val fileName = "${reportId}.pdf"
                        val dir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: cacheDir
                        val file = File(dir, fileName)
                        
                        Thread {
                            try {
                                body.byteStream().use { input ->
                                    FileOutputStream(file).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                
                                runOnUiThread {
                                    dialog.dismiss()
                                    try {
                                        val contentUri = FileProvider.getUriForFile(
                                            this@AIAuditResultsActivity,
                                            "${applicationContext.packageName}.fileprovider",
                                            file
                                        )

                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(contentUri, "application/pdf")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                                        }
                                        startActivity(Intent.createChooser(intent, "Open PDF with..."))
                                    } catch (e: Exception) {
                                        Toast.makeText(this@AIAuditResultsActivity, "No PDF Viewer found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                runOnUiThread {
                                    dialog.dismiss()
                                    Toast.makeText(this@AIAuditResultsActivity, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }.start()
                    } else {
                        dialog.dismiss()
                        Toast.makeText(this@AIAuditResultsActivity, "Export failed: Server returned non-PDF content", Toast.LENGTH_LONG).show()
                    }
                } else {
                    dialog.dismiss()
                    val errorMsg = try { response.errorBody()?.string() ?: "Unknown error" } catch(e: Exception) { "Error ${response.code()}" }
                    Toast.makeText(this@AIAuditResultsActivity, "Server Error: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                dialog.dismiss()
                Toast.makeText(this@AIAuditResultsActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

