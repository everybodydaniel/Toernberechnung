package com.example.trnberechnung.messaging

import android.content.Context

class InstallationIdStore(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            "crewspace_push",
            Context.MODE_PRIVATE,
        )

    val installationId: String?
        get() = preferences.getString(KEY_INSTALLATION_ID, null)

    val registeredOwnerId: String?
        get() = preferences.getString(KEY_REGISTERED_OWNER_ID, null)

    val registeredInstallationId: String?
        get() = preferences.getString(KEY_REGISTERED_INSTALLATION_ID, null)

    val requestedOwnerId: String?
        get() = preferences.getString(KEY_REQUESTED_OWNER_ID, null)

    fun beginRegistration(ownerId: String) {
        synchronized(STORE_LOCK) {
            val editor = preferences.edit().putString(KEY_REQUESTED_OWNER_ID, ownerId)
            if (
                !shouldKeepConfirmedPushRegistration(
                    requestedOwnerId = ownerId,
                    currentInstallationId = installationId,
                    registeredOwnerId = registeredOwnerId,
                    registeredInstallationId = registeredInstallationId,
                )
            ) {
                editor
                    .remove(KEY_REGISTERED_OWNER_ID)
                    .remove(KEY_REGISTERED_INSTALLATION_ID)
            }
            editor.apply()
        }
    }

    fun recordInstallation(installationId: String): String? {
        return synchronized(STORE_LOCK) {
            val previous = this.installationId
            val editor = preferences.edit().putString(KEY_INSTALLATION_ID, installationId)
            if (previous != installationId) {
                editor
                    .remove(KEY_REGISTERED_OWNER_ID)
                    .remove(KEY_REGISTERED_INSTALLATION_ID)
            }
            editor.apply()
            previous
        }
    }

    fun markRegistered(
        ownerId: String,
        installationId: String,
    ): Boolean {
        return synchronized(STORE_LOCK) {
            if (
                requestedOwnerId != ownerId ||
                this.installationId != installationId
            ) {
                return@synchronized false
            }
            preferences.edit()
                .putString(KEY_REGISTERED_OWNER_ID, ownerId)
                .putString(KEY_REGISTERED_INSTALLATION_ID, installationId)
                .apply()
            true
        }
    }

    fun isRegistrationRequestedFor(ownerId: String): Boolean =
        synchronized(STORE_LOCK) {
            ownerId.isNotBlank() && requestedOwnerId == ownerId
        }

    fun isRegisteredFor(ownerId: String): Boolean =
        synchronized(STORE_LOCK) {
            isCurrentPushRegistration(
                isLoggedIn = ownerId.isNotBlank(),
                currentOwnerId = ownerId,
                currentInstallationId = installationId,
                registeredOwnerId = registeredOwnerId,
                registeredInstallationId = registeredInstallationId,
            )
        }

    fun runIfRegisteredFor(
        ownerId: String,
        action: () -> Unit,
    ): Boolean =
        synchronized(STORE_LOCK) {
            if (
                !isCurrentPushRegistration(
                    isLoggedIn = ownerId.isNotBlank(),
                    currentOwnerId = ownerId,
                    currentInstallationId = installationId,
                    registeredOwnerId = registeredOwnerId,
                    registeredInstallationId = registeredInstallationId,
                )
            ) {
                return@synchronized false
            }
            action()
            true
        }

    fun clearInstallation(installationId: String) {
        synchronized(STORE_LOCK) {
            if (this.installationId != installationId) return
            preferences.edit()
                .remove(KEY_INSTALLATION_ID)
                .remove(KEY_REGISTERED_OWNER_ID)
                .remove(KEY_REGISTERED_INSTALLATION_ID)
                .apply()
        }
    }

    fun clearAll() {
        synchronized(STORE_LOCK) {
            preferences.edit()
                .remove(KEY_INSTALLATION_ID)
                .remove(KEY_REGISTERED_OWNER_ID)
                .remove(KEY_REGISTERED_INSTALLATION_ID)
                .remove(KEY_REQUESTED_OWNER_ID)
                .apply()
        }
    }

    private companion object {
        val STORE_LOCK = Any()
        const val KEY_INSTALLATION_ID = "firebase_installation_id"
        const val KEY_REGISTERED_OWNER_ID = "registered_owner_id"
        const val KEY_REGISTERED_INSTALLATION_ID = "registered_installation_id"
        const val KEY_REQUESTED_OWNER_ID = "requested_owner_id"
    }
}
