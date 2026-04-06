package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.simats.goiretrieval.api.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AIChatbotActivity : AppCompatActivity() {

    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var fabSend: FloatingActionButton
    private lateinit var chatAdapter: ChatAdapter
    private val chatMessages = mutableListOf<ChatMessage>()
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_chatbot)

        userId = SessionManager.getInstance(this).getUserId()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        initializeViews()
        setupChat()
        setupNotifications()
        
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        fabSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotificationDot()
    }

    private fun setupNotifications() {
        val bell = findViewById<View>(R.id.rl_notification_bell)
        val dot = findViewById<View>(R.id.view_notification_dot)

        bell.setOnClickListener {
            dot.visibility = View.GONE
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
    }

    private fun updateNotificationDot() {
        val dot = findViewById<View>(R.id.view_notification_dot)
        if (userId == -1) return

        RetrofitClient.instance.getNotifications(userId).enqueue(object : Callback<com.simats.goiretrieval.api.NotificationsResponse> {
            override fun onResponse(
                call: Call<com.simats.goiretrieval.api.NotificationsResponse>,
                response: Response<com.simats.goiretrieval.api.NotificationsResponse>
            ) {
                if (response.isSuccessful && !isFinishing) {
                    val notifications = response.body()?.notifications ?: emptyList()
                    val hasUnread = notifications.any { !it.is_read }
                    dot.visibility = if (hasUnread) View.VISIBLE else View.GONE
                }
            }
            override fun onFailure(call: Call<com.simats.goiretrieval.api.NotificationsResponse>, t: Throwable) {}
        })
    }

    private fun initializeViews() {
        rvChat = findViewById(R.id.rv_chat)
        etMessage = findViewById(R.id.et_message)
        fabSend = findViewById(R.id.fab_send)
    }

    private fun setupChat() {
        chatAdapter = ChatAdapter(chatMessages)
        rvChat.adapter = chatAdapter
        
        // Initial AI message
        addMessage("Hello! I am your GOI AI Assistant. How can I help you with policies or schemes today?", false)
    }

    private fun sendMessage(text: String) {
        if (userId == -1) {
            Toast.makeText(this, "Session error. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        addMessage(text, true)
        etMessage.text.clear()
        
        // Add a "Typing..." placeholder
        val typingPlaceholder = ChatMessage("Typing...", false)
        chatMessages.add(typingPlaceholder)
        chatAdapter.notifyItemInserted(chatMessages.size - 1)
        rvChat.scrollToPosition(chatMessages.size - 1)

        val request = com.simats.goiretrieval.api.ChatBotRequest(userId, text)
        RetrofitClient.instance.chatbot(request).enqueue(object : Callback<com.simats.goiretrieval.api.ChatBotResponse> {
            override fun onResponse(
                call: Call<com.simats.goiretrieval.api.ChatBotResponse>,
                response: Response<com.simats.goiretrieval.api.ChatBotResponse>
            ) {
                // Remove the typing placeholder
                val position = chatMessages.indexOf(typingPlaceholder)
                if (position != -1) {
                    chatMessages.removeAt(position)
                    chatAdapter.notifyItemRemoved(position)
                }

                if (response.isSuccessful && !isFinishing) {
                    val aiMessage = response.body()?.message ?: "I'm sorry, I couldn't process that."
                    addMessage(aiMessage, false)
                } else {
                    addMessage("Sorry, I am having trouble connecting to the Education Cloud. Please try again.", false)
                }
            }

            override fun onFailure(call: Call<com.simats.goiretrieval.api.ChatBotResponse>, t: Throwable) {
                // Remove the typing placeholder
                val position = chatMessages.indexOf(typingPlaceholder)
                if (position != -1) {
                    chatMessages.removeAt(position)
                    chatAdapter.notifyItemRemoved(position)
                }
                addMessage("Network Error: Could not reach the AI server.", false)
            }
        })
    }

    private fun addMessage(text: String, isUser: Boolean) {
        val message = ChatMessage(text, isUser)
        chatAdapter.addMessage(message)
        rvChat.scrollToPosition(chatMessages.size - 1)
    }
}
