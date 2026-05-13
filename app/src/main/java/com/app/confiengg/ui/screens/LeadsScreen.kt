package com.app.confiengg.ui.screens

import androidx.activity.compose.BackHandler
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeadsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var enquiries by remember { mutableStateOf<List<Enquiry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedEnquiry by remember { mutableStateOf<Enquiry?>(null) }

    val fetchEnquiries = { query: String ->
        scope.launch {
            isLoading = true
            try {
                val response = RetrofitClient.getClient(context).getEnquiries(
                    limit = 10,
                    page = 1,
                    status = "Open",
                    search = if (query.isEmpty()) null else query
                )
                if (response.isSuccessful) {
                    enquiries = response.body()?.list ?: emptyList()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error fetching leads: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchEnquiries("")
    }

    BackHandler(enabled = selectedEnquiry != null) {
        selectedEnquiry = null
        fetchEnquiries(searchQuery)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = selectedEnquiry,
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            }, label = "LeadsTransition"
        ) { targetEnquiry ->
            if (targetEnquiry != null) {
                EnquiryDetailScreen(
                    enquiry = targetEnquiry,
                    onBack = {
                        selectedEnquiry = null
                        fetchEnquiries(searchQuery)
                    }
                )
            } else {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)) {
                    CompactTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.isEmpty()) fetchEnquiries("")
                        },
                        placeholder = "Search enquiries...",
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
                                    text = "No Leads",
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
                                    onClick = { selectedEnquiry = enquiry },
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
        }
    }
}


@Composable
fun EnquiryDetailScreen(enquiry: Enquiry, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    var isUpdating by remember { mutableStateOf(false) }

    val startEnquiry = {
        scope.launch {
            isUpdating = true
            try {
                val response = RetrofitClient.getClient(context).updateEnquiryStatus(
                    UpdateEnquiryRequest(
                        enquiry_id = enquiry.id ?: "",
                        status = "Start",
                        level2_user = sessionManager.getUserId()
                    )
                )
                if (response.isSuccessful) {
                    Toast.makeText(context, "Started Enquiry Process", Toast.LENGTH_SHORT).show()
                    onBack()
                } else {
                    Toast.makeText(context, "Update failed: ${response.code()}", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isUpdating = false
            }
        }
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
                val intent =
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:${enquiry.contact_no ?: ""}"))
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
            Button(
                onClick = { startEnquiry() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Start")
            }
        }
    }
}
