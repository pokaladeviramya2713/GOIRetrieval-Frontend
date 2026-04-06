package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.TextView

class PersonalInfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_personal_info)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Header Back Button -> ProfileActivity
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Centered Edit Button -> EditProfileActivity
        findViewById<LinearLayout>(R.id.btn_edit_center).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        setupBottomNavigation()
        loadPersonalInfo()
    }

    private fun setupBottomNavigation() {
        val navItems = mapOf(
            R.id.nav_home to "home",
            R.id.nav_search to "search",
            R.id.nav_docs to "docs",
            R.id.nav_ai_assist to "ai_assist",
            R.id.nav_profile to "profile"
        )

        navItems.forEach { (id, fragmentTag) ->
            findViewById<LinearLayout>(id).setOnClickListener {
                if (fragmentTag == "profile") {
                    // Already on profile related screen, just finish or do nothing
                    finish()
                } else {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("SELECT_FRAGMENT", fragmentTag)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadPersonalInfo()
    }

    private fun loadPersonalInfo() {
        val session = SessionManager.getInstance(this)
        val name = session.getUserName() ?: "User"
        val email = session.getUserEmail() ?: "Not Available"
        val empId = session.getEmployeeId() ?: "Not Set"
        val role = session.getRole() ?: "Analyst"

        findViewById<TextView>(R.id.tv_name_summary).text = name
        findViewById<TextView>(R.id.tv_name_detail).text = name
        findViewById<TextView>(R.id.tv_email_detail).text = email
        findViewById<TextView>(R.id.tv_emp_id_detail).text = empId
        findViewById<TextView>(R.id.tv_role_detail).text = role
        findViewById<TextView>(R.id.tv_avatar_initial).text = if (name.isNotEmpty()) name[0].uppercaseChar().toString() else "U"
    }
}
