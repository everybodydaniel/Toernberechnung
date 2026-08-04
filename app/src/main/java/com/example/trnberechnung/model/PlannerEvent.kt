package com.example.trnberechnung.model

import java.time.LocalDate
import java.util.UUID

/**
 * Datenmodell für einen Planungstermin im Crewspace-Kalender.
 * Wird vorerst als In-Memory-Mock verwendet.
 */
data class PlannerEvent(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val title: String,
    val description: String = "",
    val startTime: String? = null,
    val endTime: String? = null,
    val location: String? = null,
    val category: String = "Allgemein"
)
