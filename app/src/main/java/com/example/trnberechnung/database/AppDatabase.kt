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

@TypeConverters(Converters::class)

@Database(
    entities = [
        TideEntity::class,
        LogbookEntry::class,
        CrewMember::class,
        ChecklistItem::class,
        PlannerEventEntity::class,
        NautiConversationEntity::class,
        NautiMessageEntity::class,
        ActiveVoyageEntity::class,
        VoyageBreadcrumbEntity::class,
    ],
    version = 15,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tideDao(): TideDao
    abstract fun logbookDao(): LogbookDao
    abstract fun crewMemberDao(): CrewMemberDao
    abstract fun checklistDao(): ChecklistDao
    abstract fun plannerEventDao(): PlannerEventDao
    abstract fun nautiDao(): NautiDao
    abstract fun activeVoyageDao(): ActiveVoyageDao
}
