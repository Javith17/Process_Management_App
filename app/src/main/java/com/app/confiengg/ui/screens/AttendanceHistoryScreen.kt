package com.app.confiengg.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.confiengg.data.api.RetrofitClient
import com.app.confiengg.data.model.AttendanceData
import com.app.confiengg.data.model.AttendanceSummary
import com.app.confiengg.data.pref.SessionManager
import com.app.confiengg.ui.theme.PrimaryGreen
import com.app.confiengg.ui.theme.PrimaryRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val apiService = remember { RetrofitClient.getClient(context) }

    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    var summary by remember { mutableStateOf<AttendanceSummary?>(null) }
    var historyList by remember { mutableStateOf<List<AttendanceData>>(emptyList()) }
    var noOfWorkingDays by remember { mutableStateOf("0") }
    var noOfLeaveDays by remember { mutableStateOf("0") }
    var isLoading by remember { mutableStateOf(false) }

    fun loadMonthlyAttendance(calendar: Calendar) {
        coroutineScope.launch {
            isLoading = true
            try {
                val userId = sessionManager.getUserId() ?: return@launch
                val month = (calendar.get(Calendar.MONTH) + 1).toString()
                val year = calendar.get(Calendar.YEAR).toString()

                val response = apiService.getMonthlyAttendance(mapOf(
                    "user_id" to userId,
                    "attendance_month" to month,
                    "attendance_year" to year
                ))

                if (response.isSuccessful) {
                    val body = response.body()
                    summary = body?.data?.summary
                    historyList = body?.data?.attendance_list ?: emptyList()
                    noOfWorkingDays = body?.data?.no_of_working_days ?: "0"
                    noOfLeaveDays = body?.data?.no_of_leave_days ?: "0"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun loadDailyAttendance() {
        coroutineScope.launch {
            try {
                val userId = sessionManager.getUserId() ?: return@launch
                val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                apiService.getDailyAttendance(mapOf(
                    "user_id" to userId,
                    "attendance_date" to todayDate
                ))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        loadDailyAttendance()
        loadMonthlyAttendance(currentMonth)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F7FA))
        ) {
            // Month Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newMonth = currentMonth.clone() as Calendar
                    newMonth.add(Calendar.MONTH, -1)
                    currentMonth = newMonth
                    loadMonthlyAttendance(newMonth)
                }) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous Month", modifier = Modifier.size(18.dp))
                }
                Text(
                    text = monthYearFormat.format(currentMonth.time),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    val newMonth = currentMonth.clone() as Calendar
                    newMonth.add(Calendar.MONTH, 1)
                    currentMonth = newMonth
                    loadMonthlyAttendance(newMonth)
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Next Month", modifier = Modifier.size(18.dp))
                }
            }

            // Summary Grid
            val summaryItems = listOf(
                SummaryItem("Present", noOfWorkingDays, PrimaryGreen),
                SummaryItem("Absent", noOfLeaveDays, PrimaryRed),
                SummaryItem("Paid-Leave", summary?.paidLeaveCount?.toString() ?: "0", Color(0xFF9C27B0))
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard(modifier = Modifier.weight(1f), summaryItems[0])
                    SummaryCard(modifier = Modifier.weight(1f), summaryItems[1])
                    SummaryCard(modifier = Modifier.weight(1f), summaryItems[2])
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // History List
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (historyList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No attendance data available", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(historyList.size) { index ->
                        HistoryItemCard(historyList[index])
                    }
                }
            }
        }
    }
}

data class SummaryItem(val label: String, val count: String, val color: Color)

@Composable
fun SummaryCard(modifier: Modifier = Modifier, item: SummaryItem) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .background(item.color, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = item.label, fontSize = 12.sp, color = Color.Black)
            }
            Text(text = item.count, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun HistoryItemCard(data: AttendanceData) {
    val date = remember(data.date, data.attendance_date) {
        val dateStr = data.date ?: data.attendance_date
        if (dateStr.isNullOrEmpty()) Date()
        else {
            // Target format: "Thu May 07 2026 00:00:00 GMT+0530 (India Standard Time)"
            val cleanDateStr = dateStr.split(" (").first()
            val formats = listOf(
                "EEE MMM dd yyyy HH:mm:ss 'GMT'Z",
                "EEE MMM dd yyyy HH:mm:ss 'GMT'z",
                "EEE MMM dd yyyy HH:mm:ss",
                "EEE MMM dd yyyy",
                "yyyy-MM-dd",
                "dd-MM-yyyy"
            )
            var parsed: Date? = null
            for (f in formats) {
                try {
                    val sdf = SimpleDateFormat(f, Locale.US)
                    parsed = sdf.parse(cleanDateStr)
                    if (parsed != null) break
                } catch (e: Exception) {
                }
            }

            // Fallback for JS-like date strings if SimpleDateFormat fails
            if (parsed == null) {
                try {
                    val parts = cleanDateStr.split(" ")
                    if (parts.size >= 4) {
                        // "Thu May 07 2026"
                        val shortDateStr = "${parts[0]} ${parts[1]} ${parts[2]} ${parts[3]}"
                        val sdf = SimpleDateFormat("EEE MMM dd yyyy", Locale.US)
                        parsed = sdf.parse(shortDateStr)
                    }
                } catch (e: Exception) {}
            }

            parsed ?: Date()
        }
    }

    val dayFormat = remember { SimpleDateFormat("dd", Locale.getDefault()) }
    val weekdayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dayWeekdayFormat = remember { SimpleDateFormat("dd EEE", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    fun formatTime(timeStr: String?): String {
        if (timeStr.isNullOrEmpty()) return "--"
        return try {
            timeStr.toLongOrNull()?.let { timeFormat.format(Date(it)) } ?: timeStr
        } catch (e: Exception) {
            timeStr
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Date Box
                Surface(
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.size(64.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = dayFormat.format(date), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = weekdayFormat.format(date).uppercase(), fontSize = 12.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Details Grid
                Column(modifier = Modifier.weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DetailColumn(
                            modifier = Modifier.weight(1f),
                            value = dayWeekdayFormat.format(date),
                            subValue = formatTime(data.check_in_time),
                            label = "CheckIn"
                        )
                        DetailColumn(
                            modifier = Modifier.weight(1f),
                            value = "--",
                            subValue = formatTime(data.check_out_time),
                            label = "CheckOut"
                        )
                        DetailColumn(
                            modifier = Modifier.weight(1f),
                            value = "",
                            subValue = data.total_working_hrs ?: "0h 0m",
                            label = "Total Hrs"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailColumn(modifier: Modifier = Modifier, value: String, subValue: String, label: String) {
    Column(modifier = modifier) {
        Text(text = value.ifEmpty { " " }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
        Text(text = subValue.ifEmpty { "--" }, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
    }
}
