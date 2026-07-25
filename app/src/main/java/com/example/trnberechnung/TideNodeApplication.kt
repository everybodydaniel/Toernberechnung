package com.example.trnberechnung

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import com.example.trnberechnung.database.AppDatabase
import com.example.trnberechnung.model.AuthRepository
import com.example.trnberechnung.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TideNodeApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "tide_database",
        ).addMigrations(
            AppDatabase.MIGRATION_8_10,
            AppDatabase.MIGRATION_9_10,
        )
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

    override fun onCreate() {
        super.onCreate()
        createChatNotificationChannel()
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

    companion object {
        const val CHAT_NOTIFICATION_CHANNEL_ID = "crewspace_messages"
    }
}
