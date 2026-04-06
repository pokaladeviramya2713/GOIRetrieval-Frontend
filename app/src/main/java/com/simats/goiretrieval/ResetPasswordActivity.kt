package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class ResetPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        val otpInputs = arrayOf(
            findViewById<android.widget.EditText>(R.id.et_otp_1),
            findViewById<android.widget.EditText>(R.id.et_otp_2),
            findViewById<android.widget.EditText>(R.id.et_otp_3),
            findViewById<android.widget.EditText>(R.id.et_otp_4),
            findViewById<android.widget.EditText>(R.id.et_otp_5),
            findViewById<android.widget.EditText>(R.id.et_otp_6)
        )

        for (i in otpInputs.indices) {
            otpInputs[i].addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && i < otpInputs.size - 1) {
                        otpInputs[i + 1].requestFocus()
                    }
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
            
            otpInputs[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && event.action == android.view.KeyEvent.ACTION_DOWN) {
                    if (otpInputs[i].text.isEmpty() && i > 0) {
                        otpInputs[i - 1].text.clear()
                        otpInputs[i - 1].requestFocus()
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }

        val email = intent.getStringExtra("email") ?: ""
        val btnVerify = findViewById<MaterialButton>(R.id.btn_reset)

        btnVerify.setOnClickListener {
            val otp = otpInputs.joinToString("") { it.text.toString() }
            if (otp.length < 6) {
                android.widget.Toast.makeText(this, "Please enter all 6 digits", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                android.widget.Toast.makeText(this, "Session expired", android.widget.Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            btnVerify.isEnabled = false
            btnVerify.text = "Verifying..."

            val request = com.simats.goiretrieval.api.VerifyOtpRequest(email, otp)
            com.simats.goiretrieval.api.RetrofitClient.instance.verifyOtp(request).enqueue(object : retrofit2.Callback<com.simats.goiretrieval.api.SignupResponse> {
                override fun onResponse(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, response: retrofit2.Response<com.simats.goiretrieval.api.SignupResponse>) {
                    btnVerify.isEnabled = true
                    btnVerify.text = "Reset Password"

                    val apiResponse = response.body()
                    if (response.isSuccessful && apiResponse != null) {
                        if (apiResponse.status == "success") {
                            androidx.appcompat.app.AlertDialog.Builder(this@ResetPasswordActivity)
                                .setTitle("Success")
                                .setMessage(apiResponse.message)
                                .setPositiveButton("OK") { dialog, _ ->
                                    dialog.dismiss()
                                    val intent = Intent(this@ResetPasswordActivity, CreateNewPasswordActivity::class.java)
                                    intent.putExtra("email", email)
                                    startActivity(intent)
                                    finish()
                                }
                                .show()
                        } else {
                            android.widget.Toast.makeText(this@ResetPasswordActivity, apiResponse.message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        android.widget.Toast.makeText(this@ResetPasswordActivity, "Error: ${response.code()}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, t: Throwable) {
                    btnVerify.isEnabled = true
                    btnVerify.text = "Reset Password"
                    android.widget.Toast.makeText(this@ResetPasswordActivity, "Network Error: ${t.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}
