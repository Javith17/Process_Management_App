package com.app.confiengg.service

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.app.confiengg.R
import com.app.confiengg.data.api.RetrofitClient
import com.app.confiengg.data.pref.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TrackingService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var sessionManager: SessionManager
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 30 * 60 * 1000L // 30 minutes

    private val trackingRunnable = object : Runnable {
        override fun run() {
            checkScreenTime()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createPersistentNotification())
        handler.postDelayed(trackingRunnable, checkInterval)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkScreenTime() {
        val checkInTime = sessionManager.getCheckInTime()
        if (checkInTime == 0L) return

        val currentTime = System.currentTimeMillis()
        val totalScreenTimeMinutes = getScreenTimeInMinutes(checkInTime, currentTime)

        Log.d("TrackingService", "Total screen time since check-in: $totalScreenTimeMinutes minutes")

        if (totalScreenTimeMinutes > 2) {
            showLimitExceededNotification()
            sendScreenAlert(totalScreenTimeMinutes)
        }
    }

    private fun getScreenTimeInMinutes(startTime: Long, endTime: Long): Long {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        var totalTimeMs = 0L
        for (usageStats in stats) {
            totalTimeMs += usageStats.totalTimeInForeground
        }
        return totalTimeMs / (1000 * 60)
    }

    private fun sendScreenAlert(screenTimeMinutes: Long) {
        serviceScope.launch {
            try {
                val userId = sessionManager.getUserId() ?: return@launch
                val apiService = RetrofitClient.getClient(applicationContext)
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val params = mapOf(
                    "user_id" to userId,
                    "screen_time" to screenTimeMinutes.toString(),
                    "attendance_date" to currentDate
                )

                val response = apiService.sendScreenAlert(params)
                if (response.isSuccessful) {
                    Log.d("TrackingService", "Screen alert sent successfully")
                }
            } catch (e: Exception) {
                Log.e("TrackingService", "Error sending screen alert", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Attendance Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Screen Time Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun createPersistentNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Attendance Active")
            .setContentText("Location and screen times are being captured.")
            .setSmallIcon(R.drawable.app_logo)
            .setOngoing(true)
            .build()
    }

    private fun showLimitExceededNotification() {
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("Screen Time Alert")
            .setContentText("Screen time limit exceeds")
            .setSmallIcon(R.drawable.app_logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(trackingRunnable)
        serviceJob.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "tracking_service_channel"
        private const val ALERT_CHANNEL_ID = "screen_alert_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ALERT_NOTIFICATION_ID = 1002
    }
}
