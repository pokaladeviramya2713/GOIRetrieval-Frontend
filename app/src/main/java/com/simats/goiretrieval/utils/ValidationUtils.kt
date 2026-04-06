package com.simats.goiretrieval.utils

import android.util.Patterns

object ValidationUtils {

    /**
     * Email validation rule:
     * - Standard email format
     * - Must end with @gmail.com or any work mail ending in .com
     */
    fun isValidEmail(email: String): Boolean {
        if (email.isEmpty()) return false
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Password validation rule:
     * - Length: 8 to 16 characters
     * - At least 1 uppercase letter
     * - At least 1 special character
     */
    fun isValidPassword(password: String): Boolean {
        if (password.length !in 8..16) return false
        val hasUppercase = password.any { it.isUpperCase() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        return hasUppercase && hasSpecial
    }

    fun isValidName(name: String): Boolean {
        // Allow letters, emojis/symbols, and spaces; strictly no numbers
        return name.isNotEmpty() && !name.any { it.isDigit() }
    }

    fun isValidEmpId(empId: String): Boolean {
        // Enforce format EMP-XXXX-XXXX where X is a digit
        val regex = Regex("^EMP-\\d{4}-\\d{4}$")
        return regex.matches(empId)
    }

    fun getEmailErrorMessage(): String {
        return "Invalid email. Please use a valid address (e.g., example@domain.com)."
    }

    fun getPasswordErrorMessage(): String {
        return "Password must be 8-16 characters long and contain at least one uppercase letter and one special character."
    }

    fun getNameErrorMessage(): String {
        return "Name should only contain letters and symbols (no numbers)."
    }

    fun getEmpIdErrorMessage(): String {
        return "Employee ID must follow the format 'EMP-XXXX-XXXX' (e.g., EMP-1234-5678)."
    }
}
