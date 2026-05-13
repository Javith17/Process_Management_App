package com.app.confiengg.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import java.util.*

class LocationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!isWithinWorkingHours()) {
            Log.d("LocationWorker", "Outside working hours (08:00 - 17:00). Skipping.")
            return Result.success()
        }

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure()
        }

        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
            val location = Tasks.await(fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null))
            
            if (location != null) {
                Log.d("LocationWorker", "Location captured: ${location.latitude}, ${location.longitude}")
                // Here you would typically send this to your server via Retrofit
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("LocationWorker", "Error capturing location", e)
            Result.retry()
        }
    }

    private fun isWithinWorkingHours(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour in 8..16 // 08:00 to 16:59
    }
}
