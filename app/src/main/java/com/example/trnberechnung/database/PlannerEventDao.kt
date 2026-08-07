package com.example.trnberechnung.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerEventDao {
    @Query("SELECT * FROM planner_events ORDER BY startDate ASC")
    fun getAllEvents(): Flow<List<PlannerEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: PlannerEventEntity)

    @Delete
    suspend fun deleteEvent(event: PlannerEventEntity)
}
