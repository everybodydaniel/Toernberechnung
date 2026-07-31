package com.example.trnberechnung.ui

/**
 * Keeps the global refresh button scoped to the currently visible tab. The
 * coordinator deliberately owns no UI state so a refresh never resets a plan
 * or changes tabs.
 */
class AppRefreshCoordinator(
    private val refreshMap: () -> Unit,
    private val refreshWeather: () -> Unit,
    private val refreshCrewspace: () -> Unit,
    private val refreshLogbook: () -> Unit,
) {
    fun refresh(route: String?) {
        when (route) {
            Screen.MapRoute.route -> refreshMap()
            Screen.Revier.route -> refreshWeather()
            Screen.Crew.route -> refreshCrewspace()
            Screen.Logbook.route -> refreshLogbook()
        }
    }
}
