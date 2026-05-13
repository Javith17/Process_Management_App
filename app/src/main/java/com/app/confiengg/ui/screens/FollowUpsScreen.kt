@file:OptIn(ExperimentalMaterial3Api::class)

package com.app.confiengg.ui.screens

import androidx.activity.compose.BackHandler
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.app.confiengg.ui.activities.ApproveEnquiryActivity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.confiengg.data.api.RetrofitClient
import com.app.confiengg.data.model.Enquiry
import com.app.confiengg.data.model.UpdateEnquiryRequest
import com.app.confiengg.data.pref.SessionManager
import com.app.confiengg.ui.components.CompactTextField
import com.app.confiengg.ui.components.DetailRow
import com.app.confiengg.ui.components.EnquiryCard
import com.app.confiengg.ui.activities.FollowUpDetailActivity
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.*


@Composable
fun FollowUpsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var enquiries by remember { mutableStateOf<List<Enquiry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedEnquiry by remember { mutableStateOf<Enquiry?>(null) }

    val sessionManager = remember { SessionManager(context) }
    val userId = sessionManager.getUserId()

    val fetchEnquiries = { query: String ->
        scope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.getClient(context).getEnquiries(
                    limit = 10,
                    page = 1,
                    status = "In Progress",
                    user = userId,
                    search = if (query.isEmpty()) null else query
                )
                if (response.isSuccessful) {
                    enquiries = response.body()?.list ?: emptyList()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error fetching follow-ups: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchEnquiries("")
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        CompactTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                if (it.isEmpty()) fetchEnquiries("")
            },
            placeholder = "Search follow-ups...",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        fetchEnquiries("")
                    }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { fetchEnquiries(searchQuery) })
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (enquiries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Follow ups",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(enquiries) { enquiry ->
                    EnquiryCard(
                        enquiry = enquiry,
                        onClick = {
                            val intent = Intent(context, FollowUpDetailActivity::class.java).apply {
                                putExtra("enquiry_json", Gson().toJson(enquiry))
                            }
                            context.startActivity(intent)
                        },
                        onCall = {
                            val intent = Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:${enquiry.contact_no ?: ""}")
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ApproveEnquiryDialog(
    enquiry: Enquiry,
    onDismiss: () -> Unit,
    onConfirm: (UpdateEnquiryRequest) -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) }
    var quotationDate by remember { mutableStateOf("") }
    var reminderDate by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    val quotationTerms = remember {
        mutableStateListOf<String>().apply {
            addAll(enquiry.quotation_terms ?: emptyList())
        }
    }
    val sessionManager = remember { SessionManager(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Approve Enquiry (Step $currentStep/2)") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep == 1) {
                    item {
                        Column {
                            Text("Quotation Date", style = MaterialTheme.typography.labelMedium)
                            Box(modifier = Modifier.fillMaxWidth().clickable {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(
                                    context, { _, year, month, day ->
                                        quotationDate = "$year-${month + 1}-$day"
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                CompactTextField(
                                    value = quotationDate,
                                    onValueChange = { },
                                    placeholder = "Select Date",
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                    item {
                        Column {
                            Text("Reminder Date", style = MaterialTheme.typography.labelMedium)
                            Box(modifier = Modifier.fillMaxWidth().clickable {
                                val calendar = Calendar.getInstance()
                                DatePickerDialog(
                                    context, { _, year, month, day ->
                                        reminderDate = "$year-${month + 1}-$day"
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                CompactTextField(
                                    value = reminderDate,
                                    onValueChange = { },
                                    placeholder = "Select Date",
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                    item {
                        CompactTextField(
                            value = cost,
                            onValueChange = { cost = it },
                            placeholder = "Cost",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )
                    }
                    item {
                        CompactTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            placeholder = "Quantity",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )
                    }
                    item {
                        CompactTextField(
                            value = remarks,
                            onValueChange = { remarks = it },
                            placeholder = "Remarks",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )
                    }
                } else {
                    item {
                        Text(
                            "Quotation Terms",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    itemsIndexed(quotationTerms) { index, term ->
                        CompactTextField(
                            value = term,
                            onValueChange = { newValue ->
                                quotationTerms[index] = newValue
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "Term ${index + 1}",
                            keyboardOptions = KeyboardOptions(
                                imeAction = if (index == quotationTerms.size - 1) ImeAction.Done else ImeAction.Next
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (currentStep == 1) {
                Button(
                    onClick = {
                        if (quotationDate.isNotEmpty() && reminderDate.isNotEmpty() && cost.isNotEmpty() && quantity.isNotEmpty()) {
                            currentStep = 2
                        } else {
                            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Next")
                }
            } else {
                Button(
                    onClick = {
                        onConfirm(
                            UpdateEnquiryRequest(
                                enquiry_id = enquiry.id ?: "",
                                status = "Approve",
                                quotation_date = quotationDate,
                                reminder_date = reminderDate,
                                cost = cost.toDoubleOrNull(),
                                qty = quantity.toIntOrNull(),
                                remarks = remarks,
                                approved_by = sessionManager.getUserId(),
                                quotation_terms = quotationTerms.toList()
                            )
                        )
                    }
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            if (currentStep == 1) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            } else {
                TextButton(onClick = { currentStep = 1 }) {
                    Text("Previous")
                }
            }
        }
    )
}

@Composable
fun RejectEnquiryDialog(
    enquiry: Enquiry,
    onDismiss: () -> Unit,
    onConfirm: (UpdateEnquiryRequest) -> Unit
) {
    var remarks by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject Enquiry") },
        text = {
            Column {
                Text("Please enter remarks for rejection:")
                Spacer(modifier = Modifier.height(8.dp))
                CompactTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Enter remarks..."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (remarks.isNotBlank()) {
                        onConfirm(
                            UpdateEnquiryRequest(
                                enquiry_id = enquiry.id ?: "",
                                status = "Reject",
                                remarks = remarks
                            )
                        )
                    } else {
                        Toast.makeText(context, "Remarks cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowUpDetailScreen(enquiry: Enquiry, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    var isUpdating by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showApproveDialog by remember { mutableStateOf(false) }

    val updateStatus = { request: UpdateEnquiryRequest ->
        scope.launch {
            isUpdating = true
            try {
                val response = RetrofitClient.getClient(context).updateEnquiryStatus(request)
                if (response.isSuccessful) {
                    val message = if (request.status == "Approve") "Enquiry Approved Successfully" else "Rejected enquiry"
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    onBack()
                } else {
                    Toast.makeText(context, "Update failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isUpdating = false
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onBack()
        }
    }

    if (showRejectDialog) {
        RejectEnquiryDialog(
            enquiry = enquiry,
            onDismiss = { showRejectDialog = false },
            onConfirm = { request ->
                showRejectDialog = false
                updateStatus(request)
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        DetailRow(label = "Customer", value = enquiry.customer_name ?: "")
        DetailRow(label = "Machine", value = enquiry.machine_name ?: "")

        Column(modifier = Modifier
            .padding(vertical = 4.dp)
            .clickable {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${enquiry.contact_no ?: ""}"))
                context.startActivity(intent)
            }) {
            Text(
                text = "Contact",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = enquiry.contact_no ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        DetailRow(label = "GST No", value = enquiry.gst_no ?: "")
        DetailRow(label = "Resource", value = enquiry.enquiry_resource ?: "")
        DetailRow(label = "Remarks", value = enquiry.remarks ?: "")
        DetailRow(label = "Lead By", value = enquiry.level1_user?.emp_name ?: "")

        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = "Address",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "${enquiry.address?.address_1 ?: ""}\n${enquiry.address?.city ?: ""}\n${enquiry.address?.state ?: ""} - ${enquiry.address?.postal_code ?: ""}",
                style = MaterialTheme.typography.bodyLarge
            )
            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isUpdating) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        showRejectDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Reject")
                }
                Button(
                    onClick = {
                        val intent = Intent(context, ApproveEnquiryActivity::class.java).apply {
                            putExtra("enquiry_json", Gson().toJson(enquiry))
                        }
                        launcher.launch(intent)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3C9E09)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Approve")
                }
            }
        }
    }
}

