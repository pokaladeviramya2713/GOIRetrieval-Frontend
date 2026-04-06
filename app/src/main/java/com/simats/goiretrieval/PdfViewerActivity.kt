package com.simats.goiretrieval

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.simats.goiretrieval.api.RetrofitClient
import java.io.File
import java.net.URLEncoder

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTitle: TextView
    private var pdfUrl: String? = null
    private var pdfTitle: String? = null
    private var pdfId: String? = null
    private var isLocal: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pdf_viewer)

        pdfUrl = intent.getStringExtra("PDF_URL")
        pdfTitle = intent.getStringExtra("PDF_TITLE") ?: "Document"
        pdfId = intent.getStringExtra("PDF_ID")
        isLocal = intent.getBooleanExtra("IS_LOCAL", false)

        initializeViews()
        setupWebView()
        loadPdf()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar_pdf)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }
    }

    private fun initializeViews() {
        webView = findViewById(R.id.web_view_pdf)
        progressBar = findViewById(R.id.pb_pdf_loading)
        tvTitle = findViewById(R.id.tv_pdf_title)
        tvTitle.text = pdfTitle

        findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
        
        findViewById<ImageView>(R.id.btn_share).setOnClickListener { sharePdf() }
        
        findViewById<ImageView>(R.id.btn_download).setOnClickListener { downloadPdf() }
        
        val btnDelete = findViewById<ImageView>(R.id.btn_delete)
        if (pdfId != null) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener { deleteReport() }
        }
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                progressBar.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
            }

            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                progressBar.visibility = View.GONE
                findViewById<TextView>(R.id.tv_pdf_error).visibility = View.VISIBLE
            }
        }
    }

    private fun loadPdf() {
        if (pdfUrl.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid PDF URL", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Use the synchronized server URL for Google Docs Viewer
        val encodedUrl = URLEncoder.encode(pdfUrl, "UTF-8")
        webView.loadUrl("https://docs.google.com/gview?embedded=true&url=$encodedUrl")
    }

    private fun sharePdf() {
        val localPath = intent.getStringExtra("LOCAL_PATH")
        if (localPath == null) {
            Toast.makeText(this, "Local file not found for sharing", Toast.LENGTH_SHORT).show()
            return
        }
        
        val file = File(localPath)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Report via"))
        } else {
            Toast.makeText(this, "Report file missing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadPdf() {
        Toast.makeText(this, "PDF already saved to Reports section.", Toast.LENGTH_SHORT).show()
    }

    private fun deleteReport() {
        if (pdfId == null) return
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Report")
            .setMessage("Are you sure you want to remove this report from the system?")
            .setPositiveButton("Delete") { _, _ ->
                // Call backend delete
                Toast.makeText(this, "Feature coming soon: Server delete for $pdfId", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
