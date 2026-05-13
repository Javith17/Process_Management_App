package com.app.confiengg.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.app.confiengg.data.model.Quotation
import com.app.confiengg.ui.screens.QuotationDetailScreen
import com.app.confiengg.ui.theme.ConfiEnggTheme
import com.google.gson.Gson

class QuotationDetailActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val quotationJson = intent.getStringExtra("quotation_json")
        val quotation = Gson().fromJson(quotationJson, Quotation::class.java)

        setContent {
            ConfiEnggTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Quotation Details") },
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
                    Surface(modifier = Modifier.padding(padding)) {
                        QuotationDetailScreen(
                            quotation = quotation,
                            onUpdateSuccess = { 
                                setResult(RESULT_OK)
                                finish() 
                            },
                            onApproveSuccess = {
                                setResult(RESULT_OK)
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
}
