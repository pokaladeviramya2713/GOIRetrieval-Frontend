package com.simats.goiretrieval

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class SignInActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signInButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        emailEditText = findViewById(R.id.et_email)
        passwordEditText = findViewById(R.id.et_password)
        signInButton = findViewById(R.id.btn_sign_in)

        findViewById<TextView>(R.id.tv_forgot_password).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        findViewById<TextView>(R.id.tv_sign_up).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        signInButton.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        if (!com.simats.goiretrieval.utils.ValidationUtils.isValidEmail(email)) {
            emailEditText.error = com.simats.goiretrieval.utils.ValidationUtils.getEmailErrorMessage()
            return
        }

        if (!com.simats.goiretrieval.utils.ValidationUtils.isValidPassword(password)) {
            passwordEditText.error = com.simats.goiretrieval.utils.ValidationUtils.getPasswordErrorMessage()
            return
        }

        signInButton.isEnabled = false
        signInButton.text = "Signing In..."

        val deviceName = android.os.Build.MODEL
        val osVersion = "Android ${android.os.Build.VERSION.RELEASE}"

        val request = com.simats.goiretrieval.api.SigninRequest(
            email = email,
            password = password,
            deviceName = deviceName,
            osVersion = osVersion
        )
        com.simats.goiretrieval.api.RetrofitClient.instance.signin(request).enqueue(object : retrofit2.Callback<com.simats.goiretrieval.api.SignupResponse> {
            override fun onResponse(
                call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>,
                response: retrofit2.Response<com.simats.goiretrieval.api.SignupResponse>
            ) {
                signInButton.isEnabled = true
                signInButton.text = "Sign In"

                val apiResponse = response.body()
                if (response.isSuccessful && apiResponse != null) {
                    if (apiResponse.status == "success") {
                        val userId = apiResponse.user?.id ?: -1
                        val name = apiResponse.user?.name
                        val email = apiResponse.user?.email
                        val empId = apiResponse.user?.employeeId
                        val role = apiResponse.user?.role
                        val is2faEnabled = apiResponse.user?.is2faEnabled ?: false
                        
                        SessionManager.getInstance(this@SignInActivity)
                            .saveUserSession(userId, "mock_token_success", name, email, empId, role)
                        SessionManager.getInstance(this@SignInActivity).set2FAEnabled(is2faEnabled)

                        Toast.makeText(this@SignInActivity, apiResponse.message, Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@SignInActivity, SubscriptionActivity::class.java))
                        finish()
                    } else if (apiResponse.status == "2fa_required") {
                        // Navigate to 2FA verification
                        val intent = Intent(this@SignInActivity, TwoFactorVerifyActivity::class.java)
                        intent.putExtra("email", apiResponse.email ?: email)
                        startActivity(intent)
                        Toast.makeText(this@SignInActivity, apiResponse.message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@SignInActivity, apiResponse.message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@SignInActivity, "Login failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: retrofit2.Call<com.simats.goiretrieval.api.SignupResponse>, t: Throwable) {
                signInButton.isEnabled = true
                signInButton.text = "Sign In"
                Toast.makeText(this@SignInActivity, "Network Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}

