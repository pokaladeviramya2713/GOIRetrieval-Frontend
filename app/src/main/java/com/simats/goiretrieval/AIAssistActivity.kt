package com.simats.goiretrieval

import android.content.Intent
import com.simats.goiretrieval.BrowseDocsActivity
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import android.provider.OpenableColumns
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts

class AIAssistActivity : AppCompatActivity() {
    
    private val selectedFileUris = mutableListOf<Uri>()
    private var currentMode: String? = null // Require explicit selection

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            selectedFileUris.clear()
            
            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    selectedFileUris.add(clipData.getItemAt(i).uri)
                }
            } ?: data?.data?.let { uri ->
                selectedFileUris.add(uri)
            }

            val tvSelected = findViewById<TextView>(R.id.tv_selected_file)
            tvSelected.text = "${selectedFileUris.size} files selected"
            Toast.makeText(this, "${selectedFileUris.size} files ready for audit", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_assist)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        setupListeners()
        setupBottomNavigation()
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }
        
        findViewById<ImageView>(R.id.iv_notifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.ll_upload_area).setOnClickListener {
            openFilePicker()
        }
        
        setupModeSelectors()
        setupAuditActions()
    }

    private fun setupModeSelectors() {
        val cardScenario = findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_mode_scenario)
        val cardConflict = findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_mode_conflict)
        val cardGap = findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_mode_gap)

        val cards = listOf(cardScenario, cardConflict, cardGap)
        val modes = listOf("SCENARIO", "CONFLICT", "GAP")

        // Initialize UI with no selection
        updateModeUI(cards, -1)

        cards.forEachIndexed { index, card ->
            card.setOnClickListener {
                currentMode = modes[index]
                updateModeUI(cards, index)
            }
        }
    }

    private fun updateModeUI(cards: List<com.google.android.material.card.MaterialCardView>, selectedIndex: Int) {
        cards.forEachIndexed { index, card ->
            val isSelected = index == selectedIndex
            card.strokeWidth = if (isSelected) 6 else 2
            card.strokeColor = if (isSelected) Color.parseColor("#1A237E") else Color.WHITE
            card.cardElevation = if (isSelected) 8f else 2f
            
            // Safer way: search by ID inside the card
            val icon = card.findViewById<ImageView>(R.id.iv_mode_icon)
            val text = card.findViewById<TextView>(R.id.tv_mode_name)
            
            icon?.setColorFilter(if (isSelected) Color.parseColor("#21336a") else Color.parseColor("#9e9e9e"))
            text?.setTextColor(if (isSelected) Color.parseColor("#21336a") else Color.parseColor("#9e9e9e"))
        }

        // Dynamically hide/show the question box based on mode
        val llQuestionsContainer = findViewById<LinearLayout>(R.id.ll_questions_container)
        if (currentMode != null) {
            llQuestionsContainer.visibility = View.VISIBLE
        } else {
            llQuestionsContainer.visibility = View.GONE
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            val mimeTypes = arrayOf("application/pdf", "application/zip", "application/x-zip-compressed")
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
        filePickerLauncher.launch(intent)
    }

    private fun setupAuditActions() {
        findViewById<MaterialButton>(R.id.btn_ai_analyze).setOnClickListener {
            // 1. Check Mode Selection
            if (currentMode == null) {
                Toast.makeText(this, "Please select an audit mode first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Check File Selection
            if (selectedFileUris.isEmpty()) {
                Toast.makeText(this, "Please select at least one document or ZIP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Question Validation
            val etKeywords = findViewById<android.widget.EditText>(R.id.et_keywords)
            val questions = etKeywords.text.toString().trim()
            
            if (questions.isEmpty()) {
                etKeywords.error = "Question is required"
                Toast.makeText(this, "Please type a question for the audit", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (questions.length < 10) {
                etKeywords.error = "Please provide more details (min 10 chars)"
                return@setOnClickListener
            }

            // 4. Contextual Validation (Mode-specific keywords)
            val lowerQ = questions.lowercase()
            var isValid = true
            var errorMsg = ""

            when (currentMode) {
                "GAP" -> {
                    if (!lowerQ.contains("missing") && !lowerQ.contains("gap") && !lowerQ.contains("not found") && !lowerQ.contains("incomplete")) {
                        isValid = false
                        errorMsg = "Gap Analysis questions should ask about what's missing or incomplete."
                    }
                }
                "CONFLICT" -> {
                    if (!lowerQ.contains("conflict") && !lowerQ.contains("contradict") && !lowerQ.contains("versus") && !lowerQ.contains("compare")) {
                        isValid = false
                        errorMsg = "Conflict Analysis should compare items or find contradictions."
                    }
                }
                "SCENARIO" -> {
                    if (!lowerQ.contains("if") && !lowerQ.contains("when") && !lowerQ.contains("what") && !lowerQ.contains("scenario")) {
                        isValid = false
                        errorMsg = "Scenario Audit should describe a situation (e.g., 'What happens if...')"
                    }
                }
            }

            if (!isValid) {
                etKeywords.error = "Suggestion: $errorMsg"
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val sourceUrl = findViewById<android.widget.EditText>(R.id.et_source_url).text.toString()

            ActivityLogger.log(this, "AI Audit", "Started $currentMode audit for ${selectedFileUris.size} files")
            
            // Navigate to Results page
            val intent = Intent(this, AIAuditResultsActivity::class.java).apply {
                putParcelableArrayListExtra("FILE_URIS", ArrayList(selectedFileUris))
                putExtra("SOURCE_URL", sourceUrl)
                putExtra("QUESTIONS", questions)
                putExtra("AUDIT_MODE", currentMode)
            }
            startActivity(intent)
        }
        
        findViewById<MaterialButton>(R.id.btn_ask_ai).setOnClickListener {
            startActivity(Intent(this, com.simats.goiretrieval.AIChatbotActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_search).setOnClickListener {
            startActivity(Intent(this, SearchDocsActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_docs).setOnClickListener {
            startActivity(Intent(this, BrowseDocsActivity::class.java))
            finish()
        }
        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("SELECT_FRAGMENT", "profile")
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
        // AI Assist is the active tab, no navigation action needed for itself
    }

}
