package com.simats.goiretrieval

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class AIAssistFragment : Fragment() {
    
    private val selectedFileUris = mutableListOf<Uri>()
    private var currentMode = "SCENARIO"

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            selectedFileUris.clear()
            
            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    selectedFileUris.add(clipData.getItemAt(i).uri)
                }
            } ?: data?.data?.let { uri ->
                selectedFileUris.add(uri)
            }

            val tvSelected = view?.findViewById<TextView>(R.id.tv_selected_file)
            tvSelected?.text = "${selectedFileUris.size} files selected"
            Toast.makeText(requireContext(), "${selectedFileUris.size} files ready for audit", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ai_assist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners(view)
        setupNotifications(view)
        
        // Initialize default mode UI
        val cardScenario = view.findViewById<MaterialCardView>(R.id.card_mode_scenario)
        val cardConflict = view.findViewById<MaterialCardView>(R.id.card_mode_conflict)
        val cardGap = view.findViewById<MaterialCardView>(R.id.card_mode_gap)
        updateModeUI(listOf(cardScenario, cardConflict, cardGap), 0)
    }

    override fun onResume() {
        super.onResume()
        updateNotificationDot()
    }

    private fun setupNotifications(view: View) {
        val bell = view.findViewById<View>(R.id.rl_notification_bell) ?: return
        val dot = view.findViewById<View>(R.id.view_notification_dot) ?: return

        bell.setOnClickListener {
            dot.visibility = View.GONE
            startActivity(Intent(requireContext(), NotificationsActivity::class.java))
        }
    }

    private fun updateNotificationDot() {
        val view = view ?: return
        val dot = view.findViewById<View>(R.id.view_notification_dot) ?: return
        val userId = SessionManager.getInstance(requireContext()).getUserId()
        
        if (userId == -1) return

        com.simats.goiretrieval.api.RetrofitClient.instance.getNotifications(userId).enqueue(object : retrofit2.Callback<com.simats.goiretrieval.api.NotificationsResponse> {
            override fun onResponse(
                call: retrofit2.Call<com.simats.goiretrieval.api.NotificationsResponse>,
                response: retrofit2.Response<com.simats.goiretrieval.api.NotificationsResponse>
            ) {
                if (response.isSuccessful && isAdded) {
                    val notifications = response.body()?.notifications ?: emptyList()
                    val hasUnread = notifications.any { !it.is_read }
                    dot.visibility = if (hasUnread) View.VISIBLE else View.GONE
                }
            }
            override fun onFailure(call: retrofit2.Call<com.simats.goiretrieval.api.NotificationsResponse>, t: Throwable) {}
        })
    }

    private fun setupListeners(view: View) {
        view.findViewById<LinearLayout>(R.id.ll_upload_area).setOnClickListener {
            openFilePicker()
        }
        
        setupModeSelectors(view)
        setupAuditActions(view)
    }

    private fun setupModeSelectors(view: View) {
        val cardScenario = view.findViewById<MaterialCardView>(R.id.card_mode_scenario)
        val cardConflict = view.findViewById<MaterialCardView>(R.id.card_mode_conflict)
        val cardGap = view.findViewById<MaterialCardView>(R.id.card_mode_gap)

        val cards = listOf(cardScenario, cardConflict, cardGap)
        val modes = listOf("SCENARIO", "CONFLICT", "GAP")

        cards.forEachIndexed { index, card ->
            card.setOnClickListener {
                currentMode = modes[index]
                updateModeUI(cards, index)
            }
        }
    }

    private fun updateModeUI(cards: List<MaterialCardView>, selectedIndex: Int) {
        if (!isAdded) return
        cards.forEachIndexed { index, card ->
            val isSelected = index == selectedIndex
            card.strokeWidth = if (isSelected) 6 else 2
            card.strokeColor = if (isSelected) Color.parseColor("#1A237E") else Color.WHITE
            card.cardElevation = if (isSelected) 8f else 2f
            
            val icon = card.findViewById<ImageView>(R.id.iv_mode_icon)
            val text = card.findViewById<TextView>(R.id.tv_mode_name)
            
            icon?.setColorFilter(if (isSelected) Color.parseColor("#21336a") else Color.parseColor("#9e9e9e"))
            text?.setTextColor(if (isSelected) Color.parseColor("#21336a") else Color.parseColor("#9e9e9e"))
        }

        val llQuestionsContainer = view?.findViewById<LinearLayout>(R.id.ll_questions_container)
        llQuestionsContainer?.visibility = View.VISIBLE
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

    private fun setupAuditActions(view: View) {
        view.findViewById<MaterialButton>(R.id.btn_ai_analyze).setOnClickListener {
            val etKeywords = view.findViewById<EditText>(R.id.et_keywords)
            val questions = etKeywords.text.toString().trim()
            
            // 1. Validate File Selection
            if (selectedFileUris.isEmpty()) {
                Toast.makeText(requireContext(), "Please select at least one document or ZIP to audit", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Validate Questions / Keywords
            if (questions.isEmpty()) {
                val errorMessage = when (currentMode) {
                    "SCENARIO" -> "Please describe the scenario you want to simulate (e.g., 'What happens if budget is cut by 10%?')"
                    "CONFLICT" -> "Please specify which rules or conflicts to detect (e.g., 'Check for overlapping researcher roles')"
                    "GAP" -> "Please define the compliance benchmarks to check (e.g., 'Ensure all 2024 mandatory fields are present')"
                    else -> "Please type a valid question for the AI to analyze"
                }
                etKeywords.error = errorMessage
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                etKeywords.requestFocus()
                return@setOnClickListener
            }

            val sourceUrl = view.findViewById<EditText>(R.id.et_source_url).text.toString()

            ActivityLogger.log(requireContext(), "AI Audit", "Started $currentMode audit for ${selectedFileUris.size} files")
            
            val intent = Intent(requireContext(), AIAuditResultsActivity::class.java).apply {
                putParcelableArrayListExtra("FILE_URIS", ArrayList(selectedFileUris))
                putExtra("SOURCE_URL", sourceUrl)
                putExtra("QUESTIONS", questions)
                putExtra("AUDIT_MODE", currentMode)
            }
            startActivity(intent)
        }
        
        view.findViewById<MaterialButton>(R.id.btn_ask_ai).setOnClickListener {
            startActivity(Intent(requireContext(), AIChatbotActivity::class.java))
        }
    }
}
