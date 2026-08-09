package com.example.trnberechnung.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigration10To11Test {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrationPreservesLogbookAndCreatesNewStores() {
        helper.createDatabase(TEST_DATABASE, 10).apply {
            execSQL(
                """
                INSERT INTO logbook_entries (
                    date, routeDesc, distance, duration, status, details
                ) VALUES ('2026-07-29', 'Emden - Juist', '25.5 nm', '4h 15m', 'ok', '')
                """.trimIndent(),
            )
            close()
        }

        val database =
            helper.runMigrationsAndValidate(
                TEST_DATABASE,
                11,
                true,
                AppDatabase.MIGRATION_10_11,
            )

        database
            .query(
                """
                SELECT voyageId, actualDistanceMeters, gpsTrackJson
                FROM logbook_entries
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(0))
                assertTrue(cursor.isNull(1))
                assertTrue(cursor.isNull(2))
            }

        listOf(
            "nauti_conversations",
            "nauti_messages",
            "active_voyages",
            "voyage_breadcrumbs",
        ).forEach { table ->
            database
                .query(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = ?",
                    arrayOf(table),
                ).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertFalse("$table fehlt", cursor.getInt(0) == 0)
                }
        }
        database.close()
    }

    companion object {
        private const val TEST_DATABASE = "migration-10-11"
    }
}
