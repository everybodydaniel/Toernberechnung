package com.example.trnberechnung.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logbook_entries")
data class LogbookEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val routeDesc: String,
    val distance: String,
    val duration: String,
    val status: String,
    val details: String,
    val voyageId: String? = null,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val actualDistanceMeters: Double? = null,
    val averageSogKnots: Double? = null,
    val maxSogKnots: Double? = null,
    val gpsTrackJson: String? = null,
)
