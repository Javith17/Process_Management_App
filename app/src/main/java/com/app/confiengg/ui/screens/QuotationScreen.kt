package com.app.confiengg.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.widget.Toast
import com.app.confiengg.ui.activities.AttachmentsActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.app.confiengg.ui.activities.QuotationDetailActivity
import com.google.gson.Gson
import com.app.confiengg.data.api.RetrofitClient
import com.app.confiengg.data.model.*
import com.app.confiengg.data.pref.SessionManager
import com.app.confiengg.ui.components.CompactTextField
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun QuotationScreen() {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val apiService = remember { RetrofitClient.getClient(context) }
    val scope = rememberCoroutineScope()
    var quotations by remember { mutableStateOf<List<Quotation>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var selectedHtmlContent by remember { mutableStateOf<String?>(null) }

    val onAttachmentClick: (Quotation) -> Unit = { quotation ->
        quotation.machine?.id?.let { machineId ->
            scope.launch {
                isSubmitting = true
                try {
                    val response = apiService.getMachineAttachments(machineId)
                    if (response.isSuccessful) {
                        val links = response.body()?.links
                        if (!links.isNullOrEmpty()) {
                            val intent = Intent(context, AttachmentsActivity::class.java).apply {
                                putStringArrayListExtra("links", ArrayList(links))
                            }
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "No attachments available", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error fetching attachments", Toast.LENGTH_SHORT).show()
                } finally {
                    isSubmitting = false
                }
            }
        }
    }

    val fetchQuotations = {
        scope.launch {
            isLoading = true
            try {
                val userId = sessionManager.getUserId()
                val response = apiService.getQuotations(userId)
                if (response.isSuccessful) {
                    quotations = response.body()?.list ?: emptyList()
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                isLoading = false
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            fetchQuotations()
        }
    }

    val filteredQuotations = quotations.filter {
        it.quotation_no?.contains(searchQuery, ignoreCase = true) == true ||
                it.customer?.customer_name?.contains(searchQuery, ignoreCase = true) == true
    }

    LaunchedEffect(Unit) {
        fetchQuotations()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        CompactTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Search Quotations",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredQuotations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Text("No quotations", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredQuotations) { quotation ->
                    QuotationCard(
                        quotation = quotation,
                        onPdfClick = {
                            scope.launch {
                                isSubmitting = true
                                try {
                                    val response = apiService.getQuotationDoc(quotation.id ?: "")
                                    if (response.isSuccessful) {
                                        selectedHtmlContent = response.body()?.string()
                                    }
                                } catch (e: Exception) {
                                    // Handle error
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        },
                        onCardClick = {
                            val intent = Intent(context, QuotationDetailActivity::class.java).apply {
                                putExtra("quotation_json", Gson().toJson(quotation))
                            }
                            launcher.launch(intent)
                        },
                        onAttachmentClick = { onAttachmentClick(quotation) }
                    )
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

    selectedHtmlContent?.let { html ->
        HtmlViewerDialog(html) { selectedHtmlContent = null }
    }
}

@Composable
fun QuotationCard(quotation: Quotation, onPdfClick: () -> Unit, onCardClick: () -> Unit, onAttachmentClick: () -> Unit) {
    val displayFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
    val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    val formattedDate = try {
        quotation.created_at?.let { dateStr ->
            val date = apiFormat.parse(dateStr)
            date?.let { displayFormat.format(it) }
        } ?: "N/A"
    } catch (e: Exception) {
        quotation.created_at ?: "N/A"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = quotation.quotation_no ?: "N/A",
                fontSize = 14.sp,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = quotation.customer?.customer_name ?: "Unknown Customer",
                fontSize = 12.sp,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = quotation.status ?: "N/A",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (quotation.status?.equals("Draft", ignoreCase = true) == true) {
                        IconButton(onClick = { /* Edit */ }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onPdfClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color.Red, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onAttachmentClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attachment", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun HtmlViewerDialog(htmlContent: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    fun shareAsImage() {
        webViewRef?.let { webView ->
            val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            webView.draw(canvas)

            val file = File(context.cacheDir, "quotation.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Quotation as Image"))
        }
    }

    fun shareAsPdf() {
        webViewRef?.let { webView ->
            val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as PrintManager
            val jobName = "Quotation Document"
            val printAdapter: PrintDocumentAdapter = webView.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    Row {
                        IconButton(onClick = { shareAsPdf() }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Share PDF", tint = Color.Red)
                        }
                        IconButton(onClick = { shareAsImage() }) {
                            Icon(Icons.Default.Image, contentDescription = "Share Image", tint = Color.Blue)
                        }
                    }
                }
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            webViewRef = this
                            loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

