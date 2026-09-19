package com.campussync.app

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlin.system.exitProcess

class CampusSyncApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 🚨 GLOBAL CRASH CATCHER 🚨
        // Ye code app mein aane wale KISI BHI crash ko pakad lega
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            
            // 1. Error ko safe jagah print karo (Logcat ke liye)
            throwable.printStackTrace()

            // 2. User ko ek safe message dikhao bina app force-close kiye
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    applicationContext, 
                    "Oops! Something went wrong, but we kept the app running.", 
                    Toast.LENGTH_LONG
                ).show()
            }

            // 3. (Optional) App ko safely restart karo yahan
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)

            // System ki default crash window ko rokne ke liye process kill karein (Safe Restart)
            exitProcess(1)
        }
    }
}
