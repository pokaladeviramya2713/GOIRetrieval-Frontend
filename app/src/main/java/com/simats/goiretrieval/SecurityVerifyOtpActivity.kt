package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.goiretrieval.api.RetrofitClient
import com.simats.goiretrieval.api.SignupResponse
import com.simats.goiretrieval.api.VerifyOtpRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SecurityVerifyOtpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_verify_otp)

        val ivBack = findViewById<ImageView>(R.id.iv_back)
        val btnVerify = findViewById<MaterialButton>(R.id.btn_verify)
        val email = intent.getStringExtra("email") ?: ""

        ivBack.setOnClickListener {
            finish()
        }

        val otpInputs = arrayOf(
            findViewById<EditText>(R.id.et_otp_1),
            findViewById<EditText>(R.id.et_otp_2),
            findViewById<EditText>(R.id.et_otp_3),
            findViewById<EditText>(R.id.et_otp_4),
            findViewById<EditText>(R.id.et_otp_5),
            findViewById<EditText>(R.id.et_otp_6)
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

        btnVerify.setOnClickListener {
            val otp = otpInputs.joinToString("") { it.text.toString() }
            if (otp.length < 6) {
                Toast.makeText(this, "Please enter all 6 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            btnVerify.isEnabled = false
            btnVerify.text = "Verifying..."

            val request = VerifyOtpRequest(email, otp)
            RetrofitClient.instance.verifyOtp(request).enqueue(object : Callback<SignupResponse> {
                override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                    btnVerify.isEnabled = true
                    btnVerify.text = "Verify OTP"

                    val apiResponse = response.body()
                    if (response.isSuccessful && apiResponse != null) {
                        if (apiResponse.status == "success") {
                            val intent = Intent(this@SecurityVerifyOtpActivity, SecurityUpdatePasswordActivity::class.java)
                            intent.putExtra("email", email)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@SecurityVerifyOtpActivity, apiResponse.message, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@SecurityVerifyOtpActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                    btnVerify.isEnabled = true
                    btnVerify.text = "Verify OTP"
                    Toast.makeText(this@SecurityVerifyOtpActivity, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}
