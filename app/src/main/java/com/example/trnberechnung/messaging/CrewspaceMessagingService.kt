package com.example.trnberechnung.messaging

import com.example.trnberechnung.TideNodeApplication
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CrewspaceMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Firebase Messaging's current FID callback. The installation ID is
     * persisted because registration can occur before the user signs in.
     */
    override fun onRegistered(installationId: String) {
        val store = InstallationIdStore(this)
        val previousInstallationId = store.recordInstallation(installationId)
        val app = application as TideNodeApplication
        val expectedOwnerId = app.authRepository.skipperId
        if (
            !app.authRepository.isLoggedIn ||
            !store.isRegistrationRequestedFor(expectedOwnerId)
        ) {
            return
        }
        serviceScope.launch {
            if (
                previousInstallationId != null &&
                previousInstallationId != installationId
            ) {
                runCatching {
                    app.chatRepository.unregisterDevice(previousInstallationId)
                }
            }
            runCatching { app.chatRepository.registerDevice(installationId) }
                .onSuccess {
                    if (
                        app.authRepository.isLoggedIn &&
                        app.authRepository.skipperId == expectedOwnerId &&
                        store.isRegistrationRequestedFor(expectedOwnerId)
                    ) {
                        store.markRegistered(expectedOwnerId, installationId)
                    }
                }
        }
    }

    override fun onUnregistered(installationId: String) {
        val store = InstallationIdStore(this)
        val app = application as TideNodeApplication
        store.clearInstallation(installationId)
        val expectedOwnerId = app.authRepository.skipperId
        if (
            app.authRepository.isLoggedIn &&
            store.isRegistrationRequestedFor(expectedOwnerId)
        ) {
            serviceScope.launch {
                runCatching { app.chatRepository.unregisterDevice(installationId) }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Android push is deliberately data-only. A Notification payload would
        // be rendered by the OS in the background and bypass account checks.
        if (message.notification != null) return
        val envelope = parseCrewspacePush(message.data) ?: return
        val app = application as TideNodeApplication
        val ownerId = app.authRepository.skipperId
        if (!app.authRepository.isLoggedIn) return
        if (!InstallationIdStore(this).isRegisteredFor(ownerId)) return
        if (ChatNavigationState.activeConversationId.value == envelope.conversationId) return
        CrewspacePushWorker.enqueue(this, ownerId, envelope)
    }

    override fun onDeletedMessages() {
        val app = application as TideNodeApplication
        if (app.authRepository.isLoggedIn) {
            serviceScope.launch {
                runCatching { app.chatRepository.activate() }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "crewspace_conversation_id"
    }
}
