package com.example.trnberechnung.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CrewMemberDao {
    @Query("SELECT * FROM crew_members ORDER BY id ASC")
    fun getAllCrew(): Flow<List<CrewMember>>

    @Query("SELECT * FROM crew_members WHERE isOnBoard = 1 ORDER BY id ASC")
    fun getOnBoardCrew(): Flow<List<CrewMember>>

    @Query("SELECT * FROM crew_members WHERE skipperId = :skipperId LIMIT 1")
    suspend fun getBySkipperId(skipperId: String): CrewMember?

    @Query("SELECT COUNT(*) FROM crew_members WHERE isOnBoard = 1")
    suspend fun countOnBoard(): Int

    @Insert
    suspend fun insertCrew(member: CrewMember)

    @Update
    suspend fun updateCrew(member: CrewMember)

    @Delete
    suspend fun deleteCrew(member: CrewMember)
}
