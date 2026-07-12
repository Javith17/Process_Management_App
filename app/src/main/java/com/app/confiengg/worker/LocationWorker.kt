package com.app.confiengg.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.app.confiengg.R
import com.app.confiengg.data.api.RetrofitClient
import com.app.confiengg.data.pref.SessionManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class LocationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val sessionManager = SessionManager(applicationContext)

        // 1. Check if user is checked in
        if (!sessionManager.isCheckedInState()) {
            Log.d("LocationWorker", "User not checked in. Stopping rescheduling.")
            return Result.success()
        }

        // 2. Check permissions
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("LocationWorker", "Location permission missing")
            return Result.failure()
        }

        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
            val location = Tasks.await(fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null))

            if (location != null) {
                Log.d("LocationWorker", "Location captured: ${location.latitude}, ${location.longitude}")
                checkPerimeterAndAlert(location, sessionManager)
            }
        } catch (e: Exception) {
            Log.e("LocationWorker", "Error in LocationWorker", e)
        } finally {
            // 3. Reschedule itself after 15 minutes if still checked in
            if (sessionManager.isCheckedInState()) {
                val nextWork = OneTimeWorkRequestBuilder<LocationWorker>()
                    .setInitialDelay(15, TimeUnit.MINUTES)
                    .addTag("LocationTracking")
                    .build()
                
                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    "LocationTrackingWork",
                    ExistingWorkPolicy.REPLACE,
                    nextWork
                )
                Log.d("LocationWorker", "Next work scheduled in 15 minutes")
            }
        }
        
        return Result.success()
    }

    private suspend fun checkPerimeterAndAlert(currentLocation: Location, sessionManager: SessionManager) {
        val officeLat = sessionManager.getOfficeLat()?.toDoubleOrNull()
        val officeLong = sessionManager.getOfficeLong()?.toDoubleOrNull()
        val perimeter = sessionManager.getOfficePerimeter().toFloat()

        if (officeLat != null && officeLong != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                officeLat, officeLong,
                results
            )
            val distanceInMeters = results[0]

            if (distanceInMeters > perimeter) {
                Log.d("LocationWorker", "User outside perimeter: $distanceInMeters meters away")
                
                // Send API Alert
                sendLocationAlert(currentLocation, sessionManager)
                
                // Show Local Notification
                showNotification("Location Alert", "You are outside the office perimeter.")
            } else {
                Log.d("LocationWorker", "User inside perimeter: $distanceInMeters meters away")
            }
        }
    }

    private suspend fun sendLocationAlert(location: Location, sessionManager: SessionManager) {
        try {
            val userId = sessionManager.getUserId() ?: return
            val apiService = RetrofitClient.getClient(applicationContext)
            
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            val now = Date()
            val currentDateTime = dateTimeFormat.format(now)
            val currentDate = dateFormat.format(now)

            val params = mapOf(
                "user_id" to userId,
                "attendance_date" to currentDate,
                "current_time" to currentDateTime,
                "location_detail" to "${location.latitude}, ${location.longitude}"
            )

            val response = apiService.sendLocationAlert(params)
            if (response.isSuccessful) {
                Log.d("LocationWorker", "Location alert sent successfully")
            } else {
                Log.e("LocationWorker", "Failed to send location alert: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("LocationWorker", "Error sending location alert", e)
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "location_alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Location Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.app_logo) // Ensure this exists or use a default
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
