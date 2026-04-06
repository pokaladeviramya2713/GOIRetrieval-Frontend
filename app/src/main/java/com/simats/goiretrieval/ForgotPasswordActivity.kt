package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.goiretrieval.api.ForgotRequest
import com.simats.goiretrieval.api.RetrofitClient
import com.simats.goiretrieval.api.SignupResponse

class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val etEmail = findViewById<EditText>(R.id.et_email)
        val btnReset = findViewById<MaterialButton>(R.id.btn_reset)

        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        btnReset.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnReset.isEnabled = false
            btnReset.text = "Checking..."

            val request = ForgotRequest(email)
            RetrofitClient.instance.forgotPassword(request).enqueue(object : retrofit2.Callback<SignupResponse> {
                override fun onResponse(call: retrofit2.Call<SignupResponse>, response: retrofit2.Response<SignupResponse>) {
                    btnReset.isEnabled = true
                    btnReset.text = "Reset Password"

                    val apiResponse = response.body()
                    if (response.isSuccessful && apiResponse != null) {
                        if (apiResponse.status == "success") {
                            Toast.makeText(this@ForgotPasswordActivity, apiResponse.message, Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java)
                            intent.putExtra("email", email)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@ForgotPasswordActivity, apiResponse.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@ForgotPasswordActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: retrofit2.Call<SignupResponse>, t: Throwable) {
                    btnReset.isEnabled = true
                    btnReset.text = "Reset Password"
                    Toast.makeText(this@ForgotPasswordActivity, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}
