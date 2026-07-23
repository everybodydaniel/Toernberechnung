package com.example.trnberechnung.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "planner_events")
data class PlannerEventEntity(
    @PrimaryKey val id: String,
    val date: LocalDate,
    val title: String,
    val description: String
)
