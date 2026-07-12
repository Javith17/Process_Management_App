package com.app.confiengg.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.app.confiengg.data.api.ApiService
import com.app.confiengg.data.api.RetrofitClient
import com.app.confiengg.data.model.AttendanceUpdateRequest
import com.app.confiengg.data.pref.SessionManager
import com.app.confiengg.ui.activities.AttachmentsActivity
import com.app.confiengg.ui.theme.PrimaryGreen
import com.app.confiengg.service.TrackingService
import com.app.confiengg.worker.LocationWorker
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("MissingPermission")
@Composable
fun AttendanceScreen(onNavigateToHistory: () -> Unit) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var shiftInTime by remember { mutableStateOf<Long?>(null) }
    var shiftOutTime by remember { mutableStateOf<Long?>(null) }
    var totalBreakMillisFromApi by remember { mutableLongStateOf(0L) }
    var currentBreakStartTime by remember { mutableStateOf<Long?>(null) }

    var isShiftInEnabled by remember { mutableStateOf(true) }
    var isBreakInEnabled by remember { mutableStateOf(false) }
    var isBreakOutEnabled by remember { mutableStateOf(false) }
    var isShiftOutEnabled by remember { mutableStateOf(false) }

    var isOnBreak by remember { mutableStateOf(false) }
    var isCheckedIn by remember { mutableStateOf(false) }
    var isCheckedOut by remember { mutableStateOf(false) }
    var isLeave by remember { mutableStateOf(false) }
    var activeAttendanceDate by remember { mutableStateOf<String?>(null) }
    var attachmentLinks by remember { mutableStateOf<List<String>>(emptyList()) }

    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var currentDate by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val apiService = remember { RetrofitClient.getClient(context) }

    val displayTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("dd MMM yyyy, EEEE", Locale.getDefault())

    fun loadDailyAttendance() {
        coroutineScope.launch {
            isLoading = true
            try {
                val userId = sessionManager.getUserId() ?: return@launch
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val response = apiService.getDailyAttendance(mapOf(
                    "user_id" to userId,
                    "attendance_date" to todayDate
                ))

                if (response.isSuccessful && response.body()?.data != null) {
                    val attendanceData = response.body()!!.data!!
                    activeAttendanceDate = todayDate // Use local today's date in yyyy-MM-dd format
                    isLeave = attendanceData.is_leave ?: false

                    if (isLeave && attendanceData.check_in_time == null) {
                        isLoading = false
                        return@launch
                    }

                    shiftInTime = attendanceData.check_in_time?.toLongOrNull()
                    shiftOutTime = attendanceData.check_out_time?.toLongOrNull()
                    totalBreakMillisFromApi = attendanceData.total_break_hrs?.toLongOrNull() ?: 0L
                    isOnBreak = attendanceData.is_break ?: false
                    attachmentLinks = attendanceData.attachment_links ?: emptyList()

                    if (isOnBreak) {
                        currentBreakStartTime = attendanceData.break_details?.lastOrNull { it.break_out.isNullOrEmpty() }?.break_in?.toLongOrNull()
                            ?: attendanceData.break_details?.lastOrNull()?.break_in?.toLongOrNull()
                    }

                    if (shiftInTime != null) {
                        isCheckedIn = true
                        isShiftInEnabled = false
                        if (shiftOutTime == null) {
                            isCheckedOut = false
                            isShiftOutEnabled = !isOnBreak
                            isBreakInEnabled = !isOnBreak
                            isBreakOutEnabled = isOnBreak
                        } else {
                            isCheckedOut = true
                            isShiftOutEnabled = false
                            isBreakInEnabled = false
                            isBreakOutEnabled = false
                        }
                    } else {
                        isCheckedIn = false
                        isShiftInEnabled = true
                        isShiftOutEnabled = false
                        isBreakInEnabled = false
                        isBreakOutEnabled = false
                    }
                } else {
                    activeAttendanceDate = todayDate
                    isLeave = false
                    isCheckedIn = false
                    isShiftInEnabled = true
                    isShiftOutEnabled = false
                    isBreakInEnabled = false
                    isBreakOutEnabled = false
                    shiftInTime = null
                    shiftOutTime = null
                    isOnBreak = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isShiftInEnabled = true
                isCheckedIn = false
            } finally {
                isLoading = false
            }
        }
    }

    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var cameraActionType by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null && cameraActionType != null) {
            val type = cameraActionType!!
            val date = activeAttendanceDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            coroutineScope.launch {
                handleAttendanceWithImage(
                    context = context,
                    type = type,
                    attendanceDate = date,
                    bitmap = bitmap,
                    fusedLocationClient = fusedLocationClient,
                    sessionManager = sessionManager,
                    apiService = apiService,
                    onSuccess = {
                        loadDailyAttendance()
                    },
                    onError = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                    setLoading = { isLoading = it }
                )
            }
        }
    }

    val settingResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingAction?.invoke()
            pendingAction = null
        } else {
            Toast.makeText(context, "Location must be enabled to proceed", Toast.LENGTH_SHORT).show()
            pendingAction = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (locationGranted) {
            pendingAction?.invoke()
            pendingAction = null
        } else {
            Toast.makeText(context, "Location permission is required for attendance", Toast.LENGTH_SHORT).show()
            pendingAction = null
        }
    }

    fun hasPermissions(context: Context, requireCamera: Boolean): Boolean {
        val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        return if (requireCamera) hasLocation && hasCamera else hasLocation
    }

    fun checkGPSAndProceed(action: () -> Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(context)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            action()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    pendingAction = action
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution.intentSender).build()
                    settingResultLauncher.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    pendingAction = null
                }
            } else {
                Toast.makeText(context, "Please enable location services in settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleAttendanceAction(requireCamera: Boolean = false, action: () -> Unit) {
        if (!hasPermissions(context, requireCamera)) {
            pendingAction = { checkGPSAndProceed(action) }
            val permissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (requireCamera) {
                permissions.add(Manifest.permission.CAMERA)
            }
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            checkGPSAndProceed(action)
        }
    }

    fun formatMillisToHMS(millis: Long): String {
        if (millis < 0) return "00:00:00"
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatMillisToHM(millis: Long): String {
        if (millis < 0) return "00:00"
        val minutes = (millis / (1000 * 60)) % 60
        val hours = (millis / (1000 * 60 * 60))
        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
    }



    LaunchedEffect(Unit) {
        loadDailyAttendance()
        var lastCheckedDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            if (currentDay != lastCheckedDay) {
                lastCheckedDay = currentDay
                loadDailyAttendance()
            }
            currentDate = dateFormat.format(Date(currentTimeMillis))
            delay(1000)
        }
    }

    val effectiveBreakMillis = if (isOnBreak && currentBreakStartTime != null) {
        totalBreakMillisFromApi + (currentTimeMillis - currentBreakStartTime!!)
    } else {
        totalBreakMillisFromApi
    }

    val workingMillis = when {
        isCheckedOut && shiftInTime != null && shiftOutTime != null -> {
            (shiftOutTime!! - shiftInTime!!) - totalBreakMillisFromApi
        }
        isCheckedIn && shiftInTime != null -> {
            (currentTimeMillis - shiftInTime!!) - effectiveBreakMillis
        }
        else -> 0L
    }

    val topTimerMillis = if (isCheckedIn && !isCheckedOut) {
        if (isOnBreak) {
            (currentBreakStartTime ?: currentTimeMillis) - shiftInTime!! - totalBreakMillisFromApi
        } else {
            (currentTimeMillis - shiftInTime!!) - totalBreakMillisFromApi
        }
    } else if (isCheckedOut && shiftInTime != null && shiftOutTime != null) {
        (shiftOutTime!! - shiftInTime!!) - totalBreakMillisFromApi
    } else {
        0L
    }

    if (isLeave && !isCheckedIn) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FA)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EventBusy,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "You are on leave today",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enjoy your day off! Attendance actions are disabled.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Text(
                    text = formatMillisToHMS(topTimerMillis),
                    fontSize = 40.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentDate,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 90.dp)
                    .background(Color.White, CircleShape)
                    .border(8.dp, Color(0xFFE0E0E0), CircleShape)
                    .clickable {
                        if (isLoading) return@clickable

                        if (isShiftInEnabled) {
                            handleAttendanceAction(requireCamera = true) {
                                cameraActionType = "check-in"
                                cameraLauncher.launch(null)
                            }
                        } else if (isShiftOutEnabled) {
                            handleAttendanceAction(requireCamera = false) {
                                coroutineScope.launch {
                                    performAttendanceUpdate(
                                        context = context,
                                        type = "check-out",
                                        attendanceDate = activeAttendanceDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                        fusedLocationClient = fusedLocationClient,
                                        sessionManager = sessionManager,
                                        apiService = apiService,
                                        onSuccess = {
                                            loadDailyAttendance()
                                        },
                                        onError = { message ->
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        },
                                        setLoading = { isLoading = it }
                                    )
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val icon = when {
                            isCheckedOut -> Icons.Default.CheckCircle
                            isShiftInEnabled -> Icons.Default.TouchApp
                            else -> Icons.Default.FrontHand
                        }
                        val label = when {
                            isCheckedOut -> "Finished"
                            isShiftInEnabled -> "Check In"
                            else -> "Check Out"
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = if (isShiftInEnabled || isShiftOutEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold,
                            color = if (isShiftInEnabled || isShiftOutEnabled) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    handleAttendanceAction {
                        coroutineScope.launch {
                            performAttendanceUpdate(
                                context = context,
                                type = "break-in",
                                attendanceDate = activeAttendanceDate ?: "",
                                fusedLocationClient = fusedLocationClient,
                                sessionManager = sessionManager,
                                apiService = apiService,
                                onSuccess = {
                                    loadDailyAttendance()
                                },
                                onError = { message ->
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                },
                                setLoading = { isLoading = it }
                            )
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                enabled = isBreakInEnabled && !isLoading
            ) {
                Icon(Icons.Default.PauseCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Break")
            }
            Button(
                onClick = {
                    handleAttendanceAction {
                        coroutineScope.launch {
                            performAttendanceUpdate(
                                context = context,
                                type = "break-out",
                                attendanceDate = activeAttendanceDate ?: "",
                                fusedLocationClient = fusedLocationClient,
                                sessionManager = sessionManager,
                                apiService = apiService,
                                onSuccess = {
                                    loadDailyAttendance()
                                },
                                onError = { message ->
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                },
                                setLoading = { isLoading = it }
                            )
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                enabled = isBreakOutEnabled && !isLoading
            ) {
                Icon(Icons.Default.PlayCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("End Break")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Today Activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (attachmentLinks.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            val intent = Intent(context, AttachmentsActivity::class.java).apply {
                                putStringArrayListExtra("links", ArrayList(attachmentLinks))
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(Icons.Default.Attachment, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("(${attachmentLinks.size})")
                    }
                }

                IconButton(onClick = onNavigateToHistory) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActivityCard(
                    modifier = Modifier.weight(1f),
                    title = "Check In",
                    time = if (shiftInTime != null) displayTimeFormat.format(Date(shiftInTime!!)) else "--:--",
                    icon = Icons.AutoMirrored.Filled.Login,
                    iconColor = PrimaryGreen
                )
                ActivityCard(
                    modifier = Modifier.weight(1f),
                    title = "Check Out",
                    time = if (shiftOutTime != null) displayTimeFormat.format(Date(shiftOutTime!!)) else "--:--",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    iconColor = PrimaryGreen
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActivityCard(
                    modifier = Modifier.weight(1f),
                    title = "Break Time",
                    time = formatMillisToHMS(effectiveBreakMillis),
                    icon = Icons.Default.Coffee,
                    iconColor = PrimaryGreen
                )
                ActivityCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Hrs",
                    time = formatMillisToHM(workingMillis),
                    icon = Icons.Default.CheckCircle,
                    iconColor = PrimaryGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@SuppressLint("MissingPermission")
suspend fun handleAttendanceWithImage(
    context: Context,
    type: String,
    attendanceDate: String,
    bitmap: Bitmap,
    fusedLocationClient: FusedLocationProviderClient,
    sessionManager: SessionManager,
    apiService: ApiService,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    setLoading: (Boolean) -> Unit
) {
    setLoading(true)
    try {
        val userId = sessionManager.getUserId() ?: throw Exception("User ID not found")
        val currentTime = System.currentTimeMillis()

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            throw Exception("Location permission not granted")
        }

        val location = withContext(Dispatchers.IO) {
            try {
                com.google.android.gms.tasks.Tasks.await(
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                )
            } catch (e: Exception) {
                null
            }
        }

        if (location == null) {
            throw Exception("Unable to retrieve location. Please ensure GPS is enabled and has a clear view of the sky.")
        }

        val locationString = "${location.latitude}, ${location.longitude}"

        val attendanceRequest = AttendanceUpdateRequest(
            user_id = userId,
            attendance_date = attendanceDate,
            type = type,
            time = currentTime,
            location = locationString
        )

        val response = apiService.updateAttendance(attendanceRequest)

        if (response.isSuccessful) {
            if (type == "check-in") {
                sessionManager.setCheckedIn(true, currentTime)
                // Start background location tracking
                val workRequest = OneTimeWorkRequestBuilder<LocationWorker>().build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "LocationTrackingWork",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
                // Start Tracking Service
                val serviceIntent = Intent(context, TrackingService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            } else if (type == "check-out") {
                sessionManager.setCheckedIn(false)
                WorkManager.getInstance(context).cancelUniqueWork("LocationTrackingWork")
                // Stop Tracking Service
                context.stopService(Intent(context, TrackingService::class.java))
            }

            withContext(Dispatchers.IO) {
                val file = File(context.cacheDir, "attendance_${type}_${currentTime}.jpg")
                val out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()

                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("files", file.name, requestFile)
                val userIdBody = userId.toRequestBody("text/plain".toMediaTypeOrNull())
                val dateBody = attendanceDate.toRequestBody("text/plain".toMediaTypeOrNull())

                val uploadResponse = apiService.uploadAttendanceImage(userIdBody, dateBody, listOf(body))

                if (uploadResponse.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    throw Exception("Image upload failed")
                }
            }
        } else {
            throw Exception("${type.replaceFirstChar { it.uppercase() }} update failed: ${response.code()}")
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            onError(e.message ?: "An unknown error occurred")
        }
    } finally {
        setLoading(false)
    }
}

@SuppressLint("MissingPermission")
suspend fun performAttendanceUpdate(
    context: Context,
    type: String,
    attendanceDate: String,
    fusedLocationClient: FusedLocationProviderClient?,
    sessionManager: SessionManager,
    apiService: ApiService,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    setLoading: (Boolean) -> Unit
) {
    setLoading(true)
    try {
        val userId = sessionManager.getUserId() ?: throw Exception("User ID not found")
        val currentTime = System.currentTimeMillis()

        var locationString = ""
        if (fusedLocationClient != null) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                val location = withContext(Dispatchers.IO) {
                    try {
                        com.google.android.gms.tasks.Tasks.await(
                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                locationString = if (location != null) "${location.latitude}, ${location.longitude}" else "0.0, 0.0"
            } else {
                locationString = "0.0, 0.0"
            }
        }

        val attendanceRequest = AttendanceUpdateRequest(
            user_id = userId,
            attendance_date = attendanceDate,
            type = type,
            time = currentTime,
            location = locationString
        )

        val response = apiService.updateAttendance(attendanceRequest)

        if (response.isSuccessful) {
            if (type == "check-out") {
                sessionManager.setCheckedIn(false)
                WorkManager.getInstance(context).cancelUniqueWork("LocationTrackingWork")
                // Stop Tracking Service
                context.stopService(Intent(context, TrackingService::class.java))
            }
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        } else {
            throw Exception("${type.replaceFirstChar { it.uppercase() }} failed: ${response.code()}")
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            onError(e.message ?: "An unknown error occurred")
        }
    } finally {
        setLoading(false)
    }
}

@Composable
fun ActivityCard(modifier: Modifier, title: String, time: String, icon: ImageVector, iconColor: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, fontSize = 12.sp, color = Color.Gray)
                Text(text = time, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
