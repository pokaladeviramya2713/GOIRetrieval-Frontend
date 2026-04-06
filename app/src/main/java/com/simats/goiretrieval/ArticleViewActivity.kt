package com.simats.goiretrieval

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.simats.goiretrieval.api.ContentResponse
import com.simats.goiretrieval.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ArticleViewActivity : AppCompatActivity() {

    private var currentContent: String = ""
    private var currentTitle: String = "Document"
    private var currentDocId: String? = null
    private var currentDocType: String? = null
    private var isOrganized: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_article_view)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        currentTitle = intent.getStringExtra("ARTICLE_TITLE") ?: intent.getStringExtra("DOC_TITLE") ?: "Document"
        currentDocId = intent.getStringExtra("DOC_ID")
        currentDocType = intent.getStringExtra("DOC_TYPE") ?: "Report"
        val folder = intent.getStringExtra("FOLDER") ?: intent.getStringExtra("DOC_FOLDER") ?: ""
        val filename = intent.getStringExtra("FILENAME") ?: intent.getStringExtra("DOC_FILENAME") ?: ""

        findViewById<TextView>(R.id.tv_toolbar_title).text = currentTitle
        findViewById<TextView>(R.id.tv_article_title).text = currentTitle
        val tvContent = findViewById<TextView>(R.id.tv_article_body)

        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }

        val mockedContent = intent.getStringExtra("MOCKED_CONTENT")
        val articleContent = intent.getStringExtra("ARTICLE_CONTENT")
        isOrganized = intent.getBooleanExtra("IS_ORGANIZED", false)

        when {
            !articleContent.isNullOrEmpty() -> {
                currentContent = articleContent
                tvContent.text = articleContent
                if (isOrganized) {
                    val organizedLabel = "🤖 Organized: $currentTitle"
                    findViewById<TextView>(R.id.tv_toolbar_title).text = organizedLabel
                    currentTitle = intent.getStringExtra("ARTICLE_TITLE") ?: intent.getStringExtra("DOC_TITLE") ?: "Document"
                }
            }
            !mockedContent.isNullOrEmpty() -> {
                currentContent = mockedContent
                tvContent.text = mockedContent
            }
            folder.isNotEmpty() && filename.isNotEmpty() -> {
                tvContent.text = "Loading content..."
                RetrofitClient.instance.getDocumentContent(folder, filename, true).enqueue(object : Callback<ContentResponse> {
                    override fun onResponse(call: Call<ContentResponse>, response: Response<ContentResponse>) {
                        if (response.isSuccessful && response.body()?.status == "success") {
                            currentContent = response.body()?.content ?: ""
                            tvContent.text = currentContent.ifEmpty { "No content available." }
                        } else {
                            tvContent.text = "Error loading content: ${response.body()?.message ?: "Unknown"}"
                        }
                    }
                    override fun onFailure(call: Call<ContentResponse>, t: Throwable) {
                        tvContent.text = "Network Error: ${t.message}"
                        Toast.makeText(this@ArticleViewActivity, "Failed to connect to backend", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            else -> {
                tvContent.text = "No content available."
            }
        }

        // Real Download Button
        findViewById<ImageView>(R.id.iv_download).setOnClickListener {
            if (currentDocId != null) {
                downloadPdfFromBackend()
            } else {
                downloadContentToText()
                Toast.makeText(this, "Downloading text version (No PDF ID found)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadPdfFromBackend() {
        val docTitle = currentTitle
        val folder = intent.getStringExtra("FOLDER") ?: intent.getStringExtra("DOC_FOLDER") ?: "pdf"
        val filename = intent.getStringExtra("FILENAME") ?: intent.getStringExtra("DOC_FILENAME") ?: "${docTitle.replace(" ", "_")}.pdf"
        
        val extension = filename.substringAfterLast(".", "pdf")

        try {
            val downloadManager = getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            val downloadUrl = "${RetrofitClient.BASE_URL}content/$folder/$filename"
            
            val request = android.app.DownloadManager.Request(android.net.Uri.parse(downloadUrl))
                .setTitle("Downloading $docTitle")
                .setDescription("GOI Retrieval Official Document")
                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "${docTitle.replace(" ", "_")}.$extension")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            downloadManager.enqueue(request)
            Toast.makeText(this, "PDF Download started...", Toast.LENGTH_SHORT).show()
            ActivityLogger.log(this, "Download", "Downloaded PDF: $docTitle", currentDocId)
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
            // Fallback to text if PDF fails
            downloadContentToText()
        }
    }

    private fun downloadContentToText() {
        val safeTitle = currentTitle
            .replace("[🤖 AI Organized: ]".toRegex(), "")
            .replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
            .take(40)
        val fileName = "GOI_${safeTitle}_${System.currentTimeMillis()}.txt"

        val finalContent = if (isOrganized) {
            """
            ==========================================
            GOI RETRIEVAL - AI ORGANIZED REPORT
            ==========================================
            TITLE: $currentTitle
            DATE: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
            CATEGORY: $currentDocType
            ------------------------------------------
            
            $currentContent
            
            ==========================================
            End of Organized Report
            ==========================================
            """.trimIndent()
        } else {
            currentContent
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ use MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(finalContent.toByteArray())
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    Toast.makeText(this, "✅ Saved to Downloads/$fileName", Toast.LENGTH_LONG).show()
                    ActivityLogger.log(this, "Download", "Downloaded: $fileName")
                } else {
                    Toast.makeText(this, "❌ Could not create file. Try again.", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Android 9 and below - use classic file path
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { fos ->
                    fos.write(finalContent.toByteArray())
                }
                Toast.makeText(this, "✅ Saved to Downloads/$fileName", Toast.LENGTH_LONG).show()
                ActivityLogger.log(this, "Download", "Downloaded: $fileName")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "❌ Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
