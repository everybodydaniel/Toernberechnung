package com.example.trnberechnung.model

import android.content.Context

/** Stores the one-time introduction state independently from the boat profile. */
class OnboardingPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val isCompleted: Boolean
        get() = preferences.getBoolean(KEY_COMPLETED, false)

    fun markCompleted() {
        preferences.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "onboarding_preferences"
        private const val KEY_COMPLETED = "onboarding_completed"
    }
}
