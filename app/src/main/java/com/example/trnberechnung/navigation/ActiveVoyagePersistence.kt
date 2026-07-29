package com.example.trnberechnung.navigation

/**
 * Persistence boundary for live navigation.
 *
 * A Room implementation can map these domain objects to database entities
 * without introducing Room annotations into the navigation core.
 */
interface ActiveVoyagePersistence {
    suspend fun loadActiveVoyage(): ActiveVoyageSession?

    suspend fun saveActiveVoyage(session: ActiveVoyageSession)

    /**
     * Stores the completed logbook voyage and removes the active session.
     * Implementations should perform both writes in one transaction.
     */
    suspend fun finishActiveVoyage(voyage: CompletedVoyage)

    suspend fun clearActiveVoyage()
}
