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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SecurityChangePasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_change_password)

        val etEmail = findViewById<EditText>(R.id.et_email)
        val btnSendOtp = findViewById<MaterialButton>(R.id.btn_send_otp)
        val ivBack = findViewById<ImageView>(R.id.iv_back)

        // Pre-fill email from session
        val session = SessionManager.getInstance(this)
        etEmail.setText(session.getUserEmail())

        ivBack.setOnClickListener {
            finish()
        }

        btnSendOtp.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSendOtp.isEnabled = false
            btnSendOtp.text = "Sending..."

            val request = ForgotRequest(email)
            RetrofitClient.instance.forgotPassword(request).enqueue(object : Callback<SignupResponse> {
                override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                    btnSendOtp.isEnabled = true
                    btnSendOtp.text = "Send OTP"

                    val apiResponse = response.body()
                    if (response.isSuccessful && apiResponse != null) {
                        if (apiResponse.status == "success") {
                            Toast.makeText(this@SecurityChangePasswordActivity, "OTP sent to your email", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@SecurityChangePasswordActivity, SecurityVerifyOtpActivity::class.java)
                            intent.putExtra("email", email)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@SecurityChangePasswordActivity, apiResponse.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@SecurityChangePasswordActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                    btnSendOtp.isEnabled = true
                    btnSendOtp.text = "Send OTP"
                    Toast.makeText(this@SecurityChangePasswordActivity, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}
