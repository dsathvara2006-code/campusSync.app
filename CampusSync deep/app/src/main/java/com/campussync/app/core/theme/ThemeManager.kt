package com.campussync.app.core.theme

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit

class ThemePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    var themeMode = mutableStateOf(prefs.getString("theme_mode", "system") ?: "system")
        private set

    // Legacy compatibility: checks if theme is currently dark (either explicitly dark, or system dark)
    var isDarkMode = mutableStateOf(
        when (themeMode.value) {
            "dark" -> true
            "light" -> false
            else -> false // Fallback resolved reactively in UI
        }
    )
        private set

    fun setThemeMode(mode: String) {
        prefs.edit {
            putString("theme_mode", mode)
            putBoolean("is_dark_mode", mode == "dark")
        }
        themeMode.value = mode
        isDarkMode.value = (mode == "dark")
    }

    // Keep legacy function to avoid breaking existing settings switches
    fun setDarkMode(enabled: Boolean) {
        setThemeMode(if (enabled) "dark" else "light")
    }
}

object ThemeManager {
    lateinit var preferences: ThemePreferences

    fun init(context: Context) {
        if (!::preferences.isInitialized) {
            preferences = ThemePreferences(context.applicationContext)
        }
    }
}

