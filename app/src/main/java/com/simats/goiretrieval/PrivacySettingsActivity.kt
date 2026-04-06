package com.simats.goiretrieval

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PrivacySettingsActivity : AppCompatActivity() {

    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_privacy_settings)
        
        session = SessionManager.getInstance(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.header)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        setupNavigation()
    }

    private fun setupNavigation() {
        // Back Button
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Download Data
        findViewById<View>(R.id.btn_download_data).setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val progressBar = android.widget.ProgressBar(this)
            progressBar.setPadding(40, 40, 40, 40)
            builder.setView(progressBar)
            builder.setTitle("Preparing Data")
            builder.setMessage("Gathering your account information...")
            builder.setCancelable(false)
            val dialog = builder.create()
            dialog.show()

            Handler(Looper.getMainLooper()).postDelayed({
                dialog.dismiss()
                val success = saveUserDataToFile()
                if (success) {
                    AlertDialog.Builder(this)
                        .setTitle("Download Complete")
                        .setMessage("Your personal data has been saved to your Downloads folder as 'GOI_Retrieval_Data.txt'. You can open it with any text viewer.")
                        .setPositiveButton("OK") { alertDialog, _ -> alertDialog.dismiss() }
                        .show()
                } else {
                    android.widget.Toast.makeText(this, "Failed to save file", android.widget.Toast.LENGTH_SHORT).show()
                }
            }, 1500)
        }

        // Delete Account
        findViewById<View>(R.id.btn_delete_account).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to permanently delete your account? All your saved documents and history will be lost forever. This action cannot be undone.")
                .setPositiveButton("Delete Forever") { _, _ ->
                    val userId = session.getUserId()
                    com.simats.goiretrieval.api.RetrofitClient.instance.deleteAccount(userId).enqueue(object : retrofit2.Callback<com.simats.goiretrieval.api.SignupResponse> {
                        override fun onResponse(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, response: retrofit2.Response<com.simats.goiretrieval.api.SignupResponse>) {
                            if (response.isSuccessful) {
                                session.logout()
                                android.widget.Toast.makeText(this@PrivacySettingsActivity, "Account Deleted", android.widget.Toast.LENGTH_LONG).show()
                                val intent = Intent(this@PrivacySettingsActivity, RegisterActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            } else {
                                android.widget.Toast.makeText(this@PrivacySettingsActivity, "Failed to delete account", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, t: Throwable) {
                            android.widget.Toast.makeText(this@PrivacySettingsActivity, "Network error", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    })
                }
                .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                .show()
        }


        // Bottom Navigation
        findViewById<View>(R.id.nav_home).setOnClickListener {
            navigateHome()
        }
        
        findViewById<View>(R.id.nav_search).setOnClickListener {
            startActivity(Intent(this, SearchDocsActivity::class.java))
            finish()
        }
        
        findViewById<View>(R.id.nav_docs).setOnClickListener {
            startActivity(Intent(this, BrowseDocsActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.nav_ai_assist).setOnClickListener {
            startActivity(Intent(this, AIAssistActivity::class.java))
            finish()
        }

        findViewById<View>(R.id.nav_profile).setOnClickListener {
            finish()
        }
    }

    private fun navigateHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun saveUserDataToFile(): Boolean {
        val data = """
            GOI RETRIEVAL - PERSONAL DATA EXPORT
            -----------------------------------
            Export Date: ${java.util.Date()}
            
            Account Information:
            Name: ${session.getUserName() ?: "N/A"}
            Email: ${session.getUserEmail() ?: "N/A"}
            Employee ID: ${session.getEmployeeId() ?: "N/A"}
            Role: ${session.getRole() ?: "Analyst"}
            
            Privacy Settings:
            2FA Enabled: ${if (session.is2FAEnabled()) "Yes" else "No"}
            
            Preferences:
            Theme: ${session.getThemeMode()}
            Show Stats: ${session.shouldShowStats()}
            Show Activity: ${session.shouldShowActivity()}
            
            -----------------------------------
            This document contains your personal data as stored on the GOI Retrieval System.
            End of Data Export
        """.trimIndent()

        return try {
            val resolver = contentResolver
            val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Downloads.EXTERNAL_CONTENT_URI else MediaStore.Files.getContentUri("external")
            
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "GOI_Retrieval_Data_${System.currentTimeMillis()}.txt")
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1) // File is pending while writing
                }
            }

            val uri = resolver.insert(contentUri, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(data.toByteArray())
                    outputStream.flush()
                }
                
                // Finalize the file so it's visible to other apps
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                
                // Broad cast for media scan (legacy but still helpful in some scenarios)
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = uri
                sendBroadcast(intent)
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}


