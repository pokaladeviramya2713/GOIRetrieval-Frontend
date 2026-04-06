package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.goiretrieval.api.RetrofitClient
import com.simats.goiretrieval.api.SignupResponse
import com.simats.goiretrieval.api.Verify2FARequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TwoFactorVerifyActivity : AppCompatActivity() {

    private lateinit var etOtp1: EditText
    private lateinit var etOtp2: EditText
    private lateinit var etOtp3: EditText
    private lateinit var etOtp4: EditText
    private lateinit var etOtp5: EditText
    private lateinit var etOtp6: EditText
    private lateinit var btnVerify: MaterialButton
    private lateinit var ivBack: ImageView
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_two_factor_verify)

        email = intent.getStringExtra("email")

        initViews()
        setupOtpEntry()
        
        if (intent.getBooleanExtra("is_from_settings", false)) {
            btnVerify.text = "VERIFY & ENABLE"
        }

        ivBack.setOnClickListener { finish() }
        
        btnVerify.setOnClickListener {
            verifyOtp()
        }
    }

    private fun initViews() {
        etOtp1 = findViewById(R.id.et_otp_1)
        etOtp2 = findViewById(R.id.et_otp_2)
        etOtp3 = findViewById(R.id.et_otp_3)
        etOtp4 = findViewById(R.id.et_otp_4)
        etOtp5 = findViewById(R.id.et_otp_5)
        etOtp6 = findViewById(R.id.et_otp_6)
        btnVerify = findViewById(R.id.btn_verify)
        ivBack = findViewById(R.id.iv_back)
    }

    private fun setupOtpEntry() {
        val editTexts = arrayOf(etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6)
        for (i in 0 until editTexts.size) {
            editTexts[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && i < editTexts.size - 1) {
                        editTexts[i + 1].requestFocus()
                    }
                }
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 0 && i > 0) {
                        editTexts[i - 1].requestFocus()
                    }
                }
            })
        }
    }

    private fun verifyOtp() {
        val otp = etOtp1.text.toString() + etOtp2.text.toString() + etOtp3.text.toString() +
                  etOtp4.text.toString() + etOtp5.text.toString() + etOtp6.text.toString()

        if (otp.length < 6) {
            Toast.makeText(this, "Please enter complete 6-digit OTP", Toast.LENGTH_SHORT).show()
            return
        }

        val requestEmail = email ?: return
        btnVerify.isEnabled = false
        btnVerify.text = "VERIFYING..."

        val request = Verify2FARequest(requestEmail, otp)
        RetrofitClient.instance.verify2FA(request).enqueue(object : Callback<SignupResponse> {
            override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                btnVerify.isEnabled = true
                btnVerify.text = "SECURE LOGIN"

                val apiResponse = response.body()
                if (response.isSuccessful && apiResponse != null && apiResponse.status == "success") {
                    val userId = apiResponse.user?.id ?: -1
                    val name = apiResponse.user?.name
                    val userEmail = apiResponse.user?.email
                    val empId = apiResponse.user?.employeeId
                    val role = apiResponse.user?.role
                    
                    val isFromSettings = intent.getBooleanExtra("is_from_settings", false)
                    
                    if (isFromSettings) {
                        SessionManager.getInstance(this@TwoFactorVerifyActivity).set2FAEnabled(true)
                        Toast.makeText(this@TwoFactorVerifyActivity, "Verification Successful", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        // Save Session (Login flow)
                        val is2faEnabled = apiResponse.user?.is2faEnabled ?: true // If we are here, it was enabled
                        SessionManager.getInstance(this@TwoFactorVerifyActivity)
                            .saveUserSession(userId, "token_2fa_success", name, userEmail, empId, role)
                        SessionManager.getInstance(this@TwoFactorVerifyActivity).set2FAEnabled(is2faEnabled)

                        Toast.makeText(this@TwoFactorVerifyActivity, "Identity Verified", Toast.LENGTH_SHORT).show()
                        
                        val intent = Intent(this@TwoFactorVerifyActivity, SubscriptionActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Toast.makeText(this@TwoFactorVerifyActivity, apiResponse?.message ?: "Verification failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                btnVerify.isEnabled = true
                btnVerify.text = "SECURE LOGIN"
                Toast.makeText(this@TwoFactorVerifyActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
