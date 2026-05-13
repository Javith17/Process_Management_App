package com.app.confiengg.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
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
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

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
                            onLoginSuccess(role)
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
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { 
                    focusManager.clearFocus()
                    performLogin()
                }
            ),
            enabled = !isLoading
        )

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
                performLogin() 
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
                Text("LOGIN")
            }
        }
    }
}

