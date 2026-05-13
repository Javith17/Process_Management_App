package com.app.confiengg.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.app.confiengg.data.api.RetrofitClient
import com.app.confiengg.data.model.ApproveQuotationRequest
import com.app.confiengg.data.model.Quotation
import com.app.confiengg.data.model.ReviseQuotationRequest
import com.app.confiengg.ui.components.CompactTextField
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationDetailScreen(
    quotation: Quotation,
    onApproveSuccess: () -> Unit,
    onUpdateSuccess: () -> Unit
) {
    val context = LocalContext.current
    val apiService = remember { RetrofitClient.getClient(context) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var isSubmitting by remember { mutableStateOf(false) }

    val displayFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
    val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var cost by remember { mutableStateOf(quotation.initial_cost ?: "") }
    var qty by remember { mutableStateOf(quotation.qty ?: "") }
    var remarks by remember { mutableStateOf(quotation.remarks ?: "") }

    fun parseDate(dateStr: String?): Long? {
        return try {
            dateStr?.let { apiFormat.parse(it)?.time }
        } catch (e: Exception) {
            null
        }
    }

    var quotationDateMillis by remember { mutableStateOf(parseDate(quotation.quotation_date)) }
    var reminderDateMillis by remember { mutableStateOf(parseDate(quotation.reminder_date)) }

    val quotationTerms = remember {
        mutableStateListOf<String>().apply {
            addAll(quotation.quotation_terms ?: emptyList())
        }
    }

    var showQuotationDatePicker by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }

    var showApproveConfirm by remember { mutableStateOf(false) }
    var showUpdateConfirm by remember { mutableStateOf(false) }

    var currentStep by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
            .padding(16.dp)
            .imePadding()
    ) {
        Text(
            text = "Step $currentStep of 2",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            if (currentStep == 1) {
                item {
                    Text(text = quotation.quotation_no ?: "N/A", style = MaterialTheme.typography.titleLarge)
                }
                item {
                    Text(
                        text = quotation.customer?.customer_name ?: "Unknown Customer",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                item {
                    Text(
                        text = quotation.machine?.machine_name ?: "N/A",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Quotation Date Picker Trigger
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Quotation Date",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(modifier = Modifier.fillMaxWidth().clickable { showQuotationDatePicker = true }) {
                            CompactTextField(
                                value = quotationDateMillis?.let { displayFormat.format(Date(it)) } ?: "",
                                onValueChange = {},
                                placeholder = "Select Date",
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // Reminder Date Picker Trigger
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Reminder Date",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(modifier = Modifier.fillMaxWidth().clickable { showReminderDatePicker = true }) {
                            CompactTextField(
                                value = reminderDateMillis?.let { displayFormat.format(Date(it)) } ?: "",
                                onValueChange = {},
                                placeholder = "Select Date",
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Cost", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        CompactTextField(
                            value = cost,
                            onValueChange = { cost = it },
                            placeholder = "Enter Cost",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Quantity", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        CompactTextField(
                            value = qty,
                            onValueChange = { qty = it },
                            placeholder = "Enter Quantity",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Remarks", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        CompactTextField(
                            value = remarks,
                            onValueChange = { remarks = it },
                            placeholder = "Enter Remarks",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        FilledIconButton(
                            onClick = { currentStep = 2 },
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                        }
                    }
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
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Term ${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        CompactTextField(
                            value = term,
                            onValueChange = { newValue ->
                                quotationTerms[index] = newValue
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "Enter Term",
                            singleLine = false,
                            keyboardOptions = KeyboardOptions(
                                imeAction = if (index == quotationTerms.size - 1) ImeAction.Done else ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() },
                                onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                            )
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Button
                        FilledIconButton(
                            onClick = { currentStep = 1 },
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Approve Button
                            if (quotation.status?.equals("Draft", ignoreCase = true) == true) {
                                Button(
                                    onClick = { showApproveConfirm = true },
                                    modifier = Modifier.height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)), // Green
                                    shape = CircleShape
                                ) {
                                    Text("Approve")
                                }
                            }

                            // Update Button
                            if (quotation.status?.equals("Draft", ignoreCase = true) == true) {
                                Button(
                                    onClick = { showUpdateConfirm = true },
                                    modifier = Modifier.height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF87CEEB)), // Sky Blue
                                    shape = CircleShape
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isSubmitting) {
        Dialog(onDismissRequest = {}) {
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    if (showQuotationDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = quotationDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showQuotationDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    quotationDateMillis = datePickerState.selectedDateMillis
                    showQuotationDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showQuotationDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showReminderDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = reminderDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showReminderDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    reminderDateMillis = datePickerState.selectedDateMillis
                    showReminderDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showApproveConfirm) {
        AlertDialog(
            onDismissRequest = { showApproveConfirm = false },
            title = { Text("Confirm Approve") },
            text = { Text("Do you want to approve?") },
            confirmButton = {
                Button(onClick = {
                    showApproveConfirm = false
                    scope.launch {
                        isSubmitting = true
                        try {
                            val request = ApproveQuotationRequest(
                                quotation_id = quotation.id,
                                qty = qty.ifBlank { quotation.qty },
                                approved_cost = cost.ifBlank { quotation.initial_cost }
                            )
                            val response = apiService.approveQuotation(request)
                            if (response.isSuccessful) {
                                onApproveSuccess()
                            }
                        } catch (e: Exception) {
                            // Handle error
                        } finally {
                            isSubmitting = false
                        }
                    }
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showApproveConfirm = false }) { Text("No") }
            }
        )
    }

    if (showUpdateConfirm) {
        AlertDialog(
            onDismissRequest = { showUpdateConfirm = false },
            title = { Text("Confirm Update") },
            text = { Text("Do you want to update?") },
            confirmButton = {
                Button(onClick = {
                    showUpdateConfirm = false
                    scope.launch {
                        isSubmitting = true
                        try {
                            val request = ReviseQuotationRequest(
                                quotation_id = quotation.id,
                                reminder_date = reminderDateMillis?.let { apiFormat.format(Date(it)) } ?: quotation.reminder_date,
                                qty = qty.ifBlank { quotation.qty },
                                cost = cost.ifBlank { quotation.initial_cost },
                                remarks = remarks.ifBlank { quotation.remarks },
                                quotation_terms = quotationTerms.toList()
                            )
                            val response = apiService.reviseQuotation(request)
                            if (response.isSuccessful) {
                                onUpdateSuccess()
                            }
                        } catch (e: Exception) {
                            // Handle error
                        } finally {
                            isSubmitting = false
                        }
                    }
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateConfirm = false }) { Text("No") }
            }
        )
    }
}
