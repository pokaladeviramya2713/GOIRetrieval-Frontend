package com.simats.goiretrieval

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.EditText
import android.widget.Toast
import com.simats.goiretrieval.api.RetrofitClient
import com.simats.goiretrieval.api.SignupResponse
import com.simats.goiretrieval.api.UpdateProfileRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Header Back Button -> PersonalInfoActivity
        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        // Header Save Button -> PersonalInfoActivity
        findViewById<LinearLayout>(R.id.btn_save_header).setOnClickListener {
            saveUserInfo()
        }
        
        // Footer Save Button -> PersonalInfoActivity
        findViewById<LinearLayout>(R.id.btn_save_bottom).setOnClickListener {
            saveUserInfo()
        }

        loadCurrentInfo()
    }

    private fun loadCurrentInfo() {
        val session = SessionManager.getInstance(this)
        findViewById<EditText>(R.id.et_name).setText(session.getUserName())
        // Keep email for safety, but often email is not editable. I'll leave it as editable per user's request.
        findViewById<EditText>(R.id.et_email).setText(session.getUserEmail())
        findViewById<EditText>(R.id.et_emp_id).setText(session.getEmployeeId())
        findViewById<EditText>(R.id.et_role).setText(session.getRole())
    }

    private fun saveUserInfo() {
        val name = findViewById<EditText>(R.id.et_name).text.toString().trim()
        val email = findViewById<EditText>(R.id.et_email).text.toString().trim()
        val empId = findViewById<EditText>(R.id.et_emp_id).text.toString().trim()
        val role = findViewById<EditText>(R.id.et_role).text.toString().trim()

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show()
            return
        }

        val session = SessionManager.getInstance(this)
        val userId = session.getUserId()
        if (userId == -1) return

        val request = UpdateProfileRequest(
            userId = userId,
            name = name,
            email = email,
            employeeId = empId,
            role = role
        )

        RetrofitClient.instance.updateProfile(request).enqueue(object : Callback<SignupResponse> {
            override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.status == "success") {
                        session.updateUserInfo(name, email, empId, role)
                        Toast.makeText(this@EditProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val msg = apiResponse?.message ?: "Unknown server error"
                        Toast.makeText(this@EditProfileActivity, "Failed to update profile: $msg", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorCode = response.code()
                    Toast.makeText(this@EditProfileActivity, "Update failed: HTTP $errorCode", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                Toast.makeText(this@EditProfileActivity, "Network Error: ${t.localizedMessage ?: t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
