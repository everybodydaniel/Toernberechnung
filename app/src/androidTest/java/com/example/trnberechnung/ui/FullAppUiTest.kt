package com.example.trnberechnung.ui

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.trnberechnung.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Vollständiger UI-Test der App (End-to-End).
 * Dieser Test startet die MainActivity und navigiert durch die verschiedenen Bereiche.
 */
class FullAppUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetOnboardingState() {
        composeTestRule.activity
            .getSharedPreferences("onboarding_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    @Test
    fun testFullAppNavigationFlow() {
        // A fresh installation must complete all onboarding pages and accept the
        // nautical disclaimer before entering the main application.
        repeat(2) {
            composeTestRule.onNodeWithTag("onboarding_continue")
                .assertIsEnabled()
                .performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.onNodeWithTag("onboarding_continue").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("onboarding_disclaimer_checkbox")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.onNodeWithTag("onboarding_continue")
            .assertIsEnabled()
            .performClick()

        // 1. Start auf der Karte (MapRoute ist Start-Destination)
        // Wir warten kurz, bis die Karte geladen ist (Semantics check)
        composeTestRule.onNodeWithTag("nav_map_route").assertIsSelected()

        // The completed state must survive the next Activity start.
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("onboarding_screen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("nav_map_route").assertIsSelected()

        // 2. Wechsel zu Wetter
        composeTestRule.onNodeWithTag("nav_weather").performClick()
        composeTestRule.onNodeWithTag("nav_weather").assertIsSelected()
        // Wir suchen per Tag, um Verwechslung mit der Nav-Bar zu vermeiden
        composeTestRule.onNodeWithTag("screen_header_weather").assertExists()

        // 3. Wechsel zu Gezeiten
        composeTestRule.onNodeWithTag("nav_tides").performClick()
        composeTestRule.onNodeWithTag("nav_tides").assertIsSelected()
        composeTestRule.onNodeWithTag("screen_header_tides").assertExists()

        // 4. Wechsel zu Crew
        composeTestRule.onNodeWithTag("nav_crew").performClick()
        composeTestRule.onNodeWithTag("nav_crew").assertIsSelected()
        composeTestRule.onNodeWithTag("screen_header_crew").assertExists()

        // 5. Wechsel zu Logbuch
        composeTestRule.onNodeWithTag("nav_logbook").performClick()
        composeTestRule.onNodeWithTag("nav_logbook").assertIsSelected()
        composeTestRule.onNodeWithTag("screen_header_logbook").assertExists()

        // 6. Navigation zu den Einstellungen (Bootsprofil) via TopBar
        // Das Icon hat kein Tag, aber wir können nach Content Description suchen
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Einstellungen").performClick()

        // Wir sollten jetzt im Dashboard sein
        composeTestRule.onNodeWithTag("boat_name_input").assertExists()

        // Bootsprofil bearbeiten
        val newBoatName = "Flying Dutchman"
        composeTestRule.onNodeWithTag("boat_name_input").performTextReplacement(newBoatName)

        // Prüfen ob Headline im Dashboard aktualisiert wurde
        composeTestRule.onNodeWithTag("boat_name_headline").assertTextEquals(newBoatName)

        // 7. Zurück zur Karte via Button im Dashboard
        composeTestRule.onNodeWithTag("navigation_button").performScrollTo().performClick()

        // Prüfen ob wir wieder auf der Karte sind
        composeTestRule.onNodeWithTag("nav_map_route").assertIsSelected()
    }
}
