package com.app.confiengg.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.confiengg.data.api.RetrofitClient
import com.app.confiengg.data.model.LeaveRequestData
import com.app.confiengg.data.pref.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveRequestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userId = sessionManager.getUserId() ?: ""
    val scope = rememberCoroutineScope()
    
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val leaveRequests = remember { mutableStateListOf<LeaveRequestData>() }

    fun fetchLeaveRequests() {
        if (userId.isEmpty()) return
        isLoading = true
        scope.launch {
            try {
                val apiService = RetrofitClient.getClient(context)
                val response = apiService.getLeaveRequests(mapOf("user_id" to userId))
                if (response.isSuccessful) {
                    leaveRequests.clear()
                    response.body()?.data?.let {
                        leaveRequests.addAll(it)
                    }
                } else {
                    Toast.makeText(context, "Failed to fetch leave requests", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchLeaveRequests()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leave Requests") },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Leave")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (isLoading && leaveRequests.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (leaveRequests.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No leave request available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(leaveRequests) { item ->
                        LeaveItemCard(item)
                    }
                }
            }
        }
    }

    if (showDialog) {
        ApplyLeaveDialog(
            onDismiss = { showDialog = false },
            onApply = { date, desc ->
                scope.launch {
                    try {
                        val apiService = RetrofitClient.getClient(context)
                        val response = apiService.applyLeave(
                            mapOf(
                                "user_id" to userId,
                                "leave_date" to date,
                                "description" to desc
                            )
                        )
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Leave request submitted successfully", Toast.LENGTH_SHORT).show()
                            showDialog = false
                            fetchLeaveRequests()
                        } else {
                            Toast.makeText(context, "Failed to submit leave request", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun LeaveItemCard(item: LeaveRequestData) {
    val inputSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val outputSdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    
    val formattedLeaveDate = try {
        item.leaveDate?.let {
            val date = if (it.contains("T")) {
                inputSdf.parse(it)
            } else {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
            }
            outputSdf.format(date!!)
        } ?: "N/A"
    } catch (e: Exception) {
        item.leaveDate ?: "N/A"
    }

    val formattedAppliedDate = try {
        item.createdAt?.let {
            val date = if (it.contains("T")) {
                inputSdf.parse(it)
            } else {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(it)
            }
            outputSdf.format(date!!)
        } ?: "N/A"
    } catch (e: Exception) {
        item.createdAt ?: "N/A"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Leave Date", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(formattedLeaveDate, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (item.status?.lowercase()) {
                        "approved" -> Color(0xFFE8F5E9)
                        "pending" -> Color(0xFFFFF3E0)
                        "rejected" -> Color(0xFFFFEBEE)
                        else -> Color(0xFFF5F5F5)
                    }
                ) {
                    Text(
                        text = item.status?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                        color = when (item.status?.lowercase()) {
                            "approved" -> Color(0xFF2E7D32)
                            "pending" -> Color(0xFFE65100)
                            "rejected" -> Color(0xFFD32F2F)
                            else -> Color.Gray
                        },
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                Text("Applied On", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(formattedAppliedDate, style = MaterialTheme.typography.bodyMedium)
            }
            if (!item.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Remarks", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(item.description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyLeaveDialog(onDismiss: () -> Unit, onApply: (String, String) -> Unit) {
    var description by remember { mutableStateOf("") }
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return utcTimeMillis >= calendar.timeInMillis
            }
        }
    )
    var showDatePicker by remember { mutableStateOf(false) }
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val selectedDate = datePickerState.selectedDateMillis?.let {
        sdf.format(Date(it))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply New Leave") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedDate ?: "Select Leave Date")
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Reason for leave") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    minLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                enabled = selectedDate != null && description.isNotBlank(),
                onClick = {
                    onApply(selectedDate!!, description)
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
