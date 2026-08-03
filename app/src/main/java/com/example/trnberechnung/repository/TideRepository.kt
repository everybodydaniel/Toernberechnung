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
import com.example.trnberechnung.database.PlannerEventDao
import com.example.trnberechnung.database.PlannerEventEntity
import com.example.trnberechnung.database.SeafarerMessageDao
import com.example.trnberechnung.database.SeafarerMessageEntity

class TideRepository(
    private val tideDao: TideDao,
    private val logbookDao: LogbookDao,
    private val crewMemberDao: CrewMemberDao,
    private val checklistDao: ChecklistDao,
    private val plannerEventDao: PlannerEventDao,
    private val seafarerMessageDao: SeafarerMessageDao? = null,
    private val maritimeNoticeRepository: MaritimeNoticeRepository? = null,
) {
    private val noticeRepository: MaritimeNoticeRepository? by lazy {
        maritimeNoticeRepository
    }

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
        crewMemberDao.insertCrew(com.example.trnberechnung.logic.ValidationUtils.sanitizeCrewMember(member))
    }

    suspend fun updateCrew(member: CrewMember) {
        crewMemberDao.updateCrew(com.example.trnberechnung.logic.ValidationUtils.sanitizeCrewMember(member))
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
    // PLANNER (Chat lives in the dedicated ChatRepository)
    // ══════════════════════════════════════════════════════════════

    val allPlannerEvents: Flow<List<PlannerEventEntity>> =
        plannerEventDao.getAllEvents()

    suspend fun insertPlannerEvent(event: PlannerEventEntity) {
        plannerEventDao.insertEvent(event)
    }

    suspend fun deletePlannerEvent(event: PlannerEventEntity) {
        plannerEventDao.deleteEvent(event)
    }

    // ── Server Sync Methods ──

    suspend fun syncRemoteEvents(idToken: String) {
        if (idToken.isBlank()) return
        try {
            val response =
                com.example.trnberechnung.network.RetrofitInstance.crewspaceApi.events(
                    "Bearer $idToken",
                )
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

    // ══════════════════════════════════════════════════════════════
    // SEAFARER MESSAGES (BfS-Nachrichten)
    // ══════════════════════════════════════════════════════════════

    val allActiveMessages: Flow<List<SeafarerMessageEntity>>
        get() = seafarerMessageDao?.getAllActive() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    val unreadMessages: Flow<List<SeafarerMessageEntity>>
        get() = seafarerMessageDao?.getUnread() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    val readMessages: Flow<List<SeafarerMessageEntity>>
        get() = seafarerMessageDao?.getRead() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    val archivedMessages: Flow<List<SeafarerMessageEntity>>
        get() = seafarerMessageDao?.getArchived() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    val unreadMessageCount: Flow<Int>
        get() = seafarerMessageDao?.getUnreadCount() ?: kotlinx.coroutines.flow.flowOf(0)

    fun searchMessages(query: String): Flow<List<SeafarerMessageEntity>> =
        seafarerMessageDao?.search(query) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun markMessageAsRead(messageId: String) {
        noticeRepository?.markRead(messageId) ?: seafarerMessageDao?.markAsRead(messageId)
    }

    suspend fun markAllMessagesAsRead() {
        noticeRepository?.markAllRead() ?: seafarerMessageDao?.markAllAsRead()
    }

    suspend fun archiveMessage(messageId: String) {
        noticeRepository?.archiveLocally(messageId) ?: seafarerMessageDao?.archive(messageId)
    }

    suspend fun syncSeafarerMessages(force: Boolean = false) {
        val repository = noticeRepository ?: return
        runCatching { repository.refresh(force) }
            .onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                android.util.Log.e("ELWIS", "Sync error: ${error.message}", error)
            }
    }
}
