package com.example.trnberechnung.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rollen, die ein Crewmitglied an Bord einnehmen kann.
 */
enum class CrewRole(val label: String) {
    SKIPPER("Skipper"),
    CO_SKIPPER("Co-Skipper"),
    NAVIGATION("Navigation"),
    STEUERMANN("Steuermann"),
    MATROSE("Matrose"),
    KOCH("Koch"),
    FUNKER("Funker"),
    BOOTSMANN("Bootsmann"),
    SONSTIGES("Sonstiges");

    companion object {
        fun fromLabel(label: String): CrewRole =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
                ?: SONSTIGES
    }
}

@Entity(tableName = "crew_members")
data class CrewMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rank: String,            // Legacy-Feld, wird weiterhin gespeichert
    val isOnBoard: Boolean,
    val medicalNote: String,
    val emergencyPhone: String,
    // ── Neue Felder für Crewspace ──
    val skipperId: String = "",          // Eindeutige Skipper-ID für Crewspace-Verknüpfung
    val role: String = "",               // CrewRole.name – z.B. "SKIPPER", "CO_SKIPPER"
    val emergencyContact: String = "",   // Name des Notfallkontakts
    val phone: String = "",              // Eigene Telefonnummer des Crewmitglieds
    val medicalNotes: String = ""        // Erweiterte medizinische Hinweise
) {
    /** Hilfsfunktion: role als Enum */
    val crewRole: CrewRole
        get() = if (role.isNotBlank()) {
            try { CrewRole.valueOf(role) } catch (_: Exception) { CrewRole.fromLabel(rank) }
        } else {
            CrewRole.fromLabel(rank)
        }
}
