package com.example.trnberechnung.model

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

/**
 * Owns the Firebase session. ID tokens deliberately never touch disk; Firebase
 * refreshes and persists the authenticated session through its official SDK.
 */
class AuthRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    val isFirebaseConfigured: Boolean
        get() = FirebaseApp.getApps(appContext).isNotEmpty()

    val configurationError: String?
        get() =
            if (isFirebaseConfigured) {
                null
            } else {
                "Firebase ist nicht konfiguriert. Bitte app/google-services.json für " +
                    "com.example.trnberechnung hinterlegen."
            }

    val isLoggedIn: Boolean
        get() = authOrNull()?.currentUser != null

    var isSkipped: Boolean
        get() = prefs.getBoolean("is_skipped", false)
        set(value) = prefs.edit().putBoolean("is_skipped", value).apply()

    val skipperId: String
        get() = authOrNull()?.currentUser?.uid.orEmpty()

    val userName: String
        get() {
            val user = authOrNull()?.currentUser ?: return ""
            return user.displayName?.takeIf { it.isNotBlank() }
                ?: prefs.getString(displayNameKey(user.uid), null)
                ?: user.email?.substringBefore("@").orEmpty()
        }

    val userEmail: String
        get() = authOrNull()?.currentUser?.email.orEmpty()

    var isDarkMode: Boolean
        get() = prefs.getBoolean("is_dark_mode", false)
        set(value) = prefs.edit().putBoolean("is_dark_mode", value).apply()

    suspend fun signIn(email: String, password: String): Result<Unit> =
        runCatching {
            requireConfiguredAuth()
                .signInWithEmailAndPassword(email.trim(), password)
                .await()
            isSkipped = false
        }

    suspend fun register(name: String, email: String, password: String): Result<Unit> =
        runCatching {
            val auth = requireConfiguredAuth()
            val result =
                auth.createUserWithEmailAndPassword(email.trim(), password)
                    .await()
            val user =
                result.user
                    ?: throw IllegalStateException("Firebase hat keinen Benutzer zurückgegeben.")
            val cleanName = name.trim()
            if (cleanName.isNotEmpty()) {
                user.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(cleanName)
                        .build(),
                ).await()
                prefs.edit().putString(displayNameKey(user.uid), cleanName).apply()
            }
            isSkipped = false
        }

    /**
     * Returns a fresh, verified Firebase ID token for an authenticated request.
     */
    suspend fun getIdToken(forceRefresh: Boolean = false): String {
        val user =
            requireConfiguredAuth().currentUser
                ?: throw AuthenticationRequiredException()
        return user.getIdToken(forceRefresh).await().token
            ?: throw AuthenticationRequiredException("Firebase lieferte kein ID-Token.")
    }

    fun skip() {
        authOrNull()?.signOut()
        isSkipped = true
    }

    fun logout() {
        authOrNull()?.signOut()
        isSkipped = false
    }

    private fun authOrNull(): FirebaseAuth? {
        val firebaseApp = FirebaseApp.getApps(appContext).firstOrNull() ?: return null
        return FirebaseAuth.getInstance(firebaseApp)
    }

    private fun requireConfiguredAuth(): FirebaseAuth =
        authOrNull() ?: throw FirebaseNotConfiguredException(configurationError.orEmpty())

    private fun displayNameKey(uid: String) = "display_name_$uid"
}

class FirebaseNotConfiguredException(
    message: String,
) : IllegalStateException(message)

class AuthenticationRequiredException(
    message: String = "Bitte zuerst mit Firebase anmelden.",
) : IllegalStateException(message)
