package com.example.trnberechnung.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.trnberechnung.BuildConfig
import com.example.trnberechnung.database.ChatDao
import com.example.trnberechnung.database.ChatMessageEntity
import com.example.trnberechnung.database.ChatThreadEntity
import com.example.trnberechnung.messaging.ChatOutboxWorker
import com.example.trnberechnung.messaging.ChatNavigationState
import com.example.trnberechnung.messaging.CrewspacePushNotifications
import com.example.trnberechnung.messaging.CrewspacePushWorker
import com.example.trnberechnung.messaging.InstallationIdStore
import com.example.trnberechnung.messaging.pushSenderTitle
import com.example.trnberechnung.model.AuthRepository
import com.example.trnberechnung.model.ChatDeliveryState
import com.example.trnberechnung.model.ChatMessageType
import com.example.trnberechnung.model.ChatThreadType
import com.example.trnberechnung.network.ApiBlocksResponse
import com.example.trnberechnung.network.ApiCreateDirectChatRequest
import com.example.trnberechnung.network.ApiCreateMessageRequest
import com.example.trnberechnung.network.ApiCrewspaceConversation
import com.example.trnberechnung.network.ApiCrewspaceMessage
import com.example.trnberechnung.network.ApiDeviceRegistrationRequest
import com.example.trnberechnung.network.ApiPresignUploadRequest
import com.example.trnberechnung.network.ApiRealtimeEnvelope
import com.example.trnberechnung.network.ApiUpdateMeRequest
import com.example.trnberechnung.network.CrewspaceApiService
import com.example.trnberechnung.network.RetrofitInstance
import com.example.trnberechnung.network.secureHttpsBaseUrl
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

class ChatRepository(
    context: Context,
    private val chatDao: ChatDao,
    private val authRepository: AuthRepository,
    private val api: CrewspaceApiService = RetrofitInstance.crewspaceApi,
    private val httpClient: OkHttpClient = RetrofitInstance.crewspaceHttpClient,
) {
    private val appContext = context.applicationContext
    private val activationMutex = Mutex()
    private val outboxSessionMutex = Mutex()
    private val realtime =
        CrewspaceRealtimeClient(
            authRepository = authRepository,
            httpClient = httpClient,
            onEnvelope = ::handleRealtimeEnvelope,
            onConnected = ::recoverAfterReconnect,
        )

    val connectionState = realtime.state

    fun threads(ownerId: String): Flow<List<ChatThreadEntity>> =
        chatDao.getThreadsForUser(ownerId)

    fun messages(
        ownerId: String,
        conversationId: String,
    ): Flow<List<ChatMessageEntity>> = chatDao.getMessagesForThread(ownerId, conversationId)

    suspend fun thread(
        ownerId: String,
        conversationId: String,
    ): ChatThreadEntity? = chatDao.getThread(ownerId, conversationId)

    /**
     * Idempotent account activation. Realtime starts immediately; independent
     * REST recovery steps continue even when one transient request fails.
     */
    suspend fun activate() {
        if (!authRepository.isLoggedIn) {
            realtime.stop()
            return
        }
        activationMutex.withLock {
            val ownerId = requireOwner()
            val failures = mutableListOf<Throwable>()
            realtime.start()
            runCatching { updateOwnProfile(ownerId) }.onFailure { failures += it }
            runCatching { syncBlocks(ownerId) }.onFailure { failures += it }
            val conversations =
                runCatching { syncConversations(ownerId) }
                    .onFailure { failures += it }
                    .getOrDefault(emptyList())
            conversations.forEach { conversation ->
                runCatching { synchronizeMessages(conversation) }
                    .onFailure { failures += it }
            }
            chatDao.getPendingMessages(ownerId).forEach {
                enqueueOutbox(it.ownerId, it.id)
            }
            runCatching { registerCurrentInstallation(ownerId) }.onFailure { failures += it }
            failures.firstOrNull()?.let { throw it }
        }
    }

    fun deactivate() {
        ChatNavigationState.setActiveConversation(null)
        realtime.stop()
    }

    suspend fun logout(): Result<Unit> {
        ChatNavigationState.setActiveConversation(null)
        ChatNavigationState.requestConversation(null)
        val installationStore = InstallationIdStore(appContext)
        val outgoingOwner = authRepository.skipperId
        val outgoingInstallationId = installationStore.installationId
        installationStore.clearAll()
        if (outgoingOwner.isNotBlank()) {
            WorkManager.getInstance(appContext).cancelAllWorkByTag(outboxTag(outgoingOwner))
            CrewspacePushWorker.cancelForOwner(appContext, outgoingOwner)
            CrewspacePushNotifications.cancelForOwner(appContext, outgoingOwner)
        }
        return outboxSessionMutex.withLock {
            val serverResult =
                runCatching {
                    if (outgoingInstallationId != null && authRepository.isLoggedIn) {
                        unregisterDevice(outgoingInstallationId)
                    }
                }
            val firebaseResult =
                runCatching {
                    if (authRepository.isFirebaseConfigured) {
                        FirebaseMessaging.getInstance().unregister().await()
                    }
                }
            installationStore.clearAll()
            realtime.stop()
            authRepository.logout()
            serverResult.fold(
                onSuccess = { firebaseResult },
                onFailure = { Result.failure(it) },
            )
        }
    }

    suspend fun createDirectChat(targetUid: String): ChatThreadEntity {
        val ownUid = requireOwner()
        val cleanUid = targetUid.trim()
        require(cleanUid.isNotEmpty()) { "Bitte eine Firebase-ID eingeben." }
        require(cleanUid != ownUid) { "Du kannst keinen Chat mit dir selbst starten." }
        val conversation =
            executeBodyForOwner(ownUid) {
                api.direct(it, ApiCreateDirectChatRequest(skipperId = cleanUid))
            }
        ensureExpectedOwner(ownUid)
        return upsertConversation(ownUid, conversation)
    }

    suspend fun syncConversations(): List<ChatThreadEntity> =
        syncConversations(requireOwner())

    private suspend fun syncConversations(ownUid: String): List<ChatThreadEntity> {
        val remote = executeBodyForOwner(ownUid) { api.conversations(it) }
        ensureExpectedOwner(ownUid)
        return remote.map { upsertConversation(ownUid, it) }
    }

    suspend fun synchronizeMessages(conversation: ChatThreadEntity) {
        val ownerId = conversation.ownerId
        var cursor = conversation.afterCursor
        val isDelta = cursor != null
        var beforeCursor = conversation.beforeCursor
        var continueDelta: Boolean

        do {
            val page =
                executeBodyForOwner(ownerId) {
                    api.messagePage(
                        authorization = it,
                        conversationId = conversation.id,
                        after = cursor,
                        limit = 100,
                    )
                }
            ensureExpectedOwner(ownerId)
            page.messages.forEach { upsertMessage(ownerId, it) }

            val nextAfter = page.nextAfterCursor ?: cursor
            if (!isDelta) {
                beforeCursor = page.nextBeforeCursor
            }
            chatDao.updateCursors(
                ownerId = ownerId,
                threadId = conversation.id,
                afterCursor = nextAfter,
                beforeCursor = beforeCursor,
            )

            val madeProgress = nextAfter != null && nextAfter != cursor
            cursor = nextAfter
            continueDelta = isDelta && page.hasMore && madeProgress
        } while (continueDelta)
    }

    suspend fun loadOlderMessages(conversationId: String): Boolean {
        val ownerId = requireOwner()
        val conversation = chatDao.getThread(ownerId, conversationId) ?: return false
        val before = conversation.beforeCursor ?: return false
        val page =
            executeBodyForOwner(ownerId) {
                api.messagePage(
                    authorization = it,
                    conversationId = conversationId,
                    before = before,
                    limit = 100,
                )
            }
        ensureExpectedOwner(ownerId)
        page.messages.forEach { upsertMessage(ownerId, it) }
        chatDao.updateCursors(
            ownerId = ownerId,
            threadId = conversationId,
            afterCursor = conversation.afterCursor,
            beforeCursor = page.nextBeforeCursor,
        )
        return page.hasMore
    }

    suspend fun markConversationRead(conversationId: String) {
        val ownerId = requireOwner()
        chatDao.clearUnreadCount(ownerId, conversationId)
        executeUnitForOwner(ownerId) { api.markRead(it, conversationId) }
    }

    suspend fun queueText(
        conversationId: String,
        text: String,
        type: ChatMessageType = ChatMessageType.TEXT,
    ): ChatMessageEntity {
        val cleanText = text.trim()
        require(cleanText.isNotEmpty()) { "Die Nachricht ist leer." }
        return queueMessage(
            conversationId = conversationId,
            text = cleanText,
            type = type,
        )
    }

    suspend fun queueAttachment(
        conversationId: String,
        source: String,
        type: ChatMessageType,
        durationSeconds: Int = 0,
    ): ChatMessageEntity {
        require(type == ChatMessageType.IMAGE || type == ChatMessageType.VOICE)
        require(durationSeconds in 0..MAX_AUDIO_DURATION_SECONDS) {
            "Sprachnachrichten dürfen höchstens 15 Minuten lang sein."
        }
        val ownerId = requireOwner()
        ensureChatAvailable(ownerId, conversationId)
        val clientMessageId = UUID.randomUUID().toString()
        val copied = copyIntoOutbox(ownerId, clientMessageId, source, type)
        return queueMessage(
            conversationId = conversationId,
            text = if (type == ChatMessageType.IMAGE) "Bild" else "Sprachnachricht",
            type = type,
            durationSeconds = durationSeconds,
            clientMessageId = clientMessageId,
            localMediaUri = Uri.fromFile(copied.file).toString(),
            mediaMimeType = copied.mimeType,
        )
    }

    suspend fun retryMessage(localId: String) {
        val ownerId = requireOwner()
        val message = chatDao.getMessage(ownerId, localId) ?: return
        val state =
            if (message.localMediaUri != null && message.mediaUrl == null) {
                ChatDeliveryState.UPLOADING
            } else {
                ChatDeliveryState.PENDING
            }
        chatDao.updateDeliveryState(ownerId, localId, state.name)
        enqueueOutbox(ownerId, localId)
    }

    suspend fun markMessageFailed(
        ownerId: String,
        localId: String,
        reason: String,
    ) {
        chatDao.updateDeliveryState(
            ownerId = ownerId,
            localId = localId,
            state = ChatDeliveryState.FAILED.name,
            reason = reason,
        )
    }

    /**
     * Called by WorkManager. Throws retryable failures and marks canonical
     * 4xx failures so the UI can offer an explicit retry.
     */
    suspend fun flushQueuedMessage(
        expectedOwnerId: String,
        localId: String,
    ) = outboxSessionMutex.withLock {
        ensureExpectedOwner(expectedOwnerId)
        var local = chatDao.getMessage(expectedOwnerId, localId) ?: return@withLock
        ensureChatAvailable(expectedOwnerId, local.threadId)

        try {
            var publicMediaUrl = local.mediaUrl
            if (publicMediaUrl == null && local.localMediaUri != null) {
                chatDao.updateDeliveryState(
                    expectedOwnerId,
                    localId,
                    ChatDeliveryState.UPLOADING.name,
                )
                publicMediaUrl = uploadMedia(expectedOwnerId, local)
                chatDao.markMediaUploaded(expectedOwnerId, localId, publicMediaUrl)
                local = chatDao.getMessage(expectedOwnerId, localId) ?: return@withLock
            }

            val response =
                executeBodyForOwner(expectedOwnerId) {
                    api.sendMessage(
                        authorization = it,
                        conversationId = local.threadId,
                        request =
                            ApiCreateMessageRequest(
                                clientMessageId =
                                    local.clientMessageId
                                        ?: throw IllegalStateException("client_message_id fehlt."),
                                text =
                                    if (local.type == ChatMessageType.TEXT ||
                                        local.type == ChatMessageType.EVENT
                                    ) {
                                        local.content
                                    } else {
                                        ""
                                    },
                                mediaUrl = publicMediaUrl,
                                mediaType =
                                    when (local.type) {
                                        ChatMessageType.IMAGE -> "image"
                                        ChatMessageType.VOICE -> "audio"
                                        else -> null
                                    },
                                mediaDurationSeconds =
                                    local.voiceDurationSeconds.takeIf { it > 0 }?.toDouble(),
                            ),
                    )
                }
            upsertMessage(expectedOwnerId, response)
            local.localMediaUri?.let { uri ->
                Uri.parse(uri).path?.let(::File)?.takeIf(File::exists)?.delete()
            }
        } catch (error: Throwable) {
            if (error is ChatApiException && error.statusCode in 400..499) {
                chatDao.updateDeliveryState(
                    expectedOwnerId,
                    localId,
                    ChatDeliveryState.FAILED.name,
                    error.userMessage,
                )
            }
            throw error
        }
    }

    suspend fun registerDevice(installationId: String) {
        if (installationId.isBlank() || !authRepository.isLoggedIn) return
        val ownerId = requireOwner()
        executeUnitForOwner(ownerId) {
            api.registerDevice(
                authorization = it,
                request = ApiDeviceRegistrationRequest(installationId = installationId),
            )
        }
    }

    suspend fun unregisterDevice(installationId: String) {
        if (installationId.isBlank() || !authRepository.isLoggedIn) return
        val ownerId = requireOwner()
        executeUnitForOwner(ownerId) { api.deleteDevice(it, installationId) }
    }

    suspend fun localPushSender(
        expectedOwnerId: String,
        conversationId: String,
        messageId: String,
    ): String? {
        ensureExpectedOwner(expectedOwnerId)
        val thread = chatDao.getThread(expectedOwnerId, conversationId) ?: return null
        if (thread.blocked) return null
        val message = chatDao.getMessageByServerId(expectedOwnerId, messageId)
        if (
            message != null &&
            (message.threadId != conversationId || message.senderId == expectedOwnerId)
        ) {
            return null
        }
        return pushSenderTitle(
            canonicalSenderName = message?.senderName,
            participantName =
                thread.participant2Name.takeIf {
                    thread.type == ChatThreadType.DIRECT
                },
        )
    }

    suspend fun resolvePushSender(
        expectedOwnerId: String,
        conversationId: String,
        messageId: String,
    ): String? {
        ensureExpectedOwner(expectedOwnerId)
        var thread = chatDao.getThread(expectedOwnerId, conversationId)
        var message = chatDao.getMessageByServerId(expectedOwnerId, messageId)
        if (message == null) {
            if (thread == null) {
                syncConversations(expectedOwnerId)
                thread = chatDao.getThread(expectedOwnerId, conversationId)
            }
            val canonicalThread = thread ?: return null
            if (canonicalThread.blocked) return null
            synchronizeMessages(canonicalThread)
            ensureExpectedOwner(expectedOwnerId)
            message = chatDao.getMessageByServerId(expectedOwnerId, messageId)
        }
        val canonicalMessage = message ?: return null
        if (thread?.blocked == true) return null
        if (
            canonicalMessage.threadId != conversationId ||
            canonicalMessage.senderId == expectedOwnerId
        ) {
            return null
        }
        return pushSenderTitle(
            canonicalSenderName = canonicalMessage.senderName,
            participantName =
                thread?.participant2Name?.takeIf {
                    thread?.type == ChatThreadType.DIRECT
                },
        )
    }

    suspend fun block(uid: String) {
        val ownerId = requireOwner()
        executeUnitForOwner(ownerId) { api.block(it, uid) }
        syncBlocks(ownerId)
        syncConversations(ownerId)
    }

    suspend fun unblock(uid: String) {
        val ownerId = requireOwner()
        executeUnitForOwner(ownerId) { api.unblock(it, uid) }
        syncBlocks(ownerId)
        syncConversations(ownerId)
    }

    private suspend fun queueMessage(
        conversationId: String,
        text: String,
        type: ChatMessageType,
        durationSeconds: Int = 0,
        clientMessageId: String = UUID.randomUUID().toString(),
        localMediaUri: String? = null,
        mediaMimeType: String? = null,
    ): ChatMessageEntity {
        val ownerId = requireOwner()
        ensureChatAvailable(ownerId, conversationId)
        val now = System.currentTimeMillis()
        val entity =
            ChatMessageEntity(
                ownerId = ownerId,
                id = clientMessageId,
                clientMessageId = clientMessageId,
                threadId = conversationId,
                senderId = ownerId,
                senderName = authRepository.userName,
                content = text,
                type = type,
                voiceDurationSeconds = durationSeconds,
                timestamp = now,
                localMediaUri = localMediaUri,
                mediaMimeType = mediaMimeType,
                deliveryState =
                    if (localMediaUri == null) {
                        ChatDeliveryState.PENDING.name
                    } else {
                        ChatDeliveryState.UPLOADING.name
                    },
            )
        chatDao.insertMessage(entity)
        chatDao.updateThreadPreview(ownerId, conversationId, text, now)
        enqueueOutbox(ownerId, entity.id)
        return entity
    }

    private suspend fun ensureChatAvailable(
        ownerId: String,
        conversationId: String,
    ) {
        val conversation = chatDao.getThread(ownerId, conversationId)
        if (conversation?.blocked == true) throw ChatUnavailableException()
    }

    private suspend fun updateOwnProfile(ownerId: String = requireOwner()) {
        val name = authRepository.userName.ifBlank { authRepository.userEmail.substringBefore("@") }
        executeUnitForOwner(ownerId) { api.updateMe(it, ApiUpdateMeRequest(name = name)) }
    }

    private suspend fun syncBlocks(ownerId: String = requireOwner()): ApiBlocksResponse {
        val blocks = executeBodyForOwner(ownerId) { api.blocks(it) }
        ensureExpectedOwner(ownerId)
        chatDao.clearDirectChatBlocks(ownerId)
        blocks.blockedUids.forEach { chatDao.setDirectChatBlocked(ownerId, it, true) }
        return blocks
    }

    private suspend fun registerCurrentInstallation(ownerId: String = requireOwner()) {
        if (!authRepository.isFirebaseConfigured) return
        ensureExpectedOwner(ownerId)
        val store = InstallationIdStore(appContext)
        // Backup rules exclude this device-bound state. Preserve a confirmed
        // same-owner binding while Firebase reports the same FID again.
        store.beginRegistration(ownerId)
        FirebaseMessaging.getInstance().register().await()
        ensureExpectedOwner(ownerId)
        store.installationId?.let { installationId ->
            executeUnitForOwner(ownerId) {
                api.registerDevice(
                    authorization = it,
                    request = ApiDeviceRegistrationRequest(installationId = installationId),
                )
            }
            store.markRegistered(ownerId, installationId)
        }
    }

    private suspend fun recoverAfterReconnect(ownerId: String) {
        if (authRepository.skipperId != ownerId) return
        runCatching { syncBlocks(ownerId) }
        val conversations =
            runCatching { syncConversations(ownerId) }
                .getOrDefault(emptyList())
        conversations.forEach {
            runCatching { synchronizeMessages(it) }
        }
    }

    private suspend fun handleRealtimeEnvelope(
        ownerId: String,
        envelope: ApiRealtimeEnvelope,
    ) {
        if (authRepository.skipperId != ownerId) return
        when (envelope.type) {
            "message.created" -> envelope.message?.let { upsertMessage(ownerId, it) }
            "conversation.updated" ->
                envelope.conversation?.let { upsertConversation(ownerId, it) }
            "ready" -> Unit
        }
    }

    private suspend fun upsertConversation(
        ownerId: String,
        remote: ApiCrewspaceConversation,
    ): ChatThreadEntity {
        val existing = chatDao.getThread(ownerId, remote.id)
        val isGroup = remote.kind.equals("group", ignoreCase = true)
        val otherIndex = remote.memberIds?.indexOfFirst { it != ownerId } ?: -1
        val otherId =
            if (isGroup) {
                ""
            } else {
                remote.memberIds?.getOrNull(otherIndex)
                    ?: existing?.participant2Id
                    ?: ""
            }
        val otherName =
            if (isGroup) {
                remote.title.ifBlank { existing?.participant2Name.orEmpty() }
            } else {
                remote.memberNames?.getOrNull(otherIndex)
                    ?: remote.title.takeIf { it.isNotBlank() }
                    ?: existing?.participant2Name
                    ?: otherId
            }
        val canonicalMillis =
            parseServerTimestamp(remote.lastMessageAt ?: remote.updatedAt)
                ?: existing?.lastMessageTimestamp
                ?: 0L
        val entity =
            ChatThreadEntity(
                ownerId = ownerId,
                id = remote.id,
                type =
                    if (isGroup) {
                        ChatThreadType.GROUP
                    } else {
                        ChatThreadType.DIRECT
                    },
                participant1Id = ownerId,
                participant1Name = authRepository.userName,
                participant2Id = otherId,
                participant2Name = otherName,
                lastMessage = remote.lastMessage.orEmpty(),
                lastMessageTimestamp = canonicalMillis,
                unreadCount = remote.unreadCount,
                lastMessageAt = remote.lastMessageAt,
                updatedAt = remote.updatedAt,
                blocked = !remote.chatAvailable,
                blockedByMe = existing?.blockedByMe ?: false,
                afterCursor = existing?.afterCursor,
                beforeCursor = existing?.beforeCursor,
            )
        chatDao.insertThread(entity)
        return entity
    }

    private suspend fun upsertMessage(
        ownerId: String,
        remote: ApiCrewspaceMessage,
    ): ChatMessageEntity {
        val existing =
            chatDao.getMessageByServerId(ownerId, remote.id)
                ?: remote.clientMessageId?.let {
                    chatDao.getMessageByClientId(ownerId, remote.conversationId, it)
                }
        val type =
            when {
                remote.mediaType == "image" -> ChatMessageType.IMAGE
                remote.mediaType == "audio" -> ChatMessageType.VOICE
                remote.event != null -> ChatMessageType.EVENT
                remote.poll != null -> ChatMessageType.UNKNOWN
                remote.mediaType.isNullOrBlank() && remote.text.isNotBlank() -> ChatMessageType.TEXT
                else -> ChatMessageType.UNKNOWN
            }
        val entity =
            ChatMessageEntity(
                ownerId = ownerId,
                id = existing?.id ?: remote.id,
                serverId = remote.id,
                clientMessageId = remote.clientMessageId ?: existing?.clientMessageId,
                threadId = remote.conversationId,
                senderId = remote.senderId,
                senderName = remote.senderName,
                content =
                    remote.text.takeIf { it.isNotBlank() }
                        ?: if (type == ChatMessageType.UNKNOWN) {
                            "Nicht unterstützte Nachricht"
                        } else {
                            ""
                        },
                type = type,
                voiceDurationSeconds = remote.mediaDurationSeconds?.toInt() ?: 0,
                timestamp =
                    parseServerTimestamp(remote.createdAt)
                        ?: existing?.timestamp
                        ?: System.currentTimeMillis(),
                serverCreatedAt = remote.createdAt,
                mediaUrl = remote.mediaUrl,
                localMediaUri = null,
                mediaMimeType = existing?.mediaMimeType,
                deliveryState = ChatDeliveryState.SENT.name,
                failureReason = null,
            )
        chatDao.insertMessage(entity)
        if (existing != null && existing.id != entity.id) {
            chatDao.deleteMessage(ownerId, existing.id)
        }
        return entity
    }

    private suspend fun uploadMedia(
        expectedOwnerId: String,
        message: ChatMessageEntity,
    ): String {
        ensureExpectedOwner(expectedOwnerId)
        val localPath =
            message.localMediaUri?.let(Uri::parse)?.path
                ?: throw IOException("Lokale Mediendatei fehlt.")
        val file = File(localPath)
        if (!file.isFile) throw IOException("Lokale Mediendatei wurde nicht gefunden.")
        val mimeType = message.mediaMimeType ?: "application/octet-stream"
        val extension = file.extension.ifBlank { "bin" }
        val presign =
            executeBodyForOwner(expectedOwnerId) {
                api.presignUpload(
                    it,
                    ApiPresignUploadRequest(
                        contentType = mimeType,
                        fileExtension = extension,
                    ),
                )
            }
        ensureExpectedOwner(expectedOwnerId)
        val body = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val requestBuilder = Request.Builder().url(presign.uploadUrl)
        presign.headers.orEmpty().forEach { (name, value) ->
            requestBuilder.header(name, value)
        }
        val request =
            when (presign.method.uppercase()) {
                "POST" -> requestBuilder.post(body).build()
                else -> requestBuilder.put(body).build()
            }
        ensureExpectedOwner(expectedOwnerId)
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Upload fehlgeschlagen (${response.code}).")
            }
        }
        ensureExpectedOwner(expectedOwnerId)
        return presign.publicUrl
    }

    private suspend fun copyIntoOutbox(
        ownerId: String,
        clientMessageId: String,
        source: String,
        type: ChatMessageType,
    ): CopiedMedia =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(source)
            val resolver = appContext.contentResolver
            val detectedMime =
                resolver.getType(uri)
                    ?: if (type == ChatMessageType.IMAGE) "image/jpeg" else "audio/mp4"
            val extension =
                MimeTypeMap.getSingleton().getExtensionFromMimeType(detectedMime)
                    ?: if (type == ChatMessageType.IMAGE) "jpg" else "m4a"
            val directory = File(appContext.filesDir, "chat_outbox/$ownerId").apply { mkdirs() }
            val destination = File(directory, "$clientMessageId.$extension")
            val input =
                when {
                    uri.scheme == "file" -> uri.path?.let(::File)?.inputStream()
                    else -> resolver.openInputStream(uri)
                } ?: throw IOException("Die ausgewählte Datei konnte nicht gelesen werden.")
            input.use { sourceStream ->
                destination.outputStream().use { target -> sourceStream.copyTo(target) }
            }
            if (uri.scheme == "file") {
                val sourceFile = uri.path?.let(::File)
                val recordingsCache = File(appContext.cacheDir, "chat_recordings")
                if (sourceFile != null && sourceFile.parentFile == recordingsCache) {
                    sourceFile.delete()
                }
            }
            CopiedMedia(destination, detectedMime)
        }

    private fun enqueueOutbox(
        ownerId: String,
        localId: String,
    ) {
        val data =
            Data.Builder()
                .putString(ChatOutboxWorker.KEY_OWNER_ID, ownerId)
                .putString(ChatOutboxWorker.KEY_LOCAL_ID, localId)
                .build()
        val request =
            OneTimeWorkRequestBuilder<ChatOutboxWorker>()
                .setInputData(data)
                .addTag(outboxTag(ownerId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10,
                    TimeUnit.SECONDS,
                )
                .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
            "crewspace-send-$ownerId-$localId",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun outboxTag(ownerId: String) = "crewspace-outbox-owner-$ownerId"

    private fun requireOwner(): String {
        ensureSecureConfiguration()
        return authRepository.skipperId.takeIf { it.isNotBlank() }
            ?: throw AuthenticationRequiredException()
    }

    private fun ensureSecureConfiguration() {
        val baseUrl = secureHttpsBaseUrl(BuildConfig.CREWSPACE_BASE_URL)
        if (baseUrl == "https://example.invalid/") {
            throw ChatConfigurationException(
                "CREWSPACE_BASE_URL muss auf eine konfigurierte HTTPS-Adresse zeigen.",
            )
        }
    }

    private suspend fun <T> executeBodyForOwner(
        expectedOwnerId: String,
        block: suspend (String) -> Response<T>,
    ): T {
        ensureSecureConfiguration()
        val response = executeForOwner(expectedOwnerId, block)
        return response.body()
            ?: throw ChatApiException(response.code(), "Der Server lieferte keine Daten.")
    }

    private suspend fun executeUnitForOwner(
        expectedOwnerId: String,
        block: suspend (String) -> Response<*>,
    ) {
        ensureSecureConfiguration()
        ensureExpectedOwner(expectedOwnerId)
        var token = authRepository.getIdToken()
        ensureExpectedOwner(expectedOwnerId)
        var response = block("Bearer $token")
        ensureExpectedOwner(expectedOwnerId)
        if (response.code() == 401) {
            token = authRepository.getIdToken(forceRefresh = true)
            ensureExpectedOwner(expectedOwnerId)
            response = block("Bearer $token")
            ensureExpectedOwner(expectedOwnerId)
        }
        if (!response.isSuccessful) {
            val serverError = response.errorBody()?.string()?.take(500)
            throw ChatApiException(response.code(), serverError)
        }
    }

    private suspend fun <T> executeForOwner(
        expectedOwnerId: String,
        block: suspend (String) -> Response<T>,
    ): Response<T> {
        ensureSecureConfiguration()
        ensureExpectedOwner(expectedOwnerId)
        var token = authRepository.getIdToken()
        ensureExpectedOwner(expectedOwnerId)
        var response = block("Bearer $token")
        ensureExpectedOwner(expectedOwnerId)
        if (response.code() == 401) {
            token = authRepository.getIdToken(forceRefresh = true)
            ensureExpectedOwner(expectedOwnerId)
            response = block("Bearer $token")
            ensureExpectedOwner(expectedOwnerId)
        }
        if (!response.isSuccessful) {
            val serverError = response.errorBody()?.string()?.take(500)
            throw ChatApiException(response.code(), serverError)
        }
        return response
    }

    private fun ensureExpectedOwner(expectedOwnerId: String) {
        if (
            expectedOwnerId.isBlank() ||
            !authRepository.isLoggedIn ||
            authRepository.skipperId != expectedOwnerId
        ) {
            throw AccountNotActiveException()
        }
    }
}

internal data class CopiedMedia(
    val file: File,
    val mimeType: String,
)

class ChatApiException(
    val statusCode: Int,
    serverMessage: String? = null,
) : IOException(
        when {
            statusCode == 403 -> "Chat nicht verfügbar."
            statusCode == 404 -> "Unterhaltung oder Firebase-ID nicht gefunden."
            statusCode == 401 -> "Die Firebase-Sitzung ist abgelaufen."
            else -> serverMessage?.takeIf { it.isNotBlank() } ?: "Serverfehler ($statusCode)."
        },
    ) {
    val userMessage: String
        get() = message ?: "Chat-Anfrage fehlgeschlagen."
}

class ChatUnavailableException : IllegalStateException("Chat nicht verfügbar.")

class AccountNotActiveException : IllegalStateException("Ein anderes Konto ist aktiv.")

class AuthenticationRequiredException :
    IllegalStateException("Für den Crewspace-Chat ist eine Firebase-Anmeldung erforderlich.")

internal fun parseServerTimestamp(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value).toEpochMilli() }
        .recoverCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        .getOrNull()
}

class ChatConfigurationException(message: String) : IllegalStateException(message)

private const val MAX_AUDIO_DURATION_SECONDS = 900
