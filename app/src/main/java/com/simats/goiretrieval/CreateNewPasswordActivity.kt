package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.goiretrieval.api.ResetRequest
import com.simats.goiretrieval.api.RetrofitClient
import com.simats.goiretrieval.api.SignupResponse

class CreateNewPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_new_password)

        val email = intent.getStringExtra("email")

        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        val etNewPassword = findViewById<EditText>(R.id.et_new_password)
        val etConfirmPassword = findViewById<EditText>(R.id.et_confirm_password)
        val btnReset = findViewById<MaterialButton>(R.id.btn_reset_password)

        btnReset.setOnClickListener {
            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Please enter both password fields.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
                return@setOnClickListener
            }

            if (!com.simats.goiretrieval.utils.ValidationUtils.isValidPassword(newPassword)) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage(com.simats.goiretrieval.utils.ValidationUtils.getPasswordErrorMessage())
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Passwords do not match.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
                return@setOnClickListener
            }

            if (email == null) {
                Toast.makeText(this, "Session expired. Please try again.", Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            btnReset.isEnabled = false
            btnReset.text = "Resetting..."

            val request = ResetRequest(email, newPassword)
            RetrofitClient.instance.resetPassword(request).enqueue(object : retrofit2.Callback<SignupResponse> {
                override fun onResponse(call: retrofit2.Call<SignupResponse>, response: retrofit2.Response<SignupResponse>) {
                    btnReset.isEnabled = true
                    btnReset.text = "Reset Password"

                    val apiResponse = response.body()
                    if (response.isSuccessful && apiResponse != null) {
                        if (apiResponse.status == "success") {
                            androidx.appcompat.app.AlertDialog.Builder(this@CreateNewPasswordActivity)
                                .setTitle("Success")
                                .setMessage(apiResponse.message)
                                .setPositiveButton("OK") { dialog, _ ->
                                    dialog.dismiss()
                                    val intent = Intent(this@CreateNewPasswordActivity, SignInActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .show()
                        } else {
                            Toast.makeText(this@CreateNewPasswordActivity, apiResponse.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@CreateNewPasswordActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: retrofit2.Call<SignupResponse>, t: Throwable) {
                    btnReset.isEnabled = true
                    btnReset.text = "Reset Password"
                    Toast.makeText(this@CreateNewPasswordActivity, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}
