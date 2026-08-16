package com.example.trnberechnung.model

import android.content.Context
import android.content.SharedPreferences

/**
 * Device-local app settings.
 *
 * What is left of the former `AuthRepository` after Crewspace was removed: the theme choice was the
 * only preference in it that had nothing to do with the account. The `auth_prefs` file name and the
 * `is_dark_mode` key are kept verbatim so an existing installation does not silently flip back to
 * light mode on update.
 */
class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    var isDarkMode: Boolean
        get() = prefs.getBoolean("is_dark_mode", false)
        set(value) = prefs.edit().putBoolean("is_dark_mode", value).apply()
}
