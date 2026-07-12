package com.app.confiengg.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.app.confiengg.R
import com.app.confiengg.data.api.RetrofitClient
import com.app.confiengg.data.model.LoginRequest
import com.app.confiengg.data.pref.SessionManager
import com.app.confiengg.ui.components.CompactTextField
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    var empCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var isVendorLogin by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    var pendingRole by remember { mutableStateOf<String?>(null) }

    fun checkUsageStatsAndNavigate() {
        if (!hasUsageStatsPermission(context)) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            context.startActivity(intent)
            Toast.makeText(context, "Please enable Usage Access for screen time tracking", Toast.LENGTH_LONG).show()
        }
        pendingRole?.let { onLoginSuccess(it) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        val postNotificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        } else true
        
        if (fineLocationGranted || coarseLocationGranted) {
            checkUsageStatsAndNavigate()
        } else {
            Toast.makeText(context, "Location permission is required for attendance", Toast.LENGTH_LONG).show()
            checkUsageStatsAndNavigate()
        }
    }

    val performSendOTP = {
        if (phoneNumber.length == 10) {
            scope.launch {
                isLoading = true
                errorMessage = null
                try {
                    val apiService = RetrofitClient.getClient(context)
                    val response = apiService.sendOTP(mapOf("phone_number" to phoneNumber))
                    if (response.isSuccessful) {
                        isOtpSent = true
                        Toast.makeText(context, "OTP sent successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        errorMessage = "Failed to send OTP: ${response.code()}"
                    }
                } catch (e: Exception) {
                    errorMessage = "Network error. Please check your connection."
                } finally {
                    isLoading = false
                }
            }
        } else {
            errorMessage = "Please enter a valid 10-digit phone number"
        }
    }

    val performVerifyOTP = {
        if (otp.length == 6) {
            scope.launch {
                isLoading = true
                errorMessage = null
                try {
                    val apiService = RetrofitClient.getClient(context)
                    val response = apiService.verifyOTP(mapOf("phone_number" to phoneNumber, "otp" to otp))
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.user != null && body.accessToken != null) {
                            val role = body.user.roleName
                            sessionManager.saveAuthToken(body.accessToken)
                            sessionManager.saveUserSession(body.user.userId, body.user.empName, role)
                            
                            val hasQuotations = (body.user.screens.any { it.screen.contains("quotations", ignoreCase = true) } ||
                                                body.screens?.any { it.screen.contains("quotations", ignoreCase = true) } == true)
                            sessionManager.saveHasQuotations(hasQuotations)
                            
                            // Save office configs
                            val officeLoc = body.configs?.office_location
                            sessionManager.saveOfficeConfigs(
                                officeLoc?.latitue,
                                officeLoc?.longitude,
                                officeLoc?.perimeter
                            )
                            
                            pendingRole = role
                            val permissions = mutableListOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(permissions.toTypedArray())
                        } else {
                            errorMessage = "Invalid OTP"
                        }
                    } else {
                        errorMessage = "Verification failed: ${response.code()}"
                    }
                } catch (e: Exception) {
                    errorMessage = "Network error. Please check your connection."
                } finally {
                    isLoading = false
                }
            }
        } else {
            errorMessage = "Please enter a 6-digit OTP"
        }
    }

    val performLogin = {
        if (empCode.isNotBlank() && password.isNotBlank()) {
            scope.launch {
                isLoading = true
                errorMessage = null
                try {
                    val apiService = RetrofitClient.getClient(context)
                    val response = apiService.login(LoginRequest(empCode, password))
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.user != null && body.accessToken != null) {
                            val role = body.user.roleName
                            sessionManager.saveAuthToken(body.accessToken)
                            sessionManager.saveUserSession(body.user.userId, body.user.empName, role)
                            
                            val hasQuotations = (body.user.screens.any { it.screen.contains("quotations", ignoreCase = true) } ||
                                                body.screens?.any { it.screen.contains("quotations", ignoreCase = true) } == true)
                            sessionManager.saveHasQuotations(hasQuotations)
                            
                            // Save office configs
                            val officeLoc = body.configs?.office_location
                            sessionManager.saveOfficeConfigs(
                                officeLoc?.latitue,
                                officeLoc?.longitude,
                                officeLoc?.perimeter
                            )
                            
                            pendingRole = role
                            val permissions = mutableListOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionLauncher.launch(permissions.toTypedArray())
                        } else {
                            errorMessage = "Invalid credentials"
                        }
                    } else {
                        errorMessage = "Login failed: ${response.code()}"
                    }
                } catch (e: Exception) {
                    errorMessage = "Network error. Please check your connection."
                } finally {
                    isLoading = false
                }
            }
        } else {
            errorMessage = "Please enter both employee code and password"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { isVendorLogin = false; errorMessage = null },
                modifier = Modifier.weight(1f),
                colors = if (!isVendorLogin) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) else ButtonDefaults.outlinedButtonColors()
            ) {
                Text("Employee Login", color = if (!isVendorLogin) MaterialTheme.colorScheme.primary else Color.Gray)
            }
            OutlinedButton(
                onClick = { isVendorLogin = true; errorMessage = null },
                modifier = Modifier.weight(1f),
                colors = if (isVendorLogin) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) else ButtonDefaults.outlinedButtonColors()
            ) {
                Text("Vendor Login", color = if (isVendorLogin) MaterialTheme.colorScheme.primary else Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isVendorLogin) {
            CompactTextField(
                value = phoneNumber,
                onValueChange = { if (it.length <= 10) phoneNumber = it },
                placeholder = "Phone Number (10 digits)",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = if (isOtpSent) ImeAction.Next else ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { if (!isOtpSent) performSendOTP() }
                ),
                enabled = !isLoading && !isOtpSent
            )

            if (isOtpSent) {
                Spacer(modifier = Modifier.height(16.dp))
                CompactTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 6) otp = it },
                    placeholder = "6 Digit OTP",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { performVerifyOTP() }
                    ),
                    enabled = !isLoading
                )
            }
        } else {
            CompactTextField(
                value = empCode,
                onValueChange = { empCode = it },
                placeholder = "Employee Code",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                enabled = !isLoading
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            CompactTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { 
                        focusManager.clearFocus()
                        performLogin()
                    }
                ),
                enabled = !isLoading
            )
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { 
                focusManager.clearFocus()
                if (isVendorLogin) {
                    if (isOtpSent) performVerifyOTP() else performSendOTP()
                } else {
                    performLogin()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(if (isVendorLogin) (if (isOtpSent) "VERIFY OTP" else "SEND OTP") else "LOGIN")
            }
        }
    }
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

