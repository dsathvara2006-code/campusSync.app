package com.campussync.app.core.utils

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AntiSpamManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("SecurityPrefs", Context.MODE_PRIVATE)

    fun canSubmitPayment(): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastDate = prefs.getString("last_submit_date", "")
        val attempts = prefs.getInt("daily_attempts", 0)

        if (today != lastDate) {
            // New day, reset attempts
            prefs.edit().putString("last_submit_date", today).putInt("daily_attempts", 1).apply()
            return true
        }

        if (attempts >= 100) {
            // Daily limit exceeded (Block the Hacker)
            return false 
        }

        // Increment attempts
        prefs.edit().putInt("daily_attempts", attempts + 1).apply()
        return true
    }
}
