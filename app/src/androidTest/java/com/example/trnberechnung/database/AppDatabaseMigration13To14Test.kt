package com.example.trnberechnung.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigration13To14Test {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrationRemovesNoticeTablesAndPreservesAppData() {
        helper.createDatabase(TEST_DATABASE, 13).apply {
            execSQL(
                """
                INSERT INTO tide (
                    id, area, region, latitude, longitude, waterLevel,
                    meanHighWater, meanLowWater, forecastTimestamp
                ) VALUES (1, 'Norderney', 'Nordsee', 53.7, 7.15, 1.2, 2.1, 0.3, '2026-08-09')
                """.trimIndent(),
            )
            close()
        }

        val database =
            helper.runMigrationsAndValidate(
                TEST_DATABASE,
                14,
                true,
                AppDatabase.MIGRATION_13_14,
            )

        assertFalse(database.hasTable("seafarer_messages"))
        assertFalse(database.hasTable("maritime_notice_sync"))
        database.query("SELECT area, waterLevel FROM tide WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Norderney", cursor.getString(0))
            assertEquals(1.2, cursor.getDouble(1), 0.0)
        }
        database.close()
    }

    private fun SupportSQLiteDatabase.hasTable(tableName: String): Boolean =
        query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(tableName),
        ).use { cursor ->
            cursor.moveToFirst() && cursor.getInt(0) > 0
        }

    companion object {
        private const val TEST_DATABASE = "migration-13-14"
    }
}
