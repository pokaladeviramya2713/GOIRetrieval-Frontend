package com.simats.goiretrieval

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.simats.goiretrieval.api.RetrofitClient
import com.simats.goiretrieval.api.SignupRequest
import com.simats.goiretrieval.api.SignupResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var empIdEditText: EditText
    private lateinit var registerButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        nameEditText = findViewById(R.id.et_name)
        emailEditText = findViewById(R.id.et_email)
        passwordEditText = findViewById(R.id.et_password)
        confirmPasswordEditText = findViewById(R.id.et_confirm_password)
        empIdEditText = findViewById(R.id.et_emp_id)
        registerButton = findViewById(R.id.btn_register)

        setupPasswordToggles()

        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        registerButton.setOnClickListener {
            performRegistration()
        }
    }

    private fun setupPasswordToggles() {
        val togglePassword = findViewById<ImageView>(R.id.iv_toggle_password)
        val toggleConfirmPassword = findViewById<ImageView>(R.id.iv_toggle_confirm_password)

        togglePassword.setOnClickListener {
            toggleVisibility(passwordEditText, togglePassword)
        }

        toggleConfirmPassword.setOnClickListener {
            toggleVisibility(confirmPasswordEditText, toggleConfirmPassword)
        }
    }

    private fun toggleVisibility(editText: EditText, imageView: ImageView) {
        if (editText.transformationMethod is android.text.method.PasswordTransformationMethod) {
            editText.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
            imageView.setImageResource(R.drawable.ic_eye_outline) // Or ic_eye_off if available
            imageView.alpha = 1.0f
        } else {
            editText.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            imageView.setImageResource(R.drawable.ic_eye_outline)
            imageView.alpha = 0.5f
        }
        editText.setSelection(editText.text.length)
    }

    private fun performRegistration() {
        val name = nameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()
        val empId = empIdEditText.text.toString().trim()
        val role = findViewById<android.widget.TextView>(R.id.tv_role_value).text.toString().trim()

        // 1. Name Validation
        if (!com.simats.goiretrieval.utils.ValidationUtils.isValidName(name)) {
            nameEditText.error = com.simats.goiretrieval.utils.ValidationUtils.getNameErrorMessage()
            return
        }

        // 2. Email Validation
        if (!com.simats.goiretrieval.utils.ValidationUtils.isValidEmail(email)) {
            emailEditText.error = com.simats.goiretrieval.utils.ValidationUtils.getEmailErrorMessage()
            return
        }

        // 3. Employee ID Validation
        if (!com.simats.goiretrieval.utils.ValidationUtils.isValidEmpId(empId)) {
            empIdEditText.error = com.simats.goiretrieval.utils.ValidationUtils.getEmpIdErrorMessage()
            return
        }

        // 4. Password Validation
        if (!com.simats.goiretrieval.utils.ValidationUtils.isValidPassword(password)) {
            passwordEditText.error = com.simats.goiretrieval.utils.ValidationUtils.getPasswordErrorMessage()
            return
        }

        // 5. Confirm Password Check
        if (password != confirmPassword) {
            confirmPasswordEditText.error = "Passwords do not match"
            return
        }

        registerButton.isEnabled = false
        registerButton.text = "Registering..."

        val request = SignupRequest(name, email, password, empId, role)
        RetrofitClient.instance.signup(request).enqueue(object : Callback<SignupResponse> {
            override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                registerButton.isEnabled = true
                registerButton.text = "Register"

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null) {
                        if (apiResponse.status == "success") {
                            Toast.makeText(this@RegisterActivity, apiResponse.message, Toast.LENGTH_LONG).show()
                            val intent = Intent(this@RegisterActivity, SignInActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@RegisterActivity, apiResponse.message, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@RegisterActivity, "Error: Empty response from server", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorCode = response.code()
                    Toast.makeText(this@RegisterActivity, "Registration failed: HTTP $errorCode", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                registerButton.isEnabled = true
                registerButton.text = "Register"
                // Log detailed error to help debugging
                val errorMessage = t.localizedMessage ?: t.message ?: "Unknown error"
                Toast.makeText(this@RegisterActivity, "Network Error: $errorMessage", Toast.LENGTH_LONG).show()
            }
        })
    }
}
