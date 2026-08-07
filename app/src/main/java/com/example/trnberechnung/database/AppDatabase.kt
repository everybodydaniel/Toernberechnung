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
        SeafarerMessageEntity::class,
        MaritimeNoticeSyncEntity::class,
        NautiConversationEntity::class,
        NautiMessageEntity::class,
        ActiveVoyageEntity::class,
        VoyageBreadcrumbEntity::class,
    ],
    version = 13,
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
    abstract fun maritimeNoticeSyncDao(): MaritimeNoticeSyncDao
    abstract fun nautiDao(): NautiDao
    abstract fun activeVoyageDao(): ActiveVoyageDao

    companion object {
        val MIGRATION_12_13 =
            object : Migration(12, 13) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    // Create new table
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `planner_events_new` (
                            `id` TEXT NOT NULL,
                            `startDate` TEXT NOT NULL,
                            `endDate` TEXT NOT NULL,
                            `title` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `startTime` TEXT,
                            `endTime` TEXT,
                            `location` TEXT,
                            `category` TEXT NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                    // Copy data from old table, mapping 'date' to 'startDate' and 'endDate'
                    db.execSQL(
                        """
                        INSERT INTO `planner_events_new` (id, startDate, endDate, title, description, startTime, endTime, location, category)
                        SELECT id, date, date, title, description, startTime, endTime, location, category FROM `planner_events`
                        """.trimIndent()
                    )
                    // Remove old table
                    db.execSQL("DROP TABLE `planner_events`")
                    // Rename new table to old table name
                    db.execSQL("ALTER TABLE `planner_events_new` RENAME TO `planner_events`")
                }
            }

        val MIGRATION_11_12 =
            object : Migration(11, 12) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE `planner_events` ADD COLUMN `startTime` TEXT")
                    db.execSQL("ALTER TABLE `planner_events` ADD COLUMN `endTime` TEXT")
                    db.execSQL("ALTER TABLE `planner_events` ADD COLUMN `location` TEXT")
                    db.execSQL(
                        "ALTER TABLE `planner_events` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'Allgemein'",
                    )
                }
            }
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

        val MIGRATION_10_11 =
            object : Migration(10, 11) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    migrateSeafarerMessagesToRevisionSafeSchema(db)
                    createMaritimeNoticeSyncTable(db)
                    createNautiTables(db)
                    createActiveVoyageTables(db)
                    addLogbookVoyageColumns(db)
                }
            }

        private fun migrateSeafarerMessagesToRevisionSafeSchema(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `seafarer_messages_v11` (
                    `id` TEXT NOT NULL,
                    `bfsNumber` TEXT NOT NULL,
                    `isTemporary` INTEGER NOT NULL,
                    `publisher` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `regionPath` TEXT NOT NULL,
                    `location` TEXT,
                    `body` TEXT NOT NULL,
                    `publishedAt` INTEGER,
                    `validFrom` INTEGER,
                    `validUntil` INTEGER,
                    `publicationState` TEXT NOT NULL,
                    `revision` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `sourceUrl` TEXT,
                    `chartReferencesJson` TEXT NOT NULL,
                    `coordinatesJson` TEXT NOT NULL,
                    `previousNoticesJson` TEXT NOT NULL,
                    `parseStatus` TEXT NOT NULL,
                    `readRevision` INTEGER NOT NULL,
                    `detailRevision` INTEGER NOT NULL,
                    `detailFetchedAt` INTEGER,
                    `cachedAt` INTEGER NOT NULL,
                    `locallyArchived` INTEGER NOT NULL,
                    `latitude` REAL,
                    `longitude` REAL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO `seafarer_messages_v11` (
                    id, bfsNumber, isTemporary, publisher, title, regionPath,
                    location, body, publishedAt, validFrom, validUntil,
                    publicationState, revision, updatedAt, sourceUrl,
                    chartReferencesJson, coordinatesJson, previousNoticesJson,
                    parseStatus, readRevision, detailRevision, detailFetchedAt,
                    cachedAt, locallyArchived, latitude, longitude
                )
                SELECT
                    id,
                    bfsNumber,
                    CASE WHEN bfsNumber LIKE '%(T)%' THEN 1 ELSE 0 END,
                    source,
                    title,
                    area,
                    NULL,
                    content,
                    publishedAt,
                    publishedAt,
                    expiresAt,
                    CASE WHEN isArchived = 1 THEN 'expired' ELSE 'current' END,
                    1,
                    publishedAt,
                    NULL,
                    '[]',
                    CASE
                        WHEN latitude IS NOT NULL AND longitude IS NOT NULL
                        THEN '[{"latitude":' || latitude ||
                             ',"longitude":' || longitude || ',"label":null}]'
                        ELSE '[]'
                    END,
                    '[]',
                    'partial',
                    CASE WHEN isRead = 1 THEN 1 ELSE 0 END,
                    CASE WHEN length(trim(content)) > 0 THEN 1 ELSE 0 END,
                    CASE WHEN length(trim(content)) > 0 THEN publishedAt ELSE NULL END,
                    publishedAt,
                    isArchived,
                    latitude,
                    longitude
                FROM `seafarer_messages`
                """.trimIndent(),
            )
            db.execSQL("DROP TABLE `seafarer_messages`")
            db.execSQL(
                "ALTER TABLE `seafarer_messages_v11` RENAME TO `seafarer_messages`",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_seafarer_messages_updatedAt` " +
                    "ON `seafarer_messages` (`updatedAt`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS " +
                    "`index_seafarer_messages_publicationState_validUntil` " +
                    "ON `seafarer_messages` (`publicationState`, `validUntil`)",
            )
        }

        private fun createMaritimeNoticeSyncTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `maritime_notice_sync` (
                    `id` INTEGER NOT NULL,
                    `fetchedAt` INTEGER NOT NULL,
                    `lastIngestedAt` INTEGER,
                    `etag` TEXT,
                    `isStale` INTEGER NOT NULL,
                    `lastError` TEXT,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }

        private fun createNautiTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `nauti_conversations` (
                    `ownerId` TEXT NOT NULL,
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `lastMessageAt` INTEGER,
                    `isPinned` INTEGER NOT NULL,
                    `draft` TEXT NOT NULL,
                    PRIMARY KEY(`ownerId`, `id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_nauti_conversations_ownerId` " +
                    "ON `nauti_conversations` (`ownerId`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS " +
                    "`index_nauti_conversations_ownerId_isPinned_updatedAt` " +
                    "ON `nauti_conversations` (`ownerId`, `isPinned`, `updatedAt`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `nauti_messages` (
                    `ownerId` TEXT NOT NULL,
                    `id` TEXT NOT NULL,
                    `conversationId` TEXT NOT NULL,
                    `role` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `actionType` TEXT,
                    `actionPayloadJson` TEXT,
                    `requiresConfirmation` INTEGER NOT NULL,
                    `actionState` TEXT,
                    `isError` INTEGER NOT NULL,
                    PRIMARY KEY(`ownerId`, `id`),
                    FOREIGN KEY(`ownerId`, `conversationId`)
                        REFERENCES `nauti_conversations`(`ownerId`, `id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS " +
                    "`index_nauti_messages_ownerId_conversationId_createdAt` " +
                    "ON `nauti_messages` (`ownerId`, `conversationId`, `createdAt`)",
            )
        }

        private fun createActiveVoyageTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `active_voyages` (
                    `ownerId` TEXT NOT NULL,
                    `id` TEXT NOT NULL,
                    `routeId` TEXT NOT NULL,
                    `routeDescription` TEXT NOT NULL,
                    `startHarbourId` TEXT NOT NULL,
                    `destinationHarbourId` TEXT NOT NULL,
                    `intermediateHarbourIdsJson` TEXT NOT NULL,
                    `routeCoordinatesJson` TEXT NOT NULL,
                    `waypointCoordinatesJson` TEXT NOT NULL,
                    `plannedDepartureAt` INTEGER NOT NULL,
                    `plannedSpeedKnots` REAL NOT NULL,
                    `routeStatus` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `startedAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `endedAt` INTEGER,
                    `nextWaypointIndex` INTEGER NOT NULL,
                    `distanceMeters` REAL NOT NULL,
                    `maxSogKnots` REAL NOT NULL,
                    `sogSampleSum` REAL NOT NULL,
                    `sogSampleCount` INTEGER NOT NULL,
                    PRIMARY KEY(`ownerId`, `id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_active_voyages_ownerId_status` " +
                    "ON `active_voyages` (`ownerId`, `status`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `voyage_breadcrumbs` (
                    `ownerId` TEXT NOT NULL,
                    `voyageId` TEXT NOT NULL,
                    `sequence` INTEGER NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `latitude` REAL NOT NULL,
                    `longitude` REAL NOT NULL,
                    `accuracyMeters` REAL NOT NULL,
                    `speedKnots` REAL NOT NULL,
                    `courseDegrees` REAL,
                    PRIMARY KEY(`ownerId`, `voyageId`, `sequence`),
                    FOREIGN KEY(`ownerId`, `voyageId`)
                        REFERENCES `active_voyages`(`ownerId`, `id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS " +
                    "`index_voyage_breadcrumbs_ownerId_voyageId_timestamp` " +
                    "ON `voyage_breadcrumbs` (`ownerId`, `voyageId`, `timestamp`)",
            )
        }

        private fun addLogbookVoyageColumns(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `logbook_entries` ADD COLUMN `voyageId` TEXT")
            db.execSQL("ALTER TABLE `logbook_entries` ADD COLUMN `startedAt` INTEGER")
            db.execSQL("ALTER TABLE `logbook_entries` ADD COLUMN `endedAt` INTEGER")
            db.execSQL(
                "ALTER TABLE `logbook_entries` ADD COLUMN `actualDistanceMeters` REAL",
            )
            db.execSQL(
                "ALTER TABLE `logbook_entries` ADD COLUMN `averageSogKnots` REAL",
            )
            db.execSQL("ALTER TABLE `logbook_entries` ADD COLUMN `maxSogKnots` REAL")
            db.execSQL("ALTER TABLE `logbook_entries` ADD COLUMN `gpsTrackJson` TEXT")
        }
    }
}
