package com.app.confiengg.ui.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.app.confiengg.data.api.RetrofitClient
import com.app.confiengg.data.model.Enquiry
import com.app.confiengg.data.model.UpdateEnquiryRequest
import com.app.confiengg.data.pref.SessionManager
import com.app.confiengg.ui.components.CompactTextField
import com.app.confiengg.ui.theme.ConfiEnggTheme
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.*

class ApproveEnquiryActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val enquiryJson = intent.getStringExtra("enquiry_json")
        val enquiry = Gson().fromJson(enquiryJson, Enquiry::class.java)

        setContent {
            ConfiEnggTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Approve Enquiry") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
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
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        ApproveEnquiryScreen(enquiry = enquiry, onBack = {
                            setResult(RESULT_OK)
                            finish()
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun ApproveEnquiryScreen(enquiry: Enquiry, onBack: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
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
    var isUpdating by remember { mutableStateOf(false) }

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
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Quotation Date", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Box(modifier = Modifier.fillMaxWidth().clickable {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(
                                context, { _, year, month, day ->
                                    quotationDate = String.format("%d-%02d-%02d", year, month + 1, day)
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
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Reminder Date", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Box(modifier = Modifier.fillMaxWidth().clickable {
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(
                                context, { _, year, month, day ->
                                    reminderDate = String.format("%d-%02d-%02d", year, month + 1, day)
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
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Quantity", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        CompactTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            placeholder = "Enter Quantity",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
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
                    Button(
                        onClick = {
                            if (quotationDate.isNotEmpty() && reminderDate.isNotEmpty() && cost.isNotEmpty() && quantity.isNotEmpty()) {
                                currentStep = 2
                            } else {
                                Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Next")
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
                        Text("Term ${index + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
                    if (isUpdating) {
                        Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { currentStep = 1 },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Previous")
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        isUpdating = true
                                        try {
                                            val request = UpdateEnquiryRequest(
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
                                            val response = RetrofitClient.getClient(context).updateEnquiryStatus(request)
                                            if (response.isSuccessful) {
                                                Toast.makeText(context, "Enquiry Approved Successfully", Toast.LENGTH_SHORT).show()
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
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3C9E09))
                            ) {
                                Text("Approve & Save")
                            }
                        }
                    }
                }
            }
        }
    }
}
