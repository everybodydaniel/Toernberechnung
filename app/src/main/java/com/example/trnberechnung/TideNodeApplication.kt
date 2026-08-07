package com.example.trnberechnung

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import com.example.trnberechnung.database.AppDatabase
import com.example.trnberechnung.model.AuthRepository
import com.example.trnberechnung.navigation.ActiveVoyageManager
import com.example.trnberechnung.navigation.FusedLocationProvider
import com.example.trnberechnung.navigation.NavigationTracker
import com.example.trnberechnung.navigation.VoyageServiceDependencies
import com.example.trnberechnung.navigation.VoyageServiceHost
import com.example.trnberechnung.repository.ActiveVoyageRepository
import com.example.trnberechnung.repository.ChatRepository
import com.example.trnberechnung.repository.MaritimeNoticeForegroundPoller
import com.example.trnberechnung.repository.MaritimeNoticeRepository
import com.example.trnberechnung.repository.NautiConversationRepository
import com.example.trnberechnung.repository.RoomActiveVoyagePersistence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class TideNodeApplication :
    Application(),
    VoyageServiceHost {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy {
        Room
            .databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "tide_database",
            ).addMigrations(
                AppDatabase.MIGRATION_8_10,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
            )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    val authRepository: AuthRepository by lazy { AuthRepository(this) }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(
            context = this,
            chatDao = database.chatDao(),
            authRepository = authRepository,
        )
    }

    val maritimeNoticeRepository: MaritimeNoticeRepository by lazy {
        MaritimeNoticeRepository(
            noticeDao = database.seafarerMessageDao(),
            syncDao = database.maritimeNoticeSyncDao(),
        )
    }

    private val maritimeNoticeForegroundPoller: MaritimeNoticeForegroundPoller by lazy {
        MaritimeNoticeForegroundPoller(
            application = this,
            repository = maritimeNoticeRepository,
            scope = applicationScope,
        )
    }

    val nautiConversationRepository: NautiConversationRepository by lazy {
        NautiConversationRepository(database.nautiDao(), ::localDataOwnerId)
    }

    val activeVoyageRepository: ActiveVoyageRepository by lazy {
        ActiveVoyageRepository(database.activeVoyageDao(), ::localDataOwnerId)
    }

    val roomActiveVoyagePersistence: RoomActiveVoyagePersistence by lazy {
        RoomActiveVoyagePersistence(database, ::localDataOwnerId)
    }

    val navigationLocationProvider: FusedLocationProvider by lazy {
        FusedLocationProvider(this)
    }

    val activeVoyageManager: ActiveVoyageManager by lazy {
        ActiveVoyageManager(
            navigationTracker = NavigationTracker(),
            persistence = roomActiveVoyagePersistence,
        )
    }

    override val voyageServiceDependencies: VoyageServiceDependencies by lazy {
        VoyageServiceDependencies(
            locationProvider = navigationLocationProvider,
            activeVoyageManager = activeVoyageManager,
        )
    }

    override fun onCreate() {
        super.onCreate()
        createChatNotificationChannel()
        maritimeNoticeForegroundPoller.start()
        if (authRepository.isLoggedIn) {
            applicationScope.launch { runCatching { chatRepository.activate() } }
        }
    }

    private fun createChatNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel =
            NotificationChannel(
                CHAT_NOTIFICATION_CHANNEL_ID,
                "Crewspace-Nachrichten",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Neue Direkt- und Gruppennachrichten"
            }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /**
     * Keeps local Nauti and voyage data account-isolated. Skipped sign-in uses
     * one installation-stable guest namespace instead of leaking into a shared
     * empty owner ID.
     */
    fun localDataOwnerId(): String {
        authRepository.skipperId
            .trim()
            .takeIf(String::isNotEmpty)
            ?.let { return it }
        val preferences = getSharedPreferences(LOCAL_DATA_PREFS, MODE_PRIVATE)
        val existing = preferences.getString(GUEST_OWNER_KEY, null)
        if (!existing.isNullOrBlank()) return "guest:$existing"
        val generated = UUID.randomUUID().toString()
        preferences.edit().putString(GUEST_OWNER_KEY, generated).apply()
        return "guest:$generated"
    }

    companion object {
        const val CHAT_NOTIFICATION_CHANNEL_ID = "crewspace_messages"
        private const val LOCAL_DATA_PREFS = "local_data_owner"
        private const val GUEST_OWNER_KEY = "guest_owner_id"
    }
}
