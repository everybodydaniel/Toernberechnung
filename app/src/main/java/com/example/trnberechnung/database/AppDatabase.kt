package com.example.trnberechnung.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.trnberechnung.model.LogbookEntry
import com.example.trnberechnung.model.LogbookDao
import com.example.trnberechnung.model.CrewMember
import com.example.trnberechnung.model.CrewMemberDao
import com.example.trnberechnung.model.ChecklistItem
import com.example.trnberechnung.model.ChecklistDao
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@TypeConverters(Converters::class)

@Database(
    entities = [
        TideEntity::class, 
        LogbookEntry::class, 
        CrewMember::class, 
        ChecklistItem::class,
        ChatThreadEntity::class,
        ChatMessageEntity::class,
        PlannerEventEntity::class,
        SeafarerMessageEntity::class
    ], 
    version = 10,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tideDao(): TideDao
    abstract fun logbookDao(): LogbookDao
    abstract fun crewMemberDao(): CrewMemberDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun chatDao(): ChatDao
    abstract fun plannerEventDao(): PlannerEventDao
    abstract fun seafarerMessageDao(): SeafarerMessageDao

    companion object {
        val MIGRATION_9_10 =
            object : Migration(9, 10) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `chat_threads_v10` (
                            `ownerId` TEXT NOT NULL,
                            `id` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `participant1Id` TEXT NOT NULL,
                            `participant1Name` TEXT NOT NULL,
                            `participant2Id` TEXT NOT NULL,
                            `participant2Name` TEXT NOT NULL,
                            `lastMessage` TEXT NOT NULL,
                            `lastMessageTimestamp` INTEGER NOT NULL,
                            `unreadCount` INTEGER NOT NULL,
                            `lastMessageAt` TEXT,
                            `updatedAt` TEXT,
                            `blocked` INTEGER NOT NULL,
                            `blockedByMe` INTEGER NOT NULL,
                            `afterCursor` TEXT,
                            `beforeCursor` TEXT,
                            PRIMARY KEY(`ownerId`, `id`)
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        INSERT INTO `chat_threads_v10` (
                            ownerId, id, type, participant1Id, participant1Name,
                            participant2Id, participant2Name, lastMessage,
                            lastMessageTimestamp, unreadCount, blocked, blockedByMe
                        )
                        SELECT participant1Id, id, type, participant1Id, participant1Name,
                            participant2Id, participant2Name, lastMessage,
                            lastMessageTimestamp, unreadCount, 0, 0
                        FROM `chat_threads`
                        """.trimIndent(),
                    )

                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `chat_messages_v10` (
                            `ownerId` TEXT NOT NULL,
                            `id` TEXT NOT NULL,
                            `serverId` TEXT,
                            `clientMessageId` TEXT,
                            `threadId` TEXT NOT NULL,
                            `senderId` TEXT NOT NULL,
                            `senderName` TEXT NOT NULL,
                            `content` TEXT NOT NULL,
                            `type` TEXT NOT NULL,
                            `voiceDurationSeconds` INTEGER NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            `serverCreatedAt` TEXT,
                            `mediaUrl` TEXT,
                            `localMediaUri` TEXT,
                            `mediaMimeType` TEXT,
                            `deliveryState` TEXT NOT NULL,
                            `failureReason` TEXT,
                            PRIMARY KEY(`ownerId`, `id`)
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        INSERT INTO `chat_messages_v10` (
                            ownerId, id, serverId, threadId, senderId, senderName,
                            content, type, voiceDurationSeconds, timestamp, deliveryState
                        )
                        SELECT COALESCE(
                                (SELECT participant1Id FROM chat_threads
                                 WHERE chat_threads.id = chat_messages.threadId LIMIT 1),
                                ''
                            ),
                            id, id, threadId, senderId, senderName, content, type,
                            voiceDurationSeconds, timestamp, 'SENT'
                        FROM `chat_messages`
                        """.trimIndent(),
                    )

                    db.execSQL("DROP TABLE `chat_messages`")
                    db.execSQL("DROP TABLE `chat_threads`")
                    db.execSQL("ALTER TABLE `chat_messages_v10` RENAME TO `chat_messages`")
                    db.execSQL("ALTER TABLE `chat_threads_v10` RENAME TO `chat_threads`")

                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_chat_threads_ownerId` " +
                            "ON `chat_threads` (`ownerId`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_chat_threads_ownerId_lastMessageTimestamp` " +
                            "ON `chat_threads` (`ownerId`, `lastMessageTimestamp`)",
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_chat_messages_ownerId_serverId` " +
                            "ON `chat_messages` (`ownerId`, `serverId`)",
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS " +
                            "`index_chat_messages_ownerId_threadId_clientMessageId` " +
                            "ON `chat_messages` (`ownerId`, `threadId`, `clientMessageId`)",
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_chat_messages_ownerId_threadId_timestamp` " +
                            "ON `chat_messages` (`ownerId`, `threadId`, `timestamp`)",
                    )
                }
            }

        /**
         * Git version 8 did not yet contain the seafarer_messages table. A
         * direct migration protects installed v8 releases that skip local v9.
         */
        val MIGRATION_8_10 =
            object : Migration(8, 10) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `seafarer_messages` (
                            `id` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `area` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `content` TEXT NOT NULL,
                            `publishedAt` INTEGER NOT NULL,
                            `expiresAt` INTEGER,
                            `source` TEXT NOT NULL,
                            `isRead` INTEGER NOT NULL,
                            `isArchived` INTEGER NOT NULL,
                            `bfsNumber` TEXT NOT NULL,
                            `latitude` REAL,
                            `longitude` REAL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent(),
                    )
                    MIGRATION_9_10.migrate(db)
                }
            }
    }
}
