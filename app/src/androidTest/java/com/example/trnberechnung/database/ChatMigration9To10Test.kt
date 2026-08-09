package com.example.trnberechnung.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChatMigration9To10Test {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val databaseName = "chat-migration-9-10.db"
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
        helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(databaseName)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(9) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                db.execSQL(
                                    """
                                    CREATE TABLE chat_threads (
                                        id TEXT NOT NULL PRIMARY KEY,
                                        type TEXT NOT NULL,
                                        participant1Id TEXT NOT NULL,
                                        participant1Name TEXT NOT NULL,
                                        participant2Id TEXT NOT NULL,
                                        participant2Name TEXT NOT NULL,
                                        lastMessage TEXT NOT NULL,
                                        lastMessageTimestamp INTEGER NOT NULL,
                                        unreadCount INTEGER NOT NULL
                                    )
                                    """.trimIndent(),
                                )
                                db.execSQL(
                                    """
                                    CREATE TABLE chat_messages (
                                        id TEXT NOT NULL PRIMARY KEY,
                                        threadId TEXT NOT NULL,
                                        senderId TEXT NOT NULL,
                                        senderName TEXT NOT NULL,
                                        content TEXT NOT NULL,
                                        type TEXT NOT NULL,
                                        voiceDurationSeconds INTEGER NOT NULL,
                                        timestamp INTEGER NOT NULL
                                    )
                                    """.trimIndent(),
                                )
                            }

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) = Unit
                        },
                    )
                    .build(),
            )
    }

    @After
    fun tearDown() {
        helper.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migrationScopesExistingChatAndPreservesMessage() {
        val db = helper.writableDatabase
        db.execSQL(
            """
            INSERT INTO chat_threads VALUES (
                'server-conversation', 'DIRECT', 'firebase-owner', 'Owner',
                'firebase-peer', 'Peer', 'Hallo', 1234, 2
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO chat_messages VALUES (
                'server-message', 'server-conversation', 'firebase-peer', 'Peer',
                'Hallo', 'TEXT', 0, 1234
            )
            """.trimIndent(),
        )

        AppDatabase.MIGRATION_9_10.migrate(db)

        db.query(
            "SELECT ownerId, id, serverId, deliveryState FROM chat_messages",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("firebase-owner", cursor.getString(0))
            assertEquals("server-message", cursor.getString(1))
            assertEquals("server-message", cursor.getString(2))
            assertEquals("SENT", cursor.getString(3))
        }
        db.query(
            "SELECT ownerId, afterCursor, beforeCursor FROM chat_threads",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("firebase-owner", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
        }

        // The same server ID may be cached by another signed-in account.
        db.execSQL(
            """
            INSERT INTO chat_threads (
                ownerId, id, type, participant1Id, participant1Name,
                participant2Id, participant2Name, lastMessage,
                lastMessageTimestamp, unreadCount, blocked, blockedByMe
            ) VALUES (
                'second-owner', 'server-conversation', 'DIRECT', 'second-owner', 'Two',
                'firebase-peer', 'Peer', '', 0, 0, 0, 0
            )
            """.trimIndent(),
        )
        db.query(
            "SELECT COUNT(*) FROM chat_threads WHERE id = 'server-conversation'",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2, cursor.getInt(0))
        }
    }
}
