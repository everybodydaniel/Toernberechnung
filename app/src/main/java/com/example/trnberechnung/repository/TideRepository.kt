package com.example.trnberechnung.repository

import com.example.trnberechnung.database.TideDao
import com.example.trnberechnung.database.TideEntity
import com.example.trnberechnung.model.LogbookDao
import com.example.trnberechnung.model.LogbookEntry
import com.example.trnberechnung.model.CrewMemberDao
import com.example.trnberechnung.model.CrewMember
import com.example.trnberechnung.model.ChecklistDao
import com.example.trnberechnung.model.ChecklistItem
import com.example.trnberechnung.model.TideStationData
import com.example.trnberechnung.model.toEntity
import com.example.trnberechnung.model.toModel
import com.example.trnberechnung.network.RetrofitInstance
import com.example.trnberechnung.dto.WeatherDto
import kotlinx.coroutines.flow.Flow
import com.example.trnberechnung.database.ChatDao
import com.example.trnberechnung.database.ChatThreadEntity
import com.example.trnberechnung.database.ChatMessageEntity
import com.example.trnberechnung.database.PlannerEventDao
import com.example.trnberechnung.database.PlannerEventEntity

class TideRepository(
    private val tideDao: TideDao,
    private val logbookDao: LogbookDao,
    private val crewMemberDao: CrewMemberDao,
    private val checklistDao: ChecklistDao,
    private val chatDao: ChatDao,
    private val plannerEventDao: PlannerEventDao
) {

    suspend fun getDataFromApi(): List<TideStationData> {
        return try {
            android.util.Log.d("BSH_API", "Calling BSH API...")
            val response = RetrofitInstance.bshApi.getWaterLevel(100, "north_sea")
            android.util.Log.d("BSH_API", "Response code: ${response.code()}")
            if (response.isSuccessful) {
                val body = response.body()
                android.util.Log.d("BSH_API", "Features count: ${body?.features?.size ?: "null body"}")
                val apiData = body?.features?.map { it.toModel() } ?: emptyList()
                android.util.Log.d("BSH_API", "Parsed stations: ${apiData.size}, first events: ${apiData.firstOrNull()?.events?.size ?: 0}")

                if (apiData.isNotEmpty()) {
                    tideDao.deleteAll()
                    tideDao.insertAll(apiData.map { it.toEntity() })
                }
                apiData
            } else {
                android.util.Log.e("BSH_API", "API error: ${response.code()} - ${response.errorBody()?.string()?.take(200)}")
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("BSH_API", "Exception: ${e.javaClass.simpleName}: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getDataFromDatabase(): List<TideEntity> {
        return tideDao.getAll()
    }

    suspend fun getWeatherData(lat: Double, lon: Double): WeatherDto? {
        return try {
            val response = RetrofitInstance.dwdApi.getCurrentWeather(lat, lon)
            response.weather
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getForecastData(lat: Double, lon: Double, start: String, end: String): List<WeatherDto> {
        return try {
            val response = RetrofitInstance.dwdApi.getForecast(lat, lon, start, end)
            response.weather ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    val allLogs: Flow<List<LogbookEntry>> = logbookDao.getAllLogs()

    suspend fun insertLog(log: LogbookEntry) {
        logbookDao.insertLog(log)
    }

    suspend fun updateLog(log: LogbookEntry) {
        logbookDao.updateLog(log)
    }

    suspend fun deleteLog(log: LogbookEntry) {
        logbookDao.deleteLog(log)
    }

    suspend fun deleteAllLogs() {
        logbookDao.deleteAllLogs()
    }

    val allCrew: Flow<List<CrewMember>> = crewMemberDao.getAllCrew()

    suspend fun insertCrew(member: CrewMember) {
        crewMemberDao.insertCrew(member)
    }

    suspend fun updateCrew(member: CrewMember) {
        crewMemberDao.updateCrew(member)
    }

    suspend fun deleteCrew(member: CrewMember) {
        crewMemberDao.deleteCrew(member)
    }

    fun getChecklistForTrip(tripId: Int): Flow<List<ChecklistItem>> =
        checklistDao.getItemsForTrip(tripId)

    suspend fun checklistCountForTrip(tripId: Int): Int =
        checklistDao.countForTrip(tripId)

    suspend fun insertChecklistItems(items: List<ChecklistItem>) {
        checklistDao.insertAll(items)
    }

    suspend fun updateChecklistItem(item: ChecklistItem) {
        checklistDao.update(item)
    }

    // ══════════════════════════════════════════════════════════════
    // CHATS & PLANNER (Local DB + Remote Sync)
    // ══════════════════════════════════════════════════════════════

    fun getChatThreadsForUser(userId: String): Flow<List<ChatThreadEntity>> =
        chatDao.getThreadsForUser(userId)

    suspend fun insertChatThread(thread: ChatThreadEntity) {
        chatDao.insertThread(thread)
    }

    fun getMessagesForThread(threadId: String): Flow<List<ChatMessageEntity>> =
        chatDao.getMessagesForThread(threadId)

    suspend fun insertChatMessage(message: ChatMessageEntity) {
        chatDao.insertMessage(message)
    }

    val allPlannerEvents: Flow<List<PlannerEventEntity>> =
        plannerEventDao.getAllEvents()

    suspend fun insertPlannerEvent(event: PlannerEventEntity) {
        plannerEventDao.insertEvent(event)
    }

    suspend fun deletePlannerEvent(event: PlannerEventEntity) {
        plannerEventDao.deleteEvent(event)
    }

    // ── Server Sync Methods ──

    suspend fun syncRemoteConversations(idToken: String, ownUserId: String) {
        if (idToken.isBlank()) return
        try {
            val response = com.example.trnberechnung.network.RetrofitInstance.socialFeedApi.getConversations("Bearer $idToken")
            if (response.isSuccessful) {
                response.body()?.forEach { conv ->
                    val otherIndex = conv.memberIds?.indexOfFirst { it != ownUserId } ?: -1
                    val otherId = if (otherIndex >= 0) conv.memberIds!![otherIndex] else (conv.memberIds?.firstOrNull() ?: "")
                    val otherName = if (otherIndex >= 0) (conv.memberNames?.getOrNull(otherIndex) ?: conv.title) else conv.title

                    val entity = ChatThreadEntity(
                        id = conv.id,
                        type = if (conv.kind == "group") com.example.trnberechnung.model.ChatThreadType.GROUP else com.example.trnberechnung.model.ChatThreadType.DIRECT,
                        participant1Id = ownUserId,
                        participant1Name = "",
                        participant2Id = otherId,
                        participant2Name = otherName,
                        lastMessage = conv.lastMessage ?: "",
                        lastMessageTimestamp = System.currentTimeMillis(),
                        unreadCount = conv.unreadCount
                    )
                    chatDao.insertThread(entity)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SYNC", "Error syncing conversations: ${e.message}")
        }
    }

    suspend fun syncRemoteMessages(idToken: String, threadId: String) {
        if (idToken.isBlank() || threadId.isBlank()) return
        try {
            val response = com.example.trnberechnung.network.RetrofitInstance.socialFeedApi.getMessages("Bearer $idToken", threadId)
            if (response.isSuccessful) {
                response.body()?.forEach { msg ->
                    val type = when (msg.mediaType) {
                        "image" -> com.example.trnberechnung.model.ChatMessageType.IMAGE
                        "audio" -> com.example.trnberechnung.model.ChatMessageType.VOICE
                        else -> {
                            if (msg.text.startsWith("Termin:")) com.example.trnberechnung.model.ChatMessageType.EVENT
                            else com.example.trnberechnung.model.ChatMessageType.TEXT
                        }
                    }
                    val entity = ChatMessageEntity(
                        id = msg.id,
                        threadId = msg.conversationId,
                        senderId = msg.senderId,
                        senderName = msg.senderName,
                        content = msg.text,
                        type = type,
                        voiceDurationSeconds = msg.mediaDurationSeconds?.toInt() ?: 0,
                        timestamp = System.currentTimeMillis()
                    )
                    chatDao.insertMessage(entity)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SYNC", "Error syncing messages: ${e.message}")
        }
    }

    suspend fun sendRemoteMessage(idToken: String, threadId: String, text: String, senderId: String, senderName: String): Boolean {
        if (idToken.isNotBlank()) {
            try {
                val req = com.example.trnberechnung.network.ApiCreateMessageRequest(text = text)
                val response = com.example.trnberechnung.network.RetrofitInstance.socialFeedApi.sendMessage("Bearer $idToken", threadId, req)
                if (response.isSuccessful && response.body() != null) {
                    val msg = response.body()!!
                    val entity = ChatMessageEntity(
                        id = msg.id,
                        threadId = msg.conversationId,
                        senderId = msg.senderId,
                        senderName = msg.senderName,
                        content = msg.text,
                        type = com.example.trnberechnung.model.ChatMessageType.TEXT,
                        voiceDurationSeconds = 0,
                        timestamp = System.currentTimeMillis()
                    )
                    chatDao.insertMessage(entity)
                    return true
                }
            } catch (e: Exception) {
                android.util.Log.e("SYNC", "Error sending message to remote: ${e.message}")
            }
        }
        // Fallback local insert
        val localMsg = ChatMessageEntity(
            id = java.util.UUID.randomUUID().toString(),
            threadId = threadId,
            senderId = senderId,
            senderName = senderName,
            content = text,
            type = com.example.trnberechnung.model.ChatMessageType.TEXT,
            voiceDurationSeconds = 0,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(localMsg)
        return false
    }

    suspend fun syncRemoteEvents(idToken: String) {
        if (idToken.isBlank()) return
        try {
            val response = com.example.trnberechnung.network.RetrofitInstance.socialFeedApi.getEvents("Bearer $idToken")
            if (response.isSuccessful) {
                response.body()?.forEach { ev ->
                    val localDate = try {
                        java.time.OffsetDateTime.parse(ev.startsAt).toLocalDate()
                    } catch (e: Exception) {
                        java.time.LocalDate.now()
                    }
                    val entity = PlannerEventEntity(
                        id = ev.id,
                        date = localDate,
                        title = ev.title,
                        description = ev.notes ?: ""
                    )
                    plannerEventDao.insertEvent(entity)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SYNC", "Error syncing events: ${e.message}")
        }
    }

    suspend fun createRemoteDirectChat(idToken: String, targetSkipperId: String, ownUserId: String): ChatThreadEntity? {
        if (idToken.isBlank() || targetSkipperId.isBlank()) return null
        try {
            val req = com.example.trnberechnung.network.ApiCreateDirectChatRequest(skipperId = targetSkipperId)
            val response = com.example.trnberechnung.network.RetrofitInstance.socialFeedApi.createDirectChat("Bearer $idToken", req)
            if (response.isSuccessful && response.body() != null) {
                val conv = response.body()!!
                val otherIndex = conv.memberIds?.indexOfFirst { it != ownUserId } ?: -1
                val otherId = if (otherIndex >= 0) conv.memberIds!![otherIndex] else targetSkipperId
                val otherName = if (otherIndex >= 0) (conv.memberNames?.getOrNull(otherIndex) ?: conv.title) else conv.title

                val entity = ChatThreadEntity(
                    id = conv.id,
                    type = com.example.trnberechnung.model.ChatThreadType.DIRECT,
                    participant1Id = ownUserId,
                    participant1Name = "",
                    participant2Id = otherId,
                    participant2Name = otherName.ifBlank { targetSkipperId },
                    lastMessage = conv.lastMessage ?: "",
                    lastMessageTimestamp = System.currentTimeMillis(),
                    unreadCount = conv.unreadCount
                )
                chatDao.insertThread(entity)
                return entity
            }
        } catch (e: Exception) {
            android.util.Log.e("SYNC", "Error creating direct chat: ${e.message}")
        }
        return null
    }
}
