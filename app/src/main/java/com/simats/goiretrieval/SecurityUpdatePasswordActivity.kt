package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.goiretrieval.api.ResetRequest
import com.simats.goiretrieval.api.RetrofitClient
import com.simats.goiretrieval.api.SignupResponse
import com.simats.goiretrieval.utils.ValidationUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SecurityUpdatePasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_update_password)

        val email = intent.getStringExtra("email") ?: ""
        val ivBack = findViewById<ImageView>(R.id.iv_back)
        val etNewPassword = findViewById<EditText>(R.id.et_new_password)
        val etConfirmPassword = findViewById<EditText>(R.id.et_confirm_password)
        val btnUpdate = findViewById<MaterialButton>(R.id.btn_update_password)

        ivBack.setOnClickListener {
            finish()
        }

        btnUpdate.setOnClickListener {
            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please enter both password fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!ValidationUtils.isValidPassword(newPassword)) {
                AlertDialog.Builder(this)
                    .setTitle("Weak Password")
                    .setMessage(ValidationUtils.getPasswordErrorMessage())
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            btnUpdate.isEnabled = false
            btnUpdate.text = "Updating..."

            val request = ResetRequest(email, newPassword)
            RetrofitClient.instance.resetPassword(request).enqueue(object : Callback<SignupResponse> {
                override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                    btnUpdate.isEnabled = true
                    btnUpdate.text = "Update Password"

                    val apiResponse = response.body()
                    if (response.isSuccessful && apiResponse != null) {
                        if (apiResponse.status == "success") {
                            AlertDialog.Builder(this@SecurityUpdatePasswordActivity)
                                .setTitle("Success")
                                .setMessage("Your password has been updated successfully.")
                                .setCancelable(false)
                                .setPositiveButton("OK") { dialog, _ ->
                                    dialog.dismiss()
                                    val intent = Intent(this@SecurityUpdatePasswordActivity, SecuritySettingsActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    startActivity(intent)
                                    finish()
                                }
                                .show()
                        } else {
                            Toast.makeText(this@SecurityUpdatePasswordActivity, apiResponse.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@SecurityUpdatePasswordActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                    btnUpdate.isEnabled = true
                    btnUpdate.text = "Update Password"
                    Toast.makeText(this@SecurityUpdatePasswordActivity, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}
