package com.example.trnberechnung.messaging

internal data class CrewspacePushEnvelope(
    val conversationId: String,
    val messageId: String,
    val messageType: String,
)

internal fun parseCrewspacePush(data: Map<String, String>): CrewspacePushEnvelope? {
    if (data.keys != CREWSPACE_PUSH_KEYS) return null
    val conversationId = data["conversation_id"]?.takeIf(String::isNotBlank) ?: return null
    val messageId = data["message_id"]?.takeIf(String::isNotBlank) ?: return null
    val messageType = data["message_type"]?.takeIf(String::isNotBlank) ?: return null
    return CrewspacePushEnvelope(
        conversationId = conversationId,
        messageId = messageId,
        messageType = messageType,
    )
}

internal fun isCurrentPushRegistration(
    isLoggedIn: Boolean,
    currentOwnerId: String?,
    currentInstallationId: String?,
    registeredOwnerId: String?,
    registeredInstallationId: String?,
): Boolean =
    isLoggedIn &&
        !currentOwnerId.isNullOrBlank() &&
        !currentInstallationId.isNullOrBlank() &&
        currentOwnerId == registeredOwnerId &&
        currentInstallationId == registeredInstallationId

internal fun shouldKeepConfirmedPushRegistration(
    requestedOwnerId: String,
    currentInstallationId: String?,
    registeredOwnerId: String?,
    registeredInstallationId: String?,
): Boolean =
    requestedOwnerId.isNotBlank() &&
        !currentInstallationId.isNullOrBlank() &&
        requestedOwnerId == registeredOwnerId &&
        currentInstallationId == registeredInstallationId

internal fun pushSenderTitle(
    canonicalSenderName: String?,
    participantName: String?,
): String =
    canonicalSenderName?.takeIf(String::isNotBlank)
        ?: participantName?.takeIf(String::isNotBlank)
        ?: "Crewspace"

internal fun crewspacePushOwnerWorkTag(ownerId: String) = "crewspace-push-owner-$ownerId"

internal fun crewspacePushUniqueWorkName(
    ownerId: String,
    messageId: String,
) = "crewspace-push-$ownerId-$messageId"

private val CREWSPACE_PUSH_KEYS =
    setOf(
        "conversation_id",
        "message_id",
        "message_type",
    )
