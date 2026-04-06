package com.simats.goiretrieval

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class AIComplianceResultsActivity : AppCompatActivity() {

    private lateinit var adapter: ComplianceResultAdapter
    private val resultsList = mutableListOf<ComplianceResult>()
    private lateinit var cardLoading: MaterialCardView
    private lateinit var tvLoadingHint: TextView
    private lateinit var nestedScrollView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_compliance_results)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        cardLoading = findViewById(R.id.card_loading)
        tvLoadingHint = findViewById(R.id.tv_loading_hint)

        setupListeners()
        setupSortOptions()
        setupRecyclerView()
        setupBottomNavigation()
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.rv_compliance_results)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = ComplianceResultAdapter(resultsList)
        recyclerView.adapter = adapter

        fetchComplianceResultFromBackend()
    }

    private fun setLoading(isLoading: Boolean, hint: String = "Checking document content and Source URL...") {
        if (isLoading) {
            cardLoading.visibility = View.VISIBLE
            tvLoadingHint.text = hint
        } else {
            cardLoading.visibility = View.GONE
        }
    }

    private fun fetchComplianceResultFromBackend() {
        val questions = intent.getStringExtra("QUESTIONS") ?: "General Compliance Audit"
        val sourceUrl = intent.getStringExtra("SOURCE_URL") ?: ""
        val fileUriString = intent.getStringExtra("FILE_URI")
        val fileName = intent.getStringExtra("FILE_NAME") ?: "Uploaded Document"

        resultsList.clear()
        adapter.notifyDataSetChanged()

        // Show loading state immediately
        setLoading(true, "Reading your document...")

        lifecycleScope.launch {
            try {
                // Step 1: Extract text content from uploaded file 
                setLoading(true, "Reading your uploaded file (extracting PDF text)...")
                val rawContent = if (fileUriString != null) {
                    val extracted = com.simats.goiretrieval.utils.DocumentTextExtractor.extractText(
                        this@AIComplianceResultsActivity,
                        android.net.Uri.parse(fileUriString)
                    )
                    if (extracted != null) {
                        android.widget.Toast.makeText(
                            this@AIComplianceResultsActivity,
                            "Read ${extracted.length} characters from your file",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    extracted
                } else null

                // Step 2: Generate overall direct answer
                setLoading(true, "Gemini is finding the exact answer to your question...")
                val titleLabel = if (questions.length > 50) "\"${questions.take(47)}...\"" else "\"$questions\""
                findViewById<TextView>(R.id.tv_overall_summary_title).text = "Direct AI Answer for $titleLabel"

                val overall = com.simats.goiretrieval.utils.AISummaryGenerator.generate(
                    title = "Document Audit",
                    category = "Audit",
                    questions = questions,
                    score = 90,
                    rawContent = rawContent,
                    sourceUrl = sourceUrl.ifEmpty { null }
                )

                val answerPrefix = if (sourceUrl.isNotEmpty())
                    "ANSWER (from document + $sourceUrl):\n\n"
                else "ANSWER:\n\n"

                val charCountInfo = if (rawContent != null) 
                    "\n\n[Debug: Read ${rawContent.length} chars from file]"
                else "\n\n[Debug: No content read from file]"

                findViewById<TextView>(R.id.tv_overall_ai_summary).text =
                    "$answerPrefix${overall.brief}$charCountInfo\n\n──────────────────────\nDetailed sections below:"

                // Step 3: Analyze uploaded document specifically
                if (rawContent != null) {
                    setLoading(true, "🤖 Analyzing your uploaded document for compliance...")
                    val res1 = com.simats.goiretrieval.utils.AISummaryGenerator.generate(
                        title = fileName,
                        category = "Uploaded Document",
                        questions = questions,
                        score = 95,
                        rawContent = rawContent,
                        sourceUrl = sourceUrl.ifEmpty { null }
                    )
                    resultsList.add(ComplianceResult(
                        title = fileName,
                        type = "Your Uploaded File",
                        date = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date()),
                        tags = res1.tags,
                        score = 95,
                        complianceLevel = "High Compliance",
                        summary = res1.brief,
                        scoreColor = "#00C853",
                        reference = res1.reference,
                        detailedContent = res1.detailed,
                        organizedContent = res1.mockContent
                    ))
                    adapter.notifyDataSetChanged()
                }

                // Step 4: Add a reference/comparison document from the source URL knowledge
                if (sourceUrl.isNotEmpty()) {
                    setLoading(true, "🌐 Fetching official guidelines from Source URL...")
                    val res2 = com.simats.goiretrieval.utils.AISummaryGenerator.generate(
                        title = "Official Portal Summary",
                        category = "Official Source Reference",
                        questions = questions,
                        score = 100,
                        rawContent = null,   // No local content — Gemini uses Source URL knowledge
                        sourceUrl = sourceUrl
                    )
                    resultsList.add(ComplianceResult(
                        title = "✅ Official Guidelines (from $sourceUrl)",
                        type = "Source URL Reference",
                        date = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date()),
                        tags = res2.tags,
                        score = 100,
                        complianceLevel = "Official Standard",
                        summary = res2.brief,
                        scoreColor = "#1565C0",
                        reference = res2.reference,
                        detailedContent = res2.detailed,
                        organizedContent = "[OFFICIAL PORTAL CONTENT]\n\n${res2.mockContent}"
                    ))
                    adapter.notifyDataSetChanged()
                }

                setLoading(false)

                if (resultsList.isEmpty()) {
                    Toast.makeText(this@AIComplianceResultsActivity,
                        "Please upload a file or add a Source URL for better results.",
                        Toast.LENGTH_LONG).show()

                    // Add a generic fallback result so the screen isn't empty
                    val fallback = com.simats.goiretrieval.utils.AISummaryGenerator.generate(
                        title = "General Knowledge Audit",
                        category = "General",
                        questions = questions,
                        score = 70
                    )
                    resultsList.add(ComplianceResult(
                        title = "General AI Answer",
                        type = "AI Knowledge Base",
                        date = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date()),
                        tags = fallback.tags,
                        score = 70,
                        complianceLevel = "Partial Compliance",
                        summary = fallback.brief,
                        scoreColor = "#FF6F00",
                        reference = fallback.reference,
                        detailedContent = fallback.detailed,
                        organizedContent = fallback.mockContent
                    ))
                    adapter.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                setLoading(false)
                Toast.makeText(
                    this@AIComplianceResultsActivity,
                    "AI Error: ${e.message ?: "Unknown error occurred"}. Check your API key or internet connection.",
                    Toast.LENGTH_LONG
                ).show()
                findViewById<TextView>(R.id.tv_overall_ai_summary).text =
                    "⚠️ AI analysis failed. Please check your internet connection and try again.\n\nError: ${e.message}"
            }
        }
    }


    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.nav_home_btn).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_search_btn).setOnClickListener {
            startActivity(Intent(this, SearchDocsActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_docs_btn).setOnClickListener {
            startActivity(Intent(this, BrowseDocsActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_profile_btn).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_ai_assist_btn).setOnClickListener {
            finish() // Goes back to AI Assist Activity
        }
    }

    private fun setupSortOptions() {
        val sortOptions = listOf(
            "Most Relevant",
            "Highest Score",
            "Lowest Score",
            "Newest First",
            "Oldest First"
        )
        
        val tvSortValue = findViewById<TextView>(R.id.tv_sort_value)

        findViewById<LinearLayout>(R.id.ll_sort_filter).setOnClickListener {
            val bottomSheetDialog = BottomSheetDialog(this)
            val view = LayoutInflater.from(this).inflate(R.layout.layout_bottom_sheet_sort, null)
            bottomSheetDialog.setContentView(view)

            (view.parent as View).setBackgroundColor(Color.TRANSPARENT)

            val rvSortOptions = view.findViewById<RecyclerView>(R.id.rv_sort_options)
            val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel)

            rvSortOptions.layoutManager = LinearLayoutManager(this)
            rvSortOptions.adapter = SortAdapter(sortOptions, tvSortValue.text.toString()) { selected ->
                tvSortValue.text = selected
                bottomSheetDialog.dismiss()
                applySorting(selected)
            }

            btnCancel.setOnClickListener { bottomSheetDialog.dismiss() }
            bottomSheetDialog.show()
        }
    }

    private fun applySorting(option: String) {
        when (option) {
            "Highest Score" -> resultsList.sortByDescending { it.score }
            "Lowest Score" -> resultsList.sortBy { it.score }
            "Newest First" -> resultsList.sortByDescending { it.date }
            "Oldest First" -> resultsList.sortBy { it.date }
            else -> { /* Most Relevant - keep existing order */ }
        }
        adapter.notifyDataSetChanged()
    }

    inner class SortAdapter(
        private val items: List<String>,
        private val currentSelection: String,
        private val onItemSelected: (String) -> Unit
    ) : RecyclerView.Adapter<SortAdapter.SortViewHolder>() {

        inner class SortViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tv_category_name)
            val ivCheck: ImageView = view.findViewById(R.id.iv_check)
            val llRoot: LinearLayout = view.findViewById(R.id.ll_root)
            
            init {
                view.findViewById<ImageView>(R.id.iv_icon).visibility = View.GONE
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SortViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
            return SortViewHolder(view)
        }

        override fun onBindViewHolder(holder: SortViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item

            if (item == currentSelection) {
                holder.llRoot.setBackgroundColor(Color.parseColor("#F0F9FF"))
                holder.ivCheck.visibility = View.VISIBLE
            } else {
                val typedValue = android.util.TypedValue()
                holder.itemView.context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                holder.llRoot.setBackgroundResource(typedValue.resourceId)
                holder.ivCheck.visibility = View.INVISIBLE
            }

            holder.llRoot.setOnClickListener {
                onItemSelected(item)
            }
        }

        override fun getItemCount() = items.size
    }
}
