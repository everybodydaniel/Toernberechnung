package com.example.trnberechnung.model

import android.content.Context
import android.content.SharedPreferences

class AuthRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    var isLoggedIn: Boolean
        get() = prefs.getBoolean("is_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_logged_in", value).apply()

    var isSkipped: Boolean
        get() = prefs.getBoolean("is_skipped", false)
        set(value) = prefs.edit().putBoolean("is_skipped", value).apply()

    var userName: String
        get() = prefs.getString("user_name", "") ?: ""
        set(value) = prefs.edit().putString("user_name", value).apply()

    var userEmail: String
        get() = prefs.getString("user_email", "") ?: ""
        set(value) = prefs.edit().putString("user_email", value).apply()

    var skipperId: String
        get() = prefs.getString("skipper_id", "") ?: ""
        set(value) = prefs.edit().putString("skipper_id", value).apply()
        
    var isDarkMode: Boolean
        get() = prefs.getBoolean("is_dark_mode", false)
        set(value) = prefs.edit().putBoolean("is_dark_mode", value).apply()

    var idToken: String
        get() = prefs.getString("id_token", "") ?: ""
        set(value) = prefs.edit().putString("id_token", value).apply()

    fun hasAccount(email: String): Boolean {
        return prefs.contains("account_${email.lowercase()}")
    }

    suspend fun loginWithFirebase(name: String, email: String, pass: String = "DefaultPass123!"): Boolean {
        return try {
            val req = com.example.trnberechnung.network.FirebaseAuthRequest(email, pass)
            var response = com.example.trnberechnung.network.RetrofitInstance.firebaseAuthApi.signInWithPassword(
                com.example.trnberechnung.network.RetrofitInstance.FIREBASE_API_KEY, req
            )
            
            // If sign in fails, try sign up
            if (!response.isSuccessful || response.body()?.idToken == null) {
                response = com.example.trnberechnung.network.RetrofitInstance.firebaseAuthApi.signUp(
                    com.example.trnberechnung.network.RetrofitInstance.FIREBASE_API_KEY, req
                )
            }

            if (response.isSuccessful && response.body()?.idToken != null) {
                val body = response.body()!!
                idToken = body.idToken!!
                skipperId = body.localId ?: generateSkipperId()
                userName = name.ifBlank { email.substringBefore("@") }
                userEmail = email.lowercase()
                isLoggedIn = true
                isSkipped = false
                prefs.edit().putString("account_${userEmail}", "$userName|$skipperId|$idToken").apply()

                // Immediately trigger profile creation/registration on Go server
                try {
                    com.example.trnberechnung.network.RetrofitInstance.socialFeedApi.getConversations("Bearer $idToken")
                } catch (e: Exception) {
                    android.util.Log.e("AUTH", "Failed to register profile on server: ${e.message}")
                }

                true
            } else {
                // Fallback to local mock login if offline / network error
                login(name, email)
                true
            }
        } catch (e: Exception) {
            // Fallback to local mock login if offline / network error
            login(name, email)
            true
        }
    }

    fun login(name: String, email: String) {
        val lowerEmail = email.lowercase()
        val accountData = prefs.getString("account_$lowerEmail", null)
        
        if (accountData != null) {
            val parts = accountData.split("|")
            userName = parts.getOrNull(0) ?: name
            skipperId = parts.getOrNull(1) ?: generateSkipperId()
            idToken = parts.getOrNull(2) ?: ""
            userEmail = lowerEmail
        } else {
            userName = name
            userEmail = lowerEmail
            skipperId = generateSkipperId()
            prefs.edit().putString("account_$lowerEmail", "$userName|$skipperId|$idToken").apply()
        }
        
        isLoggedIn = true
        isSkipped = false
    }

    fun skip() {
        isSkipped = true
        isLoggedIn = false
    }

    fun logout() {
        isLoggedIn = false
        isSkipped = false
        userName = ""
        userEmail = ""
        skipperId = ""
        idToken = ""
    }

    private fun generateSkipperId(): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..16)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
